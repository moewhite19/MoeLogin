package cn.whiteg.moeLogin.listener;

import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moeLogin.utils.NMSUtils;
import cn.whiteg.moepacketapi.MoePacketAPI;
import cn.whiteg.moepacketapi.PlayerPacketManage;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import cn.whiteg.moepacketapi.api.event.PacketSendEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.properties.Property;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.lang.reflect.Field;
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
    //正版验证线程队列
    private static final ExecutorService authenticatorPool = Executors.newCachedThreadPool((r) -> {
        return new Thread(r,"User Authenticator #" + threadId.incrementAndGet());
    });
    private final byte[] token = new byte[4];
    private final Logger logger;
    //private Map<String, LoginSession> sessionMap = Collections.synchronizedMap(new MapMaker().weakKeys().makeMap());  //弱Key引用
    private final Map<NetworkManager, LoginSession> sessionMap = Collections.synchronizedMap(new HashMap<>()); //会话Map
    MinecraftServer server; //服务器对象
    private KeyPair keypair;  //密匙


    public AuthenticateListener() {
        logger = Logger.getLogger("MoeLogin{Authenticate}");
        try{
            server = ((CraftServer) Bukkit.getServer()).getServer();
            Field f = NMSUtils.getFieldFormType(MinecraftServer.class,KeyPair.class);
            f.setAccessible(true);
            keypair = (KeyPair) f.get(server);
        }catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    public static GameProfile getGameProfile(LoginListener loginListener) throws NoSuchFieldException, IllegalAccessException {
        Field f = NMSUtils.getFieldFormType(net.minecraft.server.network.LoginListener.class,GameProfile.class);
        f.setAccessible(true);
        return (GameProfile) f.get(loginListener);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void login(final PacketReceiveEvent event) {
        var packet = event.getPacket();
        if (Bukkit.getOnlineMode()) return; //如果当前服务器为在线模式跳出
        if (packet instanceof PacketLoginInStart start){
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            //跳过插件发包
            if (manage.isPluginPacket(packet)) return;

            //遍历清理Map
            synchronized (sessionMap) {
                if (!sessionMap.isEmpty()){
                    Iterator<Map.Entry<NetworkManager, LoginSession>> it = sessionMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<NetworkManager, LoginSession> v = it.next();
                        if (v.getValue().isOnline()) continue;
                        //清理已关闭的会话
                        it.remove();
                    }
                }
            }

            GameProfile gameProfile = start.b();

            if (Setting.DEBUG){
                logger.info("玩家登陆会话ID为: " + event.getNetworkManage());
                logger.info("UUID: " + event.getNetworkManage().spoofedUUID);
            }

            //检查玩家是否开启正版登录
            if (MoeLogin.plugin.isPremium(gameProfile.getName())){
                event.setCancelled(true);
                logger.info("为玩家发送正版验证请求: " + gameProfile.getName());
                sessionMap.put(event.getNetworkManage(),new LoginSession(gameProfile,event.getChannelHandleContext()));
                PacketLoginOutEncryptionBegin encryptionBegin = new PacketLoginOutEncryptionBegin("",keypair.getPublic().getEncoded(),token);
                event.getChannel().writeAndFlush(encryptionBegin);
                return;
            }

            //检查玩家是否开启外置登录
            String yggdrasil = MoeLogin.plugin.getYggdrasil(gameProfile.getName());
            if (yggdrasil != null){
                String baseUrl = Setting.yggdrasilMap.get(yggdrasil);
                if (baseUrl != null){
                    event.setCancelled(true);
                    logger.info("为玩家发送外置登录会话验证: " + gameProfile.getName() + ", 外置服务器为: " + yggdrasil);
                    sessionMap.put(event.getNetworkManage(),new LoginSession(gameProfile,event.getChannelHandleContext(),yggdrasil,baseUrl));
                    PacketLoginOutEncryptionBegin encryptionBegin = new PacketLoginOutEncryptionBegin("",keypair.getPublic().getEncoded(),token);
                    event.getNetworkManage().sendPacket(encryptionBegin);
                } else {
                    MoeLogin.plugin.setYggdrasil(gameProfile.getName(),null);
                    logger.warning("无效外置登录: " + yggdrasil);
                }
                return;
            }
            logger.info("玩家离线登录: " + gameProfile.getName());

        } else if (packet instanceof PacketLoginInEncryptionBegin encryptionBegin){
            PlayerPacketManage manage = MoePacketAPI.getInstance().getPlayerPacketManage();
            NetworkManager network = event.getNetworkManage();
            LoginSession loginSession = sessionMap.get(network);
            if (loginSession == null){
                return;
            }
            event.setCancelled(true);
            GameProfile gameProfile = loginSession.getGameProfile();
            logger.info("收到玩家返回的会话验证: " + gameProfile.getName());

            SecretKey loginKey;
            loginKey = null;
            String s;
            PrivateKey privatekey = keypair.getPrivate();

            try{
                loginKey = encryptionBegin.a(privatekey);
                Cipher cipher = MinecraftEncryption.a(2,loginKey);
                Cipher cipher1 = MinecraftEncryption.a(1,loginKey);
                network.a(cipher,cipher1);//设置编码器和解码器Key
                s = (new BigInteger(MinecraftEncryption.a("",keypair.getPublic(),loginKey))).toString(16);
                if (!Arrays.equals(token,encryptionBegin.b(privatekey))){
                    throw new IllegalStateException("Protocol error");
                }
            }catch (CryptographyException e){
                disconnect(network,"获取login key出现错误");
                throw new IllegalStateException("Protocol error");
            }
//            network.a(loginKey);

            authenticatorPool.execute(new Runnable() {
                public void run() {
                    try{
                        GameProfile oloneGameProfile;
                        if (loginSession.yggdrasil == null)
                            oloneGameProfile = MoeLogin.getMojangAPI().hasJoinedServer(gameProfile,s,this.getInetAddress());
                        else
                            oloneGameProfile = MoeLogin.getMojangAPI().hasJoinedServer(gameProfile,s,this.getInetAddress(),loginSession.getYggdrasilUrl());
                        loginSession.setOloneGameProfile(oloneGameProfile);
                        if (oloneGameProfile != null){
                            if (!event.getChannel().isOpen()){
                                return;
                            }
                            //验证完成,恢复登录状态
                            logger.info("会话验证完成: " + oloneGameProfile.getName());
                            if (!loginSession.getGameProfile().getName().equalsIgnoreCase(oloneGameProfile.getName())){
                                logger.warning("会话ID不一致,玩家名字为: " + gameProfile.getName() + ", 会话验证获得的ID为: " + oloneGameProfile.getName());
                                disconnect(network,"阁下ID与会话ID不一致");
                                return;
                            }
                            PacketLoginInStart packet = new PacketLoginInStart(gameProfile);
                            manage.recieveClientPacket(network,packet); //恢复登录状态
                            loginSession.pass = true; //验证完成
                            if (Setting.DEBUG){
                                logger.info("会话验证后玩家GameProfile : " + oloneGameProfile);
                            }
                        } else {
                            disconnect(event.getNetworkManage(),"multiplayer.disconnect.unverified_username");
                            logger.warning("无法验证用户名: " + gameProfile.getName());
                        }
                    }catch (AuthenticationUnavailableException var3){
                        disconnect(event.getNetworkManage(),"multiplayer.disconnect.authservers_down");
                        logger.warning("会话服务器正在维护: " + gameProfile.getName());
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
    //@EventHandler(ignoreCancelled = true)
    public void appendProfile(PacketSendEvent event) {
        if (event.getPacket() instanceof PacketLoginOutSuccess){
            if (!event.getChannel().isOpen()) return;
            LoginSession loginSession = sessionMap.get(event.getNetworkManage());
            if (loginSession != null){
                GameProfile gameProfile = loginSession.getOloneGameProfile();
                if (gameProfile != null){
                    NetworkManager networkManager = event.getNetworkManage();
                    //为玩家应用皮肤
                    PacketListener listener = networkManager.j();
                    GameProfile profile;
                    if (listener instanceof LoginListener i){
                        try{
                            profile = getGameProfile(i);
                        }catch (NoSuchFieldException | IllegalAccessException e){
                            e.printStackTrace();
                            return; //出错了就返回吧
                        }
                        loginSession.initPropertiesTo(loginSession.getOloneGameProfile(),profile);
//                            logger.info("玩家档案: " + profile);
                    } else if (listener instanceof PlayerConnection){
                        EntityPlayer player = ((PlayerConnection) listener).b;
                        loginSession.initPropertiesTo(loginSession.getOloneGameProfile(),player.getProfile());
                    } else {
                        logger.warning("未知事件类: " + listener);
                    }
//                    logger.info("玩家已验证完成:" + gameProfile.getName());
                } else {
                    disconnect(event.getNetworkManage(),"会话验证成功， 但登录过程中出现错误{0}");
                }
            }
        }
    }


    @EventHandler()
    public void appendProfile(PlayerLoginEvent event) {
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
                    GameProfile gameProfile = loginSession.getOloneGameProfile();
                    if (gameProfile != null){
                        //为玩家应用皮肤
                        EntityPlayer np = ((CraftPlayer) player).getHandle();
                        loginSession.initPropertiesTo(loginSession.getOloneGameProfile(),np.getProfile());
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
        if (!networkManager.isConnected()) return;
        try{
            IChatBaseComponent ichat = new ChatMessage(msg);
            networkManager.sendPacket(new PacketLoginOutDisconnect(ichat));
            networkManager.close(ichat);
        }catch (Exception var3){
            logger.warning("Error whilst disconnecting player");
        }
    }

    public Map<NetworkManager, LoginSession> getSessionMap() {
        return sessionMap;
    }

    public static class LoginSession {
        private final String yggdrasil;
        private final String yggdrasilUrl;
        private final ChannelHandlerContext channelHandleContext;
        GameProfile gameProfile;
        volatile GameProfile oloneGameProfile = null;
        volatile boolean pass = false;

        public LoginSession(GameProfile gameProfile,ChannelHandlerContext channelHandleContext) {
            this.gameProfile = gameProfile;
            this.channelHandleContext = channelHandleContext;
            this.yggdrasil = null;
            this.yggdrasilUrl = null;
        }

        public LoginSession(GameProfile gameProfile,ChannelHandlerContext channelHandleContext,String yggdrasil,String yggdrasilUrl) {
            this.gameProfile = gameProfile;
            this.channelHandleContext = channelHandleContext;
            this.yggdrasil = yggdrasil;
            this.yggdrasilUrl = yggdrasilUrl;
        }

        public void LoginSession(GameProfile gameProfile) {
            this.gameProfile = gameProfile;
        }

        public GameProfile getGameProfile() {
            return gameProfile;
        }

        public synchronized GameProfile getOloneGameProfile() {
            return oloneGameProfile;
        }

        public synchronized void setOloneGameProfile(GameProfile oloneGameProfile) {
            this.oloneGameProfile = oloneGameProfile;
        }

        //应用皮肤
        public void initPropertiesTo(GameProfile olinProfile,GameProfile gameProfile) {
            for (Map.Entry<String, Collection<Property>> entry : olinProfile.getProperties().asMap().entrySet()) {
                Iterator<Property> it = entry.getValue().iterator();
                if (it.hasNext()){
                    gameProfile.getProperties().put(entry.getKey(),it.next());
                    MoeLogin.logger.info("已应用Properties: " + entry.getKey());
                }
            }
        }

        public boolean isOnline() {
            return channelHandleContext.channel().isOpen();
        }

        public boolean isPass() {
            return pass;
        }

        public String getYggdrasil() {
            return yggdrasil;
        }


        public String getYggdrasilUrl() {
            return yggdrasilUrl;
        }
    }

}
