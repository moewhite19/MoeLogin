package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.reflection.FieldAccessor;
import cn.whiteg.mmocore.reflection.ReflectUtil;
import cn.whiteg.mmocore.util.FileMan;
import cn.whiteg.mmocore.util.NMSUtils;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.PlayerPacketManage;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.CryptException;
import net.minecraft.util.SignatureValidator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class AuthenticateListener implements Listener {

    //会话验证线程
    private static final AtomicInteger threadId = new AtomicInteger(0);
    //正版验证线程池
    private static final ExecutorService authenticatorPool = Executors.newCachedThreadPool((r) -> new Thread(r,"User Authenticator #" + threadId.incrementAndGet()));

    //获取玩家GameProfile
    private static final FieldAccessor<GameProfile> gameProfileField;
    private final byte[] token = new byte[4];
    private final Logger logger;
    //private Map<String, LoginSession> sessionMap = Collections.synchronizedMap(new MapMaker().weakKeys().makeMap());  //弱Key引用
    private final Map<Connection, LoginSession> sessionMap = Collections.synchronizedMap(new HashMap<>()); //会话Map
    MinecraftServer server; //服务器对象
    PlayerList playerList;
    private KeyPair keypair;  //密匙
    static SignatureValidator signatureValidator; //签名效验器
    static Field loginStart_Name;
    static Field loginStart_getUUID;
    static FieldAccessor<GameProfile> loginGameProfile;

    static {
        try{
            gameProfileField = new FieldAccessor<>(ReflectUtil.getFieldFormType(net.minecraft.world.entity.player.Player.class,GameProfile.class));
        }catch (NoSuchFieldException e){
            throw new RuntimeException(e);
        }
        //获取签名验证器
        final DedicatedServer server = NMSUtils.getNmsServer();
        for (Method m : server.getClass().getMethods()) {
            if (m.getParameterTypes().length == 0 && m.getReturnType() == SignatureValidator.class){
                m.setAccessible(true);
                try{
                    signatureValidator = (SignatureValidator) m.invoke(server);
                    break;
                }catch (IllegalAccessException | InvocationTargetException e){
                    e.printStackTrace();
                }
            }
        }

        try{
            loginStart_Name = ReflectUtil.getFieldFormType(ServerboundHelloPacket.class,String.class);
            loginStart_getUUID = ReflectUtil.getFieldFormType(ServerboundHelloPacket.class,UUID.class);
            loginStart_Name.setAccessible(true);
            loginStart_getUUID.setAccessible(true);
        }catch (NoSuchFieldException e){
            throw new RuntimeException(e);
        }
        /*
        //为什么要用method呢？
        findMethod:
        {
            for (Method method : ServerboundHelloPacket.class.getMethods()) {
                if (method.getReturnType().isAssignableFrom(String.class) && method.getParameterTypes().length == 0 && !method.getName().startsWith("to")){
                    loginStart_Name = new MethodInvoker<>(method);
                    break findMethod;
                }
            }
            throw new RuntimeException("Cant find LoginStartGetName");
        }
        findMethod:
        {
            for (Method method : ServerboundHelloPacket.class.getMethods()) {
                if (method.getReturnType().isAssignableFrom(UUID.class) && method.getParameterTypes().length == 0){
                    loginStart_getUUID = new MethodInvoker<>(method);
                    break findMethod;
                }
            }
            throw new RuntimeException("Cant find LoginStartGetName");
        }
*/
        try{
            Field f = ReflectUtil.getFieldFormType(net.minecraft.server.network.ServerLoginPacketListenerImpl.class,GameProfile.class);
            loginGameProfile = new FieldAccessor<>(f);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    /**
     * @noinspection NoTranslation
     */
    private static final Component MISSING_PUBLIC_KEY = Component.translatable("multiplayer.disconnect.missing_public_key");


    public AuthenticateListener() {
        logger = Logger.getLogger("MoeLogin{Authenticate}");
        new Random().nextBytes(token); //随机token
        server = NMSUtils.getNmsServer();
        try{
            Field f;
            //获取服务器密匙
            f = ReflectUtil.getFieldFormType(MinecraftServer.class,KeyPair.class);
            f.setAccessible(true);
            keypair = (KeyPair) f.get(server);

            //获取玩家列表
            f = ReflectUtil.getFieldFormType(MinecraftServer.class,PlayerList.class);
            f.setAccessible(true);
            playerList = (PlayerList) f.get(server);

        }catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    public static GameProfile getGameProfile(LoginListener loginListener) {
        return loginGameProfile.get(loginListener);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void login(final PacketReceiveEvent event) {
        if (Bukkit.getOnlineMode()) return; //如果当前服务器为在线模式跳出
        var packet = event.getPacket();
        if (packet instanceof ServerboundHelloPacket start){
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            //跳过插件发包
            if (manage.isPluginPacket(packet)) return;

            //检查服务器正在关闭
            if (Bukkit.getServer().isStopping()){
                disconnect(event.getNetworkManage(),"服务器正在重启,请稍等一会再重进服务器");
                return;
            }

            //遍历清理Map
            if (!sessionMap.isEmpty()) synchronized (sessionMap) {
                Iterator<Map.Entry<Connection, LoginSession>> it = sessionMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Connection, LoginSession> v = it.next();
                    if (v.getValue().isOnline()) continue;
                    //清理已关闭的会话
                    it.remove();
                }
            }


            String name = start.name();

            if (Setting.DEBUG){
                logger.info("玩家登陆: " + name + "#" + event.getNetworkManage());
            }

            //检查别名并替换为玩家真实名字
            var aliasManage = MoeLogin.plugin.getAliasManage();
            if (aliasManage != null){
                var alias = aliasManage.getPlayer(name);
                if (alias != null){

                    event.setPacket(new ServerboundHelloPacket(alias,MMOCore.getUUID(alias))); //将别名替换为当前名字
                    logger.info("玩家别名登录" + name + "已替换为" + alias + "并且使用离线登录");
//                    name = alias;
                    return; //都用上别名了，应该不会需要正版验证
                }
            }

            //检查重复登录
            if (playerList.getPlayerByName(name) != null){
                disconnect(event.getNetworkManage(),"multiplayer.disconnect.duplicate_login");
                return;
            }
            GameProfile gameProfile = new GameProfile(MMOCore.getUUID(name),name);
            final LoginType loginType = MoeLogin.plugin.getLoginType(event.getNetworkManage());

            //检查玩家是否允许使用登录类型
            DataCon dc = MMOCore.getPlayerData(name);
            if (!MoeLogin.plugin.canLogin(dc,loginType)){
                disconnect(event.getNetworkManage(),"§b当前账号已存在\n" +
                        "§b但是未启用登录方式: §a" + loginType.getName());
                return;
            }

            //检查玩家是否开启正版登录
            if (loginType.isOnline()){
                event.setCancelled(true);
                logger.info("为玩家发送" + loginType.getName() + "验证请求: " + name);
                try{
                    final LoginSession loginSession = new LoginSession(gameProfile,event.getChannelHandleContext(),loginType);
                    loginSession.loginStart(start);
                    sessionMap.put(event.getNetworkManage(),loginSession);
                    //为玩家发送加密会话
                    event.getChannel().writeAndFlush(new ClientboundHelloPacket(Setting.serverId,keypair.getPublic().getEncoded(),token,true));
                }catch (RuntimeException e){
                    String msg = e.getMessage();
                    if (!msg.isBlank()){
                        e.printStackTrace();
                        msg = e.getClass().getSimpleName();
                    }
                    disconnect(event.getNetworkManage(),msg);
                }catch (Exception e){
                    String msg = e.getMessage();
                    if (!msg.isBlank()){
                        msg = e.getClass().getSimpleName();
                    }
                    e.printStackTrace();
                    disconnect(event.getNetworkManage(),msg);
                }
                return;
            }

            logger.info("玩家离线登录: " + gameProfile.getName());

        } else if (packet instanceof ServerboundKeyPacket encryptionBegin){
            //收到加密会话
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            Connection network = event.getNetworkManage();
            LoginSession loginSession = sessionMap.get(network);
            if (loginSession == null){
                return;
            }
            event.setCancelled(true);
            GameProfile gameProfile = loginSession.getGameProfile();
            logger.info("收到玩家返回的会话验证: " + gameProfile.getName());

            SecretKey secretKey;
            String serverId;
            PrivateKey privatekey = keypair.getPrivate();

            try{
                //设置编码器和解码器Key
                secretKey = encryptionBegin.getSecretKey(privatekey);
                //spigot用
//                Cipher cipher = MinecraftEncryption.a(2,secretKey);
//                Cipher cipher1 = MinecraftEncryption.a(1,secretKey);
//                network.a(cipher,cipher1);
                //paper用
                network.setEncryptionKey(secretKey);

                //生成用于会话验证的serverId
                serverId = (new BigInteger(net.minecraft.util.Crypt.digestData(Setting.serverId,keypair.getPublic(),secretKey))).toString(16);
            }catch (CryptException e){
                disconnect(network,"multiplayer.disconnect.invalid_public_key_signature");
                return;
//                throw new IllegalStateException("Protocol error");
            }catch (IllegalStateException e){
                disconnect(network,e.getMessage());
                return;
            }
            authenticatorPool.execute(new Runnable() {
                public void run() {
                    try{
                        ProfileResult profileResult;
                        profileResult = MoeLogin.getMojangAPI().hasJoinedServer(gameProfile,serverId,this.getInetAddress(),loginSession.getYggdrasilUrl());
                        if (profileResult != null){
                            GameProfile onlineProfile = profileResult.profile();
                            loginSession.setOnlineGameProfile(onlineProfile);
                            //验证token
                            if (!encryptionBegin.isChallengeValid(token,privatekey)){
                                throw new IllegalStateException("Protocol error");
                            }
                            if (!event.getChannel().isOpen()){
                                return;
                            }
                            //验证完成,恢复登录状态
                            logger.info("会话验证完成: " + onlineProfile.getName());
                            if (!loginSession.getGameProfile().getName().equalsIgnoreCase(onlineProfile.getName())){
                                logger.warning("会话ID不一致,玩家名字为: " + gameProfile.getName() + ", 会话验证获得的ID为: " + onlineProfile.getName());
                                disconnect(network,"阁下ID与会话ID不一致");
                                return;
                            }

                            //检查正版玩家是否重命名
                            if (loginSession.getType().isMojang()){
                                final String lateName = MoeLogin.plugin.getMojangPlayerManage().getPlayer(onlineProfile.getId());
                                if (lateName != null){
                                    final String nowName = gameProfile.getName();
                                    if (!lateName.equals(nowName)){
                                        MoeLogin.logger.warning("§b正版玩家" + nowName + "曾用名" + lateName);
                                        final DataCon dc = MMOCore.getPlayerData(lateName);
                                        if (dc != null){
                                            FileMan.rename(Bukkit.getConsoleSender(),dc,nowName);
                                        }
                                    }
                                }
                            }


                            ServerboundHelloPacket packet = new ServerboundHelloPacket(gameProfile.getName(),MMOCore.getUUID(gameProfile.getName()));
                            manage.recieveClientPacket(network,packet); //恢复登录状态
                            loginSession.pass();
                            if (Setting.DEBUG){
                                logger.info("会话验证后玩家GameProfile : " + profileResult);
                            }
                        } else {
                            disconnect(event.getNetworkManage(),"multiplayer.disconnect.unverified_username");
                            logger.warning("无法验证用户名: " + gameProfile.getName());
                        }
                    }catch (AuthenticationUnavailableException | MinecraftClientException e){
                        disconnect(event.getNetworkManage(),"multiplayer.disconnect.authservers_down");
                        logger.warning("会话服务器正在维护: " + e.getMessage());
                    }catch (IllegalStateException e){
                        //noinspection CallToPrintStackTrace
                        e.printStackTrace();
                        disconnect(event.getNetworkManage(),e.getMessage());
                    }
                }

                //获取玩家真实Ip地址
                @Nullable
                private InetAddress getInetAddress() {

                    //event.getChannelHandleContext();
                    //return LoginListener.this.server.U() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
                    return null;
                }
            });
        }
    }

    //用于登录完成后设置皮肤
    @EventHandler()
    public void appendProfile(PlayerLoginEvent event) throws IllegalAccessException {
        if (sessionMap.isEmpty()) return;
        Player player = event.getPlayer();
        //Connection network = MoePacketAPI.getInstance().getPlayerPacketManage().getNetworkManage(player);  //这时获取到的Connection为Null
        for (Map.Entry<Connection, LoginSession> entry : sessionMap.entrySet()) {
            LoginSession session = entry.getValue();
            if (session.getGameProfile().getName().equals(player.getName())){
                Connection network;
                network = entry.getKey();
                LoginSession loginSession = sessionMap.get(network);
                if (loginSession != null){
                    GameProfile gameProfile = loginSession.getOnlineGameProfile();
                    if (gameProfile != null){
                        //为玩家应用皮肤
                        net.minecraft.world.entity.player.Player np = (net.minecraft.world.entity.player.Player) NMSUtils.getNmsEntity(player);
                        loginSession.initPropertiesTo(loginSession.getOnlineGameProfile(),gameProfileField.get(np));
                    } else {
                        disconnect(network,"会话验证成功， 但登录过程中出现错误{0}");
                    }
                }
                return;
            }
        }
    }

    //断开连接
    public void disconnect(Connection networkManager,String msg) {
        sessionMap.remove(networkManager);
        if (!MoePacketAPI.getInstance().getPlayerPacketManage().getChannel(networkManager).isOpen()) return;
        try{
            Component ichat = Component.translatable(msg);
            networkManager.send(new ClientboundLoginDisconnectPacket(ichat));
            networkManager.disconnect(ichat);
        }catch (Exception var3){
            logger.warning("Error whilst disconnecting player");
        }
    }

    public Map<Connection, LoginSession> getSessionMap() {
        return sessionMap;
    }

    public KeyPair getKeypair() {
        return keypair;
    }

    public byte[] getToken() {
        return token;
    }

    /*public static @Nullable ProfilePublicKey getPublicKey(Optional<ProfilePublicKey.a> key,LoginSession loginSession,boolean enforce_secure_profile) {
        try{
            if (key.isEmpty()){
                if (enforce_secure_profile){
                    //无公匙
                    throw new RuntimeException("multiplayer.disconnect.missing_public_key");
                } else {
                    return null;
                }
            } else {
                //TODO 等待获取服务器签名验证器
                return ProfilePublicKey.a(signatureValidator,loginSession.getOnlineGameProfile().getId(),key.get(),Duration.ZERO);
            }
        }catch (InsecurePublicKeyException.MissingException var4){
            if (enforce_secure_profile){
                throw new RuntimeException("multiplayer.disconnect.missing_public_key");
            } else {
                return null;
            }
        }catch (ProfilePublicKey.b e){
            throw new RuntimeException(e);
        }
    }*/

    public static class LoginSession {
        private final ChannelHandlerContext channelHandleContext;
        GameProfile gameProfile;
        GameProfile onlineGameProfile = null;
        boolean pass = false;
        private final LoginType type;

        //正版登录
        public LoginSession(GameProfile gameProfile,ChannelHandlerContext channelHandleContext,LoginType type) {
            this.gameProfile = gameProfile;
            this.channelHandleContext = channelHandleContext;
            this.type = type;
        }

        public GameProfile getGameProfile() {
            return gameProfile;
        }

        public synchronized GameProfile getOnlineGameProfile() {
            return onlineGameProfile;
        }

        public synchronized void setOnlineGameProfile(GameProfile onlineGameProfile) {
            this.onlineGameProfile = onlineGameProfile;


//            profilePublicKey = getPublicKey(optionalA,false);
        }

        //应用皮肤
        public void initPropertiesTo(GameProfile olinProfile,GameProfile gameProfile) {
            final PropertyMap properties = gameProfile.getProperties();
            for (Map.Entry<String, Collection<Property>> entry : olinProfile.getProperties().asMap().entrySet()) {
                Iterator<Property> it = entry.getValue().iterator();
                if (it.hasNext()){
                    properties.put(entry.getKey(),it.next());
                    MoeLogin.logger.info("已应用Properties: " + entry.getKey());
                }
            }
        }

        public synchronized boolean isOnline() {
            return channelHandleContext.channel().isOpen();
        }

        public synchronized boolean isPass() {
            return pass;
        }

        synchronized boolean pass() {
            return pass = true;
            //正版的话发送join报文
            //todo 还没找到使用这个方法的地方，没有参考对象
//            if(yggdrasil == null){
//                final GameProfile profile = getOnlineGameProfile();
//                MoeLogin.getMojangAPI().joinServer(profile.getId(),null,);
//            }
        }

        public LoginType getType() {
            return type;
        }

        public String getYggdrasilUrl() {
            return getType().getYggdrasilUrl();
        }

        public void loginStart(ServerboundHelloPacket login) {
        }
    }

}
