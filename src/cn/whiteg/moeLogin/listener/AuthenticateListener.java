package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.reflection.FieldAccessor;
import cn.whiteg.mmocore.reflection.MethodInvoker;
import cn.whiteg.mmocore.reflection.ReflectUtil;
import cn.whiteg.mmocore.util.NMSUtils;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.PlayerPacketManage;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.LoginListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Duration;
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
    private static FieldAccessor<GameProfile> gameProfileField;


    private final byte[] token = new byte[4];
    private final Logger logger;
    //private Map<String, LoginSession> sessionMap = Collections.synchronizedMap(new MapMaker().weakKeys().makeMap());  //弱Key引用
    private final Map<NetworkManager, LoginSession> sessionMap = Collections.synchronizedMap(new HashMap<>()); //会话Map
    MinecraftServer server; //服务器对象
    PlayerList playerList;
    private KeyPair keypair;  //密匙
    static SignatureValidator signatureValidator; //签名效验器
    static MethodInvoker<String> loginStart_Name;
    static MethodInvoker<Optional<UUID>> loginStart_getUUID;
    static FieldAccessor<GameProfile> loginGameProfile;

    static {
        try{
            gameProfileField = new FieldAccessor<>(ReflectUtil.getFieldFormType(EntityHuman.class,GameProfile.class));
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

        findMethod:
        {
            for (Method method : PacketLoginInStart.class.getMethods()) {
                if (method.getReturnType().isAssignableFrom(String.class) && method.getParameterTypes().length == 0 && !method.getName().startsWith("to")){
                    loginStart_Name = new MethodInvoker<>(method);
                    break findMethod;
                }
            }
            throw new RuntimeException("Cant find LoginStartGetName");
        }
        findMethod:
        {
            for (Method method : PacketLoginInStart.class.getMethods()) {
                if (method.getReturnType().isAssignableFrom(Optional.class) && method.getParameterTypes().length == 0){
                    loginStart_getUUID = new MethodInvoker<>(method);
                    break findMethod;
                }
            }
            throw new RuntimeException("Cant find LoginStartGetName");
        }

        try{
            Field f = ReflectUtil.getFieldFormType(net.minecraft.server.network.LoginListener.class,GameProfile.class);
            loginGameProfile = new FieldAccessor<>(f);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    private static final IChatBaseComponent MISSING_PUBLIC_KEY = IChatBaseComponent.c("multiplayer.disconnect.missing_public_key");


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
        if (packet instanceof PacketLoginInStart start){
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            //跳过插件发包
            if (manage.isPluginPacket(packet)) return;

            //检查服务器正在关闭
//            if (Bukkit.getServer().isStopping()){
//                disconnect(event.getNetworkManage(),"服务器正在重启,请稍等一会再重进服务器");
//                return;
//            }

            //遍历清理Map
            if (!sessionMap.isEmpty()) synchronized (sessionMap) {
                Iterator<Map.Entry<NetworkManager, LoginSession>> it = sessionMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<NetworkManager, LoginSession> v = it.next();
                    if (v.getValue().isOnline()) continue;
                    //清理已关闭的会话
                    it.remove();
                }
            }


            String name;
            name = loginStart_Name.invoke(packet);

            if (Setting.DEBUG){
                logger.info("玩家登陆: " + name + "#" + event.getNetworkManage());
            }

            //检查别名并替换为玩家真实名字
            var aliasManage = MoeLogin.plugin.getAliasManage();
            if (aliasManage != null){
                var alias = aliasManage.getPlayer(name);
                if (alias != null){

                    event.setPacket(new PacketLoginInStart(alias,Optional.of(MMOCore.getUUID(alias)))); //将别名替换为当前名字
                    logger.info("玩家别名登录" + name + "已替换为" + alias + "并且使用离线登录");
//                    name = alias;
                    //都用上别名了，应该不会需要正版验证
                    return;
                }
            }

            //检查重复登录
            if (playerList.a(name) != null){
                disconnect(event.getNetworkManage(),"multiplayer.disconnect.duplicate_login");
                return;
            }
            GameProfile gameProfile = new GameProfile(null,name);
            //检查玩家是否开启正版登录
            if (MoeLogin.plugin.isPremium(name)){
                event.setCancelled(true);
                logger.info("为玩家发送正版验证请求: " + name);
                try{
                    final LoginSession loginSession = new LoginSession(gameProfile,event.getChannelHandleContext());
                    loginSession.loginStart(start);
                    sessionMap.put(event.getNetworkManage(),loginSession);
                    //为玩家发送加密会话
                    event.getChannel().writeAndFlush(new PacketLoginOutEncryptionBegin("",keypair.getPublic().getEncoded(),token));
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

            //检查玩家是否开启外置登录
            String yggdrasil = MoeLogin.plugin.getYggdrasil(name);
            if (yggdrasil != null){
                String baseUrl = Setting.yggdrasilMap.get(yggdrasil);
                if (baseUrl != null){
                    event.setCancelled(true);
                    logger.info("为玩家发送外置登录会话验证: " + name + ", 外置服务器为: " + yggdrasil);
                    final LoginSession loginSession = new LoginSession(gameProfile,event.getChannelHandleContext(),yggdrasil,baseUrl);
                    loginSession.loginStart(start);
                    sessionMap.put(event.getNetworkManage(),loginSession);
                    //为玩家发送加密会话
                    event.getChannel().writeAndFlush(new PacketLoginOutEncryptionBegin("",keypair.getPublic().getEncoded(),token));
                } else {
                    MoeLogin.plugin.setYggdrasil(name,null);
                    logger.warning("无效外置登录: " + yggdrasil);
                }
                return;
            }

            logger.info("玩家离线登录: " + gameProfile.getName());

        } else if (packet instanceof PacketLoginInEncryptionBegin encryptionBegin){
            //收到加密会话
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            NetworkManager network = event.getNetworkManage();
            LoginSession loginSession = sessionMap.get(network);
            if (loginSession == null){
                return;
            }
            event.setCancelled(true);
            GameProfile gameProfile = loginSession.getGameProfile();
            logger.info("收到玩家返回的会话验证: " + gameProfile.getName());

            SecretKey secretKey;
            String s;
            PrivateKey privatekey = keypair.getPrivate();

            try{
                //设置编码器和解码器Key
                secretKey = encryptionBegin.a(privatekey);
                //spigot用
//                Cipher cipher = MinecraftEncryption.a(2,secretKey);
//                Cipher cipher1 = MinecraftEncryption.a(1,secretKey);
//                network.a(cipher,cipher1);
                //paper用
                network.setupEncryption(secretKey);

                s = (new BigInteger(MinecraftEncryption.a("",keypair.getPublic(),secretKey))).toString(16); //这个意味不明


                /*
                //旧的验证token
                if (!Arrays.equals(token,encryptionBegin.b(privatekey))){
                    throw new IllegalStateException("Protocol error");
                }*/
            }catch (CryptographyException e){
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
                        GameProfile onlineGameProfile;
                        if (loginSession.yggdrasil == null)
                            onlineGameProfile = MoeLogin.getMojangAPI().hasJoinedServer(gameProfile,s,this.getInetAddress());
                        else
                            onlineGameProfile = MoeLogin.getMojangAPI().hasJoinedServer(gameProfile,s,this.getInetAddress(),loginSession.getYggdrasilUrl());
                        loginSession.setOnlineGameProfile(onlineGameProfile);
                        if (onlineGameProfile != null){
                            //验证token
                            if (!encryptionBegin.a(token,privatekey)){
                                throw new IllegalStateException("Protocol error");
                            }
                            if (!event.getChannel().isOpen()){
                                return;
                            }
                            //验证完成,恢复登录状态
                            logger.info("会话验证完成: " + onlineGameProfile.getName());
                            if (!loginSession.getGameProfile().getName().equalsIgnoreCase(onlineGameProfile.getName())){
                                logger.warning("会话ID不一致,玩家名字为: " + gameProfile.getName() + ", 会话验证获得的ID为: " + onlineGameProfile.getName());
                                disconnect(network,"阁下ID与会话ID不一致");
                                return;
                            }
                            PacketLoginInStart packet = new PacketLoginInStart(gameProfile.getName(),Optional.of(MMOCore.getUUID(gameProfile.getName())));
                            manage.recieveClientPacket(network,packet); //恢复登录状态
                            loginSession.pass();
                            if (Setting.DEBUG){
                                logger.info("会话验证后玩家GameProfile : " + onlineGameProfile);
                            }
                        } else {
                            disconnect(event.getNetworkManage(),"multiplayer.disconnect.unverified_username");
                            logger.warning("无法验证用户名: " + gameProfile.getName());
                        }
                    }catch (AuthenticationUnavailableException var3){
                        disconnect(event.getNetworkManage(),"multiplayer.disconnect.authservers_down");
                        logger.warning("会话服务器正在维护: " + gameProfile.getName());
                    }catch (IllegalStateException e){
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
        //NetworkManager network = MoePacketAPI.getInstance().getPlayerPacketManage().getNetworkManage(player);  //这时获取到的NetworkManager为Null
        for (Map.Entry<NetworkManager, LoginSession> entry : sessionMap.entrySet()) {
            LoginSession session = entry.getValue();
            if (session.getGameProfile().getName().equals(player.getName())){
                NetworkManager network;
                network = entry.getKey();
                LoginSession loginSession = sessionMap.get(network);
                if (loginSession != null){
                    GameProfile gameProfile = loginSession.getOnlineGameProfile();
                    if (gameProfile != null){
                        //为玩家应用皮肤
                        EntityPlayer np = (EntityPlayer) NMSUtils.getNmsEntity(player);
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
    public void disconnect(NetworkManager networkManager,String msg) {
        sessionMap.remove(networkManager);
        if (!MoePacketAPI.getInstance().getPlayerPacketManage().getChannel(networkManager).isOpen()) return;
        try{
            IChatBaseComponent ichat = IChatBaseComponent.c(msg);
            networkManager.a(new PacketLoginOutDisconnect(ichat));
            networkManager.a(ichat);
        }catch (Exception var3){
            logger.warning("Error whilst disconnecting player");
        }
    }

    public Map<NetworkManager, LoginSession> getSessionMap() {
        return sessionMap;
    }

    public KeyPair getKeypair() {
        return keypair;
    }

    public byte[] getToken() {
        return token;
    }

    public static @Nullable ProfilePublicKey getPublicKey(Optional<ProfilePublicKey.a> key,LoginSession loginSession,boolean enforce_secure_profile) {
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
    }

    public static class LoginSession {
        private final String yggdrasil;
        private final String yggdrasilUrl;
        private final ChannelHandlerContext channelHandleContext;
        GameProfile gameProfile;
        GameProfile onlineGameProfile = null;
        boolean pass = false;

        //正版登录
        public LoginSession(GameProfile gameProfile,ChannelHandlerContext channelHandleContext) {
            this.gameProfile = gameProfile;
            this.channelHandleContext = channelHandleContext;
            this.yggdrasil = null;
            this.yggdrasilUrl = null;
        }


        //外置登录
        public LoginSession(GameProfile gameProfile,ChannelHandlerContext channelHandleContext,String yggdrasil,String yggdrasilUrl) {
            this.gameProfile = gameProfile;
            this.channelHandleContext = channelHandleContext;
            this.yggdrasil = yggdrasil;
            this.yggdrasilUrl = yggdrasilUrl;
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
        }

        public String getYggdrasil() {
            return yggdrasil;
        }

        public String getYggdrasilUrl() {
            return yggdrasilUrl;
        }

        public void loginStart(PacketLoginInStart login) {
        }
    }

}
