package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.PlayerLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moeLogin.utils.PasswordUtils;
import cn.whiteg.moeLogin.utils.Utils;
import net.minecraft.network.Connection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.TabCompleteEvent;

import java.text.SimpleDateFormat;
import java.util.*;

import static cn.whiteg.moeLogin.LoginManage.*;

public class LoginListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        //在这里弄个自动保存吧x
        final AliasManage aliasManage = MoeLogin.plugin.getAliasManage();
        if (aliasManage != null) aliasManage.save();

        Player player = event.getPlayer();
        DataCon dc = MMOCore.getPlayerData(player);

        if (!dc.isSet(Setting.authPath)){ //如果先前没有设置过认证方式，则设置默认认证方式
            dc.set(Setting.authPath,Setting.defaultAuthenticate);
            Utils.sendCommandList(Setting.RegisteredCommands,player);
        }

        if (hasLogin(player)){
            MoeLogin.console.sendMessage(player.getName() + "§b已自动登录");
            Utils.sendCommandList(Setting.LoginCommands,player);
        }
    }

/* //这种好像没法自动登录
   @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (hasLogin(player)){
            MoeLogin.console.sendMessage(player.getName() + "§b已自动登录");
            DataCon dc = MMOCore.getPlayerData(player);
            dc.set("Player.login_time",System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isNoLogin(event.getPlayer())){
            Player player = event.getPlayer();
            InetSocketAddress addr = event.getPlayer().getAddress();
            if (addr != null && hasAddressLogin(player,player.getAddress().getAddress().getHostAddress())){
                PlayerLogin pl = noLogin.get(player.getUniqueId());
                if (pl != null){
                    event.getPlayer().sendMessage(" §b已为阁下自动登录");
                    pl.Logined();
                }
            }
        } else {
            event.getPlayer().sendMessage(" §b阁下已自动登录");
        }
    }
    */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED || Bukkit.getOnlineMode()) return;
        final String name = event.getName();
        if (!PasswordUtils.checkName(name)){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"§b阁下ID无效§f(仅允许字母数字和下划线,字符数量限制为3-16)");
            return;
        }

        final DataCon pd = MMOCore.getPlayerData(name);

        if (pd == null){
            if (Setting.DISALL_NEWPLAYER){
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,Setting.DISALL_MESSAGE);
            } else {
                DataCon dc = MMOCore.craftData(name);
                dc.onSet();
            }
        } else {
            String dname = pd.getString("Player.name");
            if (dname == null){
                dname = name;
                pd.setString("Player.name",dname);
            }
            if (!dname.equals(name)){
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,Setting.DISALL_MESSAGE);
                MoeLogin.plugin.getLogger().warning(" 玩家名称" + dname + "和数据名称不匹配" + name);
                return;
            }
            ConfigurationSection sc = pd.getConfig().getConfigurationSection(Setting.banPath);
            if (sc != null){
                long now = System.currentTimeMillis();
                long time = sc.getLong("time");
                if (time > now){
                    String msg = sc.getString("message");
                    String source = sc.getString("source");
                    SimpleDateFormat timeform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,"§b阁下已被§f" + source + "§b关入小黑屋至\n§f" + timeform.format(new Date(time)) + (msg == null ? "" : "\n§b原因: §f" + msg));
                } else
                    pd.set(Setting.banPath,null);
            }
        }

        try{
            if (Setting.authenticate && (MoeLogin.plugin.isPremium(name) || MoeLogin.plugin.getYggdrasil(name) != null)){
                final Map<Connection, AuthenticateListener.LoginSession> map = MoeLogin.plugin.getAuthenticateListener().getSessionMap();
                final Set<Map.Entry<Connection, AuthenticateListener.LoginSession>> entries = map.entrySet();
                hasSession:
                {
                    for (Map.Entry<Connection, AuthenticateListener.LoginSession> entry : entries) {
                        final AuthenticateListener.LoginSession session = entry.getValue();
                        if (session.getGameProfile().getName().equals(name)){
                            if (session.isPass()) break hasSession;
                            else break;
                        }
                    }
                    MoeLogin.logger.warning(name + "没有完成登录验证");
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage("没有完成登录验证");
                    if (pd != null) MMOCore.unLoad(pd.getUUID());
                }
            }
        }catch (Throwable e){
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,e.getClass().getName());
            if (pd != null) MMOCore.unLoad(pd.getUUID());
            e.printStackTrace();
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (isNoLogin(player)){
            UUID uuid = player.getUniqueId();
            PlayerLogin pl = noLogin.get(uuid);
            if (pl != null){
                pl.remove();
            }
            noLogin.remove(uuid);
        }

        //如果玩家不是正版登录，建不能在上次下线位置上线
        final DataCon dc = MMOCore.getPlayerData(player);
        if (!MoeLogin.plugin.isPremium(dc) && MoeLogin.plugin.getYggdrasil(dc) == null){
            player.teleport(MoeLogin.plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (Setting.antiDeathHandle && event.getPlayer().isDead()){
            event.setCancelled(true);
            MoeLogin.logger.warning("玩家" + event.getPlayer().getName() + "尝试在死亡时发送聊天" + event.getMessage());
            return;
        }
        if (!noLogin.isEmpty()){
            PlayerLogin lg = noLogin.get(event.getPlayer().getUniqueId());
            if (lg == null) return;
            lg.sendMsg();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendCommandLow(PlayerCommandPreprocessEvent event) {
        PlayerCommandPreprocess(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void sendCommand(PlayerCommandPreprocessEvent event) {
        PlayerCommandPreprocess(event);
    }

    private void PlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (Setting.antiDeathHandle && event.getPlayer().isDead()){
            event.setCancelled(true);
            MoeLogin.logger.warning("玩家" + event.getPlayer().getName() + "尝试在死亡时发送指令" + event.getMessage());
            return;
        }
        if (!noLogin.isEmpty()){
            PlayerLogin lg = noLogin.get(event.getPlayer().getUniqueId());
            if (lg == null) return;
            String msg = event.getMessage().substring(1);
            lg.SendCommand(msg);
            event.setMessage("/ml ?");
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void breakBlock(TabCompleteEvent event) {
        if (event.getSender() instanceof Player && isNoLogin((Player) event.getSender())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (isNoLogin(event.getDamager())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void target(EntityTargetLivingEntityEvent event) {
        if (isNoLogin(event.getTarget())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void canBuild(BlockCanBuildEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setBuildable(false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onOpenInv(InventoryOpenEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
            event.getPlayer().closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDead(PlayerDeathEvent event) {
        if (isNoLogin(event.getEntity())){
            event.setKeepInventory(true);
            event.setKeepLevel(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDead(PlayerDropItemEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInvClicl(InventoryClickEvent event) {
        if (noLogin.containsKey(event.getWhoClicked().getUniqueId())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void SwapHandItem(PlayerSwapHandItemsEvent event) {
        if (isNoLogin(event.getPlayer())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void SwapHandItem(InventoryClickEvent event) {
        if (isNoLogin(event.getWhoClicked())){
            event.setCancelled(true);
        }
    }
}
