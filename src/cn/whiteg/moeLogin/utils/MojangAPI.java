package cn.whiteg.moeLogin.utils;

import cn.whiteg.mmocore.reflection.FieldAccessor;
import cn.whiteg.mmocore.reflection.ReflectUtil;
import cn.whiteg.mmocore.reflection.ReflectionFactory;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.ErrorResponse;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
    URL JOIN_URL;

    {
        try{
            JOIN_URL = new URL(MOJANG_BASE_URL.concat("join"));
        }catch (MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    YggdrasilMinecraftSessionService yggdrasilMinecraftSessionService;
    MinecraftClient client;

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
            this.post(this.JOIN_URL,request,Void.class);
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

        HasJoinedMinecraftServerResponse response = get(url,HasJoinedMinecraftServerResponse.class);
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


    /*
     * 下面都是摘自MinecraftClient.class
     * 因为无法重写私有的createUrlConnection方法，所以只能整个都把他的方法都抄过来
     * ----------------------------------
     */
    @Nullable
    public <T> T get(URL url,Class<T> responseClass) {
        org.apache.commons.lang3.Validate.notNull(url);
        org.apache.commons.lang3.Validate.notNull(responseClass);
        HttpURLConnection connection = this.createUrlConnection(url);
//        if (this.accessToken != null){
//            connection.setRequestProperty("Authorization","Bearer " + this.accessToken);
//        }

        return this.readInputStream(url,responseClass,connection);
    }

    @Nullable
    public <T> T post(URL url,Class<T> responseClass) {
        org.apache.commons.lang3.Validate.notNull(url);
        org.apache.commons.lang3.Validate.notNull(responseClass);
        HttpURLConnection connection = this.postInternal(url,new byte[0]);
        return this.readInputStream(url,responseClass,connection);
    }

    @Nullable
    public <T> T post(URL url,Object body,Class<T> responseClass) {
        org.apache.commons.lang3.Validate.notNull(url);
        org.apache.commons.lang3.Validate.notNull(body);
        Validate.notNull(responseClass);
        String bodyAsJson = objectMapper.writeValueAsString(body);
        byte[] postAsBytes = bodyAsJson.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = this.postInternal(url,postAsBytes);
        return this.readInputStream(url,responseClass,connection);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T readInputStream(URL url,Class<T> clazz,HttpURLConnection connection) {
        InputStream inputStream = null;

        Object var7;
        try{
            int status = connection.getResponseCode();
            String result;
            if (status >= 400){
                inputStream = connection.getErrorStream();
                if (inputStream != null){
                    result = IOUtils.toString(inputStream,StandardCharsets.UTF_8);
                    ErrorResponse errorResponse = objectMapper.readValue(result,ErrorResponse.class);
                    throw new MinecraftClientHttpException(status,errorResponse);
                }

                throw new MinecraftClientHttpException(status);
            }

            inputStream = connection.getInputStream();
            result = IOUtils.toString(inputStream,StandardCharsets.UTF_8);
            if (result.isEmpty()){
                var7 = null;
                return (T) var7;
            }

            var7 = objectMapper.readValue(result,clazz);
        }catch (IOException var11){
            throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,"Failed to read from " + url + " due to " + var11.getMessage(),var11);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return (T) var7;
    }

    private HttpURLConnection postInternal(URL url,byte[] postAsBytes) {
        HttpURLConnection connection = this.createUrlConnection(url);
        OutputStream outputStream = null;

        try{
            connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
            connection.setRequestProperty("Content-Length","" + postAsBytes.length);
//            if (this.accessToken != null) {
//                connection.setRequestProperty("Authorization", "Bearer " + this.accessToken);
//            }

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes,outputStream);
        }catch (IOException var9){
            throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,"Failed to POST " + url,var9);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return connection;
    }

    private HttpURLConnection createUrlConnection(URL url) {
        try{
            if (Setting.DEBUG){
                MoeLogin.logger.info("访问URL: " + url.toString());
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            return connection;
        }catch (IOException var3){
            throw new MinecraftClientException(MinecraftClientException.ErrorType.SERVICE_UNAVAILABLE,"Failed connecting to " + url,var3);
        }
    }

    public void setProxy(Proxy py) {
        try{
            final FieldAccessor<Object> proxy = ReflectionFactory.createFieldAccessor(MinecraftClient.class.getDeclaredField("proxy"));
            proxy.set(client,proxy);
        }catch (NoSuchFieldException e){
            e.printStackTrace();
        }
    }

}
