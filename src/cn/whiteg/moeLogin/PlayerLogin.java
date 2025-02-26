package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.moeLogin.event.PlayerLoginEvent;
import cn.whiteg.moeLogin.event.PlayerRegisteredEvent;
import cn.whiteg.moeLogin.utils.PasswordUtils;
import cn.whiteg.moeLogin.utils.Utils;
import cn.whiteg.moeLogin.utils.Verification;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static cn.whiteg.moeLogin.Setting.*;

public class PlayerLogin {
    private final static String loginfallmsg = "阁下密码输入有误";
    private Player player;
    private Location loc;
    private BukkitTask task;
    private DataCon dc;
    private boolean isReg;
    private int mtime = 240;
    private String regStr;
    private Verification vf;
//    private FallingBlock hatEntity;


    public PlayerLogin(Player player) {
        this.player = player;
        dc = MMOCore.getPlayerData(player);
        isReg = dc.contarins(passPat);
        final String msg = isReg ? loginmsg : regmsg;
        task = Bukkit.getScheduler().runTaskTimer(MoeLogin.plugin,() -> {
            if (!player.isOnline()){
                remove();
                MoeLogin.logger.info("玩家没有登录");
                return;
            }
            if (loc == null) loc = player.getLocation();
            Location pl = player.getLocation();
            if (pl.getWorld() != loc.getWorld())
                loc = pl;
            if (Utils.HorizontalDistance(pl,loc) > 0.5){
                player.teleport(loc);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(msg));
            }
            mtime--;
            if (mtime < 1){
                player.kickPlayer("§b阁下登录超时啦");
                remove();
            }
        },20,20);
        Bukkit.getScheduler().runTask(MoeLogin.plugin,() -> {
            if (player.isOnline()) player.sendMessage(msg);
        });
    }

    public void remove() {
        task.cancel();
        LoginManage.noLogin.remove(player.getUniqueId());
    }

    public void hasPasswd(String passwd) {
        if (passwd.length() < 6){
            player.sendMessage(loginfallmsg);
        } else {
            if (LoginManage.hasPassword(dc,passwd)){
                onLogined();
            } else {
                player.sendMessage("§b密码错误");
            }
        }
    }

    public void onLogined() {
        remove();
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(player,PlayerLoginEvent.LoginType.OFFLINE);
        Bukkit.getPluginManager().callEvent(loginEvent);
        dc.set("Player.login_time",System.currentTimeMillis());
        if (AUTO_LOGIN) dc.set("Player.latest_login_ip",player.getAddress().getHostString());
        MoeLogin.logger.info(player.getName() + "已登录");
        Utils.sendCommandList(Setting.LoginCommands,player);
        player.sendMessage(" §b欢迎回家~");
        dc.save();
    }

    //尝试注册
    public boolean register(String password) {
        if (isReg){
            player.sendMessage("阁下已经注册过啦");
            return false;
        }
        if (password.length() < 6){
            player.sendMessage("密码太短啦");
            return false;
        }
        if (password.contains(" ")){
            player.sendMessage("密码里不能含有空格哦");
            return false;
        }
        if (regStr == null){
            regStr = password;
//            player.sendMessage("§b请阁下再输入一遍密码来确认注册");
            player.sendMessage("§b§l听不见,根本听不见,那么小声还想注册玩服务器? 重来!");
            return false;
        }
        if (!regStr.equals(password)){
            regStr = null;
            player.sendMessage("§3两次输入的密码不一致");
            return false;
        }
        player.sendMessage("§b§l好 很有精神");
        return true;
    }

    public void registered() {
        if (regStr == null){
            MoeLogin.logger.warning("玩家" + player.getName() + "注册失败");
            return;
        }
        PlayerRegisteredEvent registeredEvent = new PlayerRegisteredEvent(player);
        Bukkit.getPluginManager().callEvent(registeredEvent);
        if (registeredEvent.isCancelled()) return;
        dc.set(passPat,PasswordUtils.toMD5(regStr));
        dc.set(sha1Pat,PasswordUtils.toSha1(regStr));
        dc.save();
        onLogined();
    }

    public void sendMsg() {
        player.sendMessage(isReg ? loginmsg : regmsg);
    }

    public void SendCommand(String msg) {
        if (isReg) hasPasswd(msg);
        else {
            if (vf != null){
                if (vf.has(msg)){
                    registered();
                } else {
                    vf = null;
                    regStr = null;
                    player.sendMessage("§b验证码错误,请重新输入密码");
                }
            } else if (register(msg)){
                if (VERIFICATION_REG){
                    player.sendMessage("§b请根据提示点击相应字体完成最后注册");
                    vf = new Verification(player);
                    return;
                }
                registered();
            }
        }
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isReg() {
        return isReg;
    }

}
