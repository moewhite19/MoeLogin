package cn.whiteg.moeLogin.utils;

import cn.whiteg.mmocore.reflection.ReflectUtil;
import cn.whiteg.mmocore.reflection.ReflectionFactory;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R2.CraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MojangAPI {
    static Method getMinecraftSessionService;

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
    private final Gson gson;
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
    final MinecraftClient client;

    public MojangAPI() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class,new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class,new PropertyMap.Serializer());
        builder.registerTypeAdapter(UUID.class,new UUIDTypeAdapter());
        builder.registerTypeAdapter(ProfileSearchResultsResponse.class,new com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse.Serializer());
        this.gson = builder.create();
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
            //设置超时
            ReflectionFactory.createFieldAccessor(MinecraftClient.class.getField("CONNECT_TIMEOUT_MS")).setLong(null,15000);
            ReflectionFactory.createFieldAccessor(MinecraftClient.class.getField("READ_TIMEOUT_MS")).setLong(null,15000);


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

    //参考类名YggdrasilMinecraftSessionService
    public void joinServer(UUID profileId,String authenticationToken,String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken,profileId,serverId);

        try{
            this.client.post(this.JOIN_URL,request,Void.class);
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

        HasJoinedMinecraftServerResponse response = this.client.get(url,HasJoinedMinecraftServerResponse.class);
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


//    public <T extends Response> T makeRequest(URL url,Object input,Class<T> classOfT) throws AuthenticationException {
//        try{
//            String jsonResult = input == null ? this.performGetRequest(url) : this.performPostRequest(url,gson.toJson(input),"application/json");
//            if (Setting.DEBUG){
//                MoeLogin.logger.info("访问URL: " + url.toString());
//            }
//            T result = this.gson.fromJson(jsonResult,classOfT);
//            if (result == null){
//                return null;
//            } else if (StringUtils.isNotBlank(result.getError())){
//                if ("UserMigratedException".equals(result.getCause())){
//                    throw new UserMigratedException(result.getErrorMessage());
//                } else if ("ForbiddenOperationException".equals(result.getError())){
//                    throw new InvalidCredentialsException(result.getErrorMessage());
//                } else {
//                    throw new AuthenticationException(result.getErrorMessage());
//                }
//            } else {
//                return result;
//            }
//        }catch (IllegalStateException | JsonParseException | IOException var6){
//            throw new AuthenticationUnavailableException("Cannot contact authentication server",var6);
//        }
//    }

    public String performGetRequest(URL url) throws IOException {
        Validate.notNull(url);
        HttpURLConnection connection = this.createUrlConnection(url);
        //LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;

        String var6;
        try{
            String result;
            try{
                inputStream = connection.getInputStream();
                result = IOUtils.toString(inputStream,Charsets.UTF_8);
                //LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                //LOGGER.debug("Response: " + result);
                return result;
            }catch (IOException var10){
                IOUtils.closeQuietly(inputStream);
                inputStream = connection.getErrorStream();
                if (inputStream == null){
                    //LOGGER.debug("Request failed", var10);
                    throw var10;
                }

                //LOGGER.debug("Reading error page from " + url);
                result = IOUtils.toString(inputStream,Charsets.UTF_8);
                //LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                //LOGGER.debug("Response: " + result);
                var6 = result;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return var6;
    }

    public String performPostRequest(URL url,String post,String contentType) throws IOException {
        Validate.notNull(url);
        Validate.notNull(post);
        Validate.notNull(contentType);
        HttpURLConnection connection = this.createUrlConnection(url);
        byte[] postAsBytes = post.getBytes(Charsets.UTF_8);
        connection.setRequestProperty("Content-Type",contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length","" + postAsBytes.length);
        connection.setDoOutput(true);
        //LOGGER.debug("Writing POST data to " + url + ": " + post);
        OutputStream outputStream = null;

        try{
            outputStream = connection.getOutputStream();
            IOUtils.write(postAsBytes,outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        //LOGGER.debug("Reading data from " + url);
        InputStream inputStream = null;

        String var10;
        try{
            String result = IOUtils.toString(inputStream,Charsets.UTF_8);
            try{
                inputStream = connection.getInputStream();
                //LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
                //LOGGER.debug("Response: " + result);
                result = result;
                return result;
            }catch (IOException var19){
                IOUtils.closeQuietly(inputStream);
                inputStream = connection.getErrorStream();
                if (inputStream == null){
//                    LOGGER.debug("Request failed", var19);
                    throw var19;
                }

//                LOGGER.debug("Reading error page from " + url);
                result = IOUtils.toString(inputStream,Charsets.UTF_8);
//                LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
//                LOGGER.debug("Response: " + result);
                var10 = result;
            }
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        return var10;
    }

    protected HttpURLConnection createUrlConnection(URL url) throws IOException {
        Validate.notNull(url);
        //LOGGER.debug("Opening connection to " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        private GameProfileSerializer() {
        }

        public GameProfile deserialize(JsonElement json,Type typeOfT,JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = (JsonObject) json;
            UUID id = object.has("id") ? (UUID) context.deserialize(object.get("id"),UUID.class) : null;
            String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id,name);
        }

        public JsonElement serialize(GameProfile src,Type typeOfSrc,JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            if (src.getId() != null){
                result.add("id",context.serialize(src.getId()));
            }

            if (src.getName() != null){
                result.addProperty("name",src.getName());
            }

            return result;
        }
    }

}
