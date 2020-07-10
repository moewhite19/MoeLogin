//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// 游戏自带的登录验证
// 以后获取会替换游戏自带的登录验证来实现正版验证和外置登录
//

package cn.whiteg.moeLogin.listener;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.properties.Property;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_16_R3.LoginListener;
import net.minecraft.server.v1_16_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.Validate;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;
import org.spigotmc.SpigotConfig;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class NmsLoginListener extends LoginListener {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random random = new Random();
    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final ExecutorService authenticatorPool = Executors.newCachedThreadPool((r) -> {
        return new Thread(r,"User Authenticator #" + threadId.incrementAndGet());
    });
    public static Map<UUID, EntityPlayer> uuidMap = null;
    public static Map<UUID, EntityPlayer> pendingPlayers = null;
    private static CraftServer cserver = null;
    private final byte[] e = new byte[4];
    private final MinecraftServer server;
    private NmsLoginListener.EnumProtocolState state;
    private int timeOut;
    private GameProfile gameProfile;
    private SecretKey loginKey;
    private EntityPlayer entityPlayer;
    private int velocityLoginMessageId = -1;

    public NmsLoginListener(MinecraftServer minecraftserver,NetworkManager networkmanager) {
        super(minecraftserver,networkmanager);
        this.state = NmsLoginListener.EnumProtocolState.HELLO;
        this.server = minecraftserver;
        random.nextBytes(this.e);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    private void setGameProfile(GameProfile profile) {
        this.gameProfile = profile;
    }

    @Override
    public void tick() {
        if (!server.isRunning()){
            this.disconnect(new ChatMessage("服务器正在启动中"));
        } else {
            if (this.state == NmsLoginListener.EnumProtocolState.READY_TO_ACCEPT){
                if (this.networkManager.isConnected()){
                    this.ReadyLogin();
                }
            } else if (this.state == NmsLoginListener.EnumProtocolState.DELAY_ACCEPT){
                EntityPlayer entityplayer = getActivePlayer(this.gameProfile.getId());
                if (entityplayer == null){
                    this.state = NmsLoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    this.server.getPlayerList().a(this.networkManager,this.entityPlayer);
                    this.entityPlayer = null;
                }
            }

            if (this.timeOut++ == 600){
                this.disconnect(new ChatMessage("multiplayer.disconnect.slow_login"));
            }

        }
    }


    //获取NetworkManager
    public NetworkManager a() {
        return this.networkManager;
    }

    public void disconnect(String s) {
        disconnect(new ChatComponentText(s));
    }

    public void disconnect(IChatBaseComponent ichatbasecomponent) {
        try{
            LOGGER.info("Disconnecting {}: {}",this.d(),ichatbasecomponent.getString());
            this.networkManager.sendPacket(new PacketLoginOutDisconnect(ichatbasecomponent));
            this.networkManager.close(ichatbasecomponent);
        }catch (Exception var3){
            LOGGER.error("Error whilst disconnecting player",var3);
        }

    }

    public void initUUID() {
        UUID uuid;
        if (this.networkManager.spoofedUUID != null){
            uuid = this.networkManager.spoofedUUID;
        } else {
            uuid = EntityHuman.getOfflineUUID(this.gameProfile.getName());
        }

        this.gameProfile = new GameProfile(uuid,this.gameProfile.getName());
        if (this.networkManager.spoofedProfile != null){
            Property[] var2 = this.networkManager.spoofedProfile;
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Property property = var2[var4];
                this.gameProfile.getProperties().put(property.getName(),property);
            }
        }

    }

    //准备登录
    public void ReadyLogin() {
        EntityPlayer s = attemptLogin(this.gameProfile,this.hostname);
        if (s != null){
            this.state = NmsLoginListener.EnumProtocolState.ACCEPTED;
            if (this.server.av() >= 0 && !this.networkManager.isLocal()){
                this.networkManager.sendPacket(new PacketLoginOutSetCompression(this.server.av()),(channelfuture) -> {
                    this.networkManager.setCompressionLevel(this.server.av());
                });
            }

            this.networkManager.sendPacket(new PacketLoginOutSuccess(this.gameProfile));
            EntityPlayer entityplayer = getActivePlayer(this.gameProfile.getId());
            if (entityplayer != null){
                this.state = NmsLoginListener.EnumProtocolState.DELAY_ACCEPT;
                this.entityPlayer = this.server.getPlayerList().processLogin(this.gameProfile,s);
            } else {
                this.server.getPlayerList().a(this.networkManager,this.server.getPlayerList().processLogin(this.gameProfile,s));
            }
        }
    }


    //获取UUID玩家Map
    public Map<UUID, EntityPlayer> getUUIDMap() {
        if (uuidMap == null){
            try{
                uuidMap = (Map<UUID, EntityPlayer>) PlayerList.class.getDeclaredField("j").get(server.getPlayerList());
            }catch (IllegalAccessException | NoSuchFieldException illegalAccessException){
                illegalAccessException.printStackTrace();
            }
        }
        return uuidMap;
    }

    //获取活跃玩家
    EntityPlayer getActivePlayer(UUID uuid) {
        EntityPlayer player = getUUIDMap().get(uuid);
        return player != null ? player : getPendingPlayers().get(uuid);
    }

    //获取等待中玩家
    public Map<UUID, EntityPlayer> getPendingPlayers() {
        if (pendingPlayers == null){
            try{
                pendingPlayers = (Map<UUID, EntityPlayer>) PlayerList.class.getDeclaredField("pendingPlayers").get(server.getPlayerList());
            }catch (IllegalAccessException | NoSuchFieldException e){
                e.printStackTrace();
            }
        }
        return pendingPlayers;
    }

    //获取Craftbukkit服务器
    CraftServer getCraftServer() {
        if (cserver == null){
            try{
                cserver = (CraftServer) PlayerList.class.getDeclaredField("cserver").get(server.getPlayerList());
            }catch (IllegalAccessException | NoSuchFieldException e){
                e.printStackTrace();
            }
        }
        return cserver;
    }

    //kick玩家
    void disconnectPendingPlayer(EntityPlayer entityplayer) {
        ChatMessage msg = new ChatMessage("multiplayer.disconnect.duplicate_login",new Object[0]);
        entityplayer.networkManager.sendPacket(new PacketPlayOutKickDisconnect(msg),(future) -> {
            entityplayer.networkManager.close(msg);
            entityplayer.networkManager = null;
        });
    }

    //会话验证完成开始登录
    public EntityPlayer attemptLogin(GameProfile gameprofile,String hostname) {
        UUID uuid = EntityHuman.a(gameprofile);
        List<EntityPlayer> list = Lists.newArrayList();

        EntityPlayer entityplayer;
        List<EntityPlayer> players = server.getPlayerList().players;
        for (int i = 0; i < players.size(); ++i) {
            entityplayer = players.get(i);
            if (entityplayer.getUniqueID().equals(uuid)){
                list.add(entityplayer);
            }
        }
        Map<UUID, EntityPlayer> pendingPlayers = getPendingPlayers();
        entityplayer = pendingPlayers.get(uuid);
        if (entityplayer != null){
            pendingPlayers.remove(uuid);
            disconnectPendingPlayer(entityplayer);
        }

        Iterator<EntityPlayer> iterator = list.iterator();

        while (iterator.hasNext()) {
            entityplayer = iterator.next();
            //server.getPlayerList().savePlayerFile(entityplayer);
            entityplayer.playerConnection.disconnect(new ChatMessage("multiplayer.disconnect.duplicate_login",new Object[0]));
        }

        SocketAddress socketaddress = networkManager.getSocketAddress();
        EntityPlayer entity = new EntityPlayer(this.server,this.server.getWorldServer(World.OVERWORLD),gameprofile,new PlayerInteractManager(this.server.getWorldServer(World.OVERWORLD)));
        entity.isRealPlayer = true;
        Player player = entity.getBukkitEntity();
        //Bukkit玩家登陆事件
        PlayerLoginEvent event = new PlayerLoginEvent(player,hostname,((InetSocketAddress) socketaddress).getAddress(),((InetSocketAddress) networkManager.getRawAddress()).getAddress());
        ChatMessage chatmessage;
        GameProfileBanEntry gameprofilebanentry;
        //检查是否被ban
        if (server.getPlayerList().getProfileBans().isBanned(gameprofile) && (gameprofilebanentry = server.getPlayerList().getProfileBans().get(gameprofile)) != null){
            chatmessage = new ChatMessage("multiplayer.disconnect.banned.reason",gameprofilebanentry.getReason());
            if (gameprofilebanentry.getExpires() != null){
                chatmessage.addSibling(new ChatMessage("multiplayer.disconnect.banned.expiration",DATE_FORMAT.format(gameprofilebanentry.getExpires())));
            }

//            if (!gameprofilebanentry.hasExpired()){
//                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,CraftChatMessage.fromComponent(chatmessage));
//            }
        }
        //检查是否开启白名单
        else if (!server.getPlayerList().isWhitelisted(gameprofile,event)){
            new ChatMessage("multiplayer.disconnect.not_whitelisted");
        }
        //检查BanIP
//        else if (server.getPlayerList().getIPBans().isBanned(socketaddress) && server.getPlayerList().getIPBans().get(socketaddress) != null && !server.getPlayerList().getIPBans().get(socketaddress).hasExpired()){
//            IpBanEntry ipbanentry = server.getPlayerList().l.get(socketaddress);
//            chatmessage = new ChatMessage("multiplayer.disconnect.banned_ip.reason",new Object[]{ipbanentry.getReason()});
//            if (ipbanentry.getExpires() != null){
//                chatmessage.addSibling(new ChatMessage("multiplayer.disconnect.banned_ip.expiration",new Object[]{g.format(ipbanentry.getExpires())}));
//            }
//            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,CraftChatMessage.fromComponent(chatmessage));
//        }
        //检查在线玩家人数,满了踢出
        else if (players.size() >= server.getPlayerList().getMaxPlayers()){
            event.disallow(PlayerLoginEvent.Result.KICK_FULL,SpigotConfig.serverFullMessage);
        }
        //call插件事件
        getCraftServer().getPluginManager().callEvent(event);
        //如果被插件拒绝登录
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED){
            disconnect(event.getKickMessage());
            return null;
        } else {
            return entity;
        }
    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent) {
        LOGGER.info("{} lost connection: {}",this.d(),ichatbasecomponent.getString());
    }

    @Override
    public String d() {
        return this.gameProfile != null ? this.gameProfile + " (" + this.networkManager.getSocketAddress() + ")" : String.valueOf(this.networkManager.getSocketAddress());
    }

    @Override
    public void a(PacketLoginInStart packetlogininstart) {
        Validate.validState(this.state == NmsLoginListener.EnumProtocolState.HELLO,"Unexpected hello packet",new Object[0]);
        this.gameProfile = packetlogininstart.b();
        if (this.server.getOnlineMode() && !this.networkManager.isLocal()){
            this.state = NmsLoginListener.EnumProtocolState.KEY;
            this.networkManager.sendPacket(new PacketLoginOutEncryptionBegin("",this.server.getKeyPair().getPublic(),this.e));
        } else {
            if (PaperConfig.velocitySupport){
                this.velocityLoginMessageId = ThreadLocalRandom.current().nextInt();
                PacketLoginOutCustomPayload packet = new PacketLoginOutCustomPayload(this.velocityLoginMessageId,VelocityProxy.PLAYER_INFO_CHANNEL,new PacketDataSerializer(Unpooled.EMPTY_BUFFER));
                this.networkManager.sendPacket(packet);
                return;
            }

            authenticatorPool.execute(new Runnable() {
                public void run() {
                    try{
                        initUUID();
                        new LoginHandler().fireEvents();
                    }catch (Exception var2){
                        disconnect("Failed to verify username!");
                        server.server.getLogger().log(Level.WARNING,"Exception verifying " + gameProfile.getName(),var2);
                    }

                }
            });
        }

    }

    //收到握手密匙
    @Override
    public void a(PacketLoginInEncryptionBegin packetlogininencryptionbegin) {
        Validate.validState(this.state == NmsLoginListener.EnumProtocolState.KEY,"Unexpected key packet");
        PrivateKey privatekey = this.server.getKeyPair().getPrivate();
        if (!Arrays.equals(this.e,packetlogininencryptionbegin.b(privatekey))){
            throw new IllegalStateException("Invalid nonce!");
        } else {
            this.loginKey = packetlogininencryptionbegin.a(privatekey);
            this.state = NmsLoginListener.EnumProtocolState.AUTHENTICATING;
            this.networkManager.a(this.loginKey); //设置会话加密key
            authenticatorPool.execute(new Runnable() {
                public void run() {
                    GameProfile gameprofile = gameProfile;
                    try{
                        String s = (new BigInteger(MinecraftEncryption.a("",server.getKeyPair().getPublic(),loginKey))).toString(16);
                        gameProfile = server.getMinecraftSessionService().hasJoinedServer(new GameProfile(null,gameprofile.getName()),s,this.a());
                        if (gameProfile != null){
                            if (!networkManager.isConnected()){
                                return;
                            }

                            new LoginHandler().fireEvents();
                        } else if (server.isEmbeddedServer()){
                            NmsLoginListener.LOGGER.warn("Failed to verify username but will let them in anyway!");
                            gameProfile = NmsLoginListener.this.a(gameprofile);
                            state = NmsLoginListener.EnumProtocolState.READY_TO_ACCEPT;
                        } else {
                            disconnect(new ChatMessage("multiplayer.disconnect.unverified_username"));
                            NmsLoginListener.LOGGER.error("Username '{}' tried to join with an invalid session",gameprofile.getName());
                        }
                    }catch (AuthenticationUnavailableException var3){
                        if (server.isEmbeddedServer()){
                            NmsLoginListener.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                            gameProfile = NmsLoginListener.this.a(gameprofile);
                            state = NmsLoginListener.EnumProtocolState.READY_TO_ACCEPT;
                        } else {
                            if (PaperConfig.authenticationServersDownKickMessage != null){
                                disconnect(new ChatComponentText(PaperConfig.authenticationServersDownKickMessage));
                            } else {
                                disconnect(new ChatMessage("multiplayer.disconnect.authservers_down"));
                            }

                            NmsLoginListener.LOGGER.error("Couldn't verify username because servers are unavailable");
                        }
                    }catch (Exception var4){
                        disconnect("Failed to verify username!");
                        server.server.getLogger().log(Level.WARNING,"Exception verifying " + gameprofile.getName(),var4);
                    }

                }

                @Nullable
                private InetAddress a() {
                    SocketAddress socketaddress = networkManager.getSocketAddress();
                    return server.U() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
                }
            });
        }
    }

    @Override
    public void a(PacketLoginInCustomPayload packetloginincustompayload) {
        if (PaperConfig.velocitySupport && packetloginincustompayload.getId() == this.velocityLoginMessageId){
            PacketDataSerializer buf = packetloginincustompayload.getBuf();
            if (buf == null){
                this.disconnect("This server requires you to connect with Velocity.");
            } else if (!VelocityProxy.checkIntegrity(buf)){
                this.disconnect("Unable to verify player details");
            } else {
                this.networkManager.setSpoofedRemoteAddress(new InetSocketAddress(VelocityProxy.readAddress(buf),((InetSocketAddress) this.networkManager.getSocketAddress()).getPort()));
                this.setGameProfile(VelocityProxy.createProfile(buf));
                authenticatorPool.execute(() -> {
                    try{
                        new LoginHandler().fireEvents();
                    }catch (Exception var2){
                        this.disconnect("Failed to verify username!");
                        this.server.server.getLogger().log(Level.WARNING,"Exception verifying " + this.gameProfile.getName(),var2);
                    }

                });
            }
        } else {
            this.disconnect((IChatBaseComponent) (new ChatMessage("multiplayer.disconnect.unexpected_query_response")));
        }
    }

    protected GameProfile a(GameProfile gameprofile) {
        UUID uuid = EntityHuman.getOfflineUUID(gameprofile.getName());
        return new GameProfile(uuid,gameprofile.getName());
    }

    public static enum EnumProtocolState {
        HELLO, //开始握手
        KEY, //交换Key
        AUTHENTICATING, //验证中
        NEGOTIATING,
        READY_TO_ACCEPT, //准备接受
        DELAY_ACCEPT, //等待接受
        ACCEPTED; //接受
    }

    public class LoginHandler {
        public LoginHandler() {
        }

        public void fireEvents() throws Exception {
            if (velocityLoginMessageId == -1 && PaperConfig.velocitySupport){
                disconnect("This server requires you to connect with Velocity.");
            } else {
                String playerName = gameProfile.getName();
                InetAddress address = ((InetSocketAddress) networkManager.getSocketAddress()).getAddress();
                UUID uniqueId = gameProfile.getId();
                final CraftServer server = NmsLoginListener.this.server.server;
                PlayerProfile profile = CraftPlayerProfile.asBukkitMirror(getGameProfile());
                AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName,address,uniqueId,profile);
                server.getPluginManager().callEvent(asyncEvent);
                profile = asyncEvent.getPlayerProfile();
                profile.complete(true);
                setGameProfile(CraftPlayerProfile.asAuthlib(profile));
                playerName = gameProfile.getName();
                uniqueId = gameProfile.getId();
                if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0){
                    final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName,address,uniqueId);
                    if (asyncEvent.getResult() != Result.ALLOWED){
                        event.disallow(asyncEvent.getResult(),asyncEvent.getKickMessage());
                    }

                    Waitable<Result> waitable = new Waitable<Result>() {
                        protected Result evaluate() {
                            server.getPluginManager().callEvent(event);
                            return event.getResult();
                        }
                    };
                    NmsLoginListener.this.server.processQueue.add(waitable);
                    if (waitable.get() != Result.ALLOWED){
                        disconnect(event.getKickMessage());
                        return;
                    }
                } else if (asyncEvent.getLoginResult() != org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result.ALLOWED){
                    disconnect(asyncEvent.getKickMessage());
                    return;
                }

                NmsLoginListener.LOGGER.info("UUID of player {} is {}",gameProfile.getName(),gameProfile.getId());
                state = NmsLoginListener.EnumProtocolState.READY_TO_ACCEPT;
            }
        }
    }
}
