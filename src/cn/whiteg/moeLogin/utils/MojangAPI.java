package cn.whiteg.moeLogin.utils;

import cn.whiteg.mmocore.reflection.FieldAccessor;
import cn.whiteg.mmocore.reflection.ReflectUtil;
import cn.whiteg.mmocore.reflection.ReflectionFactory;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MojangAPI {
    static Method getMinecraftSessionService;
    static ObjectMapper objectMapper;

    static {
        for (Method method : MinecraftServer.class.getMethods()) {
            if (method.getReturnType().equals(MinecraftSessionService.class)){
                method.setAccessible(true);
                getMinecraftSessionService = method;
            }
        }
    }

    //private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s";
    //private static final String[] WHITELISTED_DOMAINS = new String[]{".minecraft.net",".mojang.com"};
    //String baseUrl = env.getSessionHost() + "/session/minecraft/";
    public static final String MOJANG_BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";  //Mojang会话服务器
    private final FieldAccessor<Proxy> proxyField;
    URL JOIN_URL;

    {
        try{
            JOIN_URL = new URL(MOJANG_BASE_URL.concat("join"));
        }catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    YggdrasilMinecraftSessionService yggdrasilMinecraftSessionService;
    public MinecraftClient client;

    @SuppressWarnings("unchecked")
    public MojangAPI() {
        DedicatedServer server = ((CraftServer) Bukkit.getServer()).getServer();
        MinecraftSessionService sessionService;
        try{
            sessionService = (MinecraftSessionService) getMinecraftSessionService.invoke(server);
        }catch (InvocationTargetException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
        if (sessionService instanceof YggdrasilMinecraftSessionService){
            yggdrasilMinecraftSessionService = (YggdrasilMinecraftSessionService) sessionService;
        }
        try{
            Field field = ReflectUtil.getFieldFormType(sessionService.getClass(),MinecraftClient.class);
            field.setAccessible(true);
            client = (MinecraftClient) field.get(sessionService);
            field = ReflectUtil.getFieldFormType(client.getClass(),ObjectMapper.class);
            field.setAccessible(true);
            objectMapper = (ObjectMapper) field.get(client);
            Field declaredField = MinecraftClient.class.getDeclaredField("proxy");
            declaredField.setAccessible(true);
            proxyField = (FieldAccessor<Proxy>) ReflectionFactory.createFieldAccessor(declaredField);
        }catch (NoSuchFieldException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    public URL constantURL(String url) {
        try{
            return new URL(url);
        }catch (MalformedURLException var2){
            throw new Error("Couldn't create constant for " + url,var2);
        }
    }

    /*
     * 参考类名YggdrasilMinecraftSessionService.class
     */
    public void joinServer(UUID profileId,String authenticationToken,String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken,profileId,serverId);

        try{
            client.post(this.JOIN_URL,request,Void.class);
        }catch (MinecraftClientException var6){
            throw var6.toAuthenticationException();
        }
    }

    //正版验证
    public ProfileResult hasJoinedServer(GameProfile user,String serverId,InetAddress address) throws AuthenticationUnavailableException {
        return hasJoinedServer(user,serverId,address,MOJANG_BASE_URL);
    }

    //验证玩家会话
    public ProfileResult hasJoinedServer(GameProfile user,String serverId,InetAddress address,String baseUrl) throws AuthenticationUnavailableException {
        Map<String, Object> arguments = new HashMap<>(3);
        arguments.put("username",user.getName());
        arguments.put("serverId",serverId);
        if (address != null){
            arguments.put("ip",address.getHostAddress());
        }

        //生成服务器会话URL
        URL url = HttpAuthenticationService.concatenateURL(constantURL(baseUrl.concat("hasJoined")),HttpAuthenticationService.buildQuery(arguments));

        HasJoinedMinecraftServerResponse response = client.get(url,HasJoinedMinecraftServerResponse.class);
        if (response != null && response.id() != null){
            GameProfile result = new GameProfile(response.id(),user.getName());
            if (response.properties() != null){
                result.getProperties().putAll(response.properties());
            }

            Set<ProfileActionType> profileActions = response.profileActions().stream().map(ProfileAction::type).collect(Collectors.toSet());
            return new ProfileResult(result,profileActions);
        } else {
            return null;
        }
    }

    public void setProxy(Proxy py) {
        proxyField.set(client,py);
    }

    public Proxy getProxy() {
        return proxyField.get(client);
    }
}
