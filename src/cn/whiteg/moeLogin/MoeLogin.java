package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.PluginBase;
import cn.whiteg.moeLogin.Filter.ConsoleFilter;
import cn.whiteg.moeLogin.listener.AuthenticateListener;
import cn.whiteg.moeLogin.listener.LoginListener;
import cn.whiteg.moeLogin.listener.ViaVersion;
import cn.whiteg.moeLogin.utils.MojangAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.Map;
import java.util.logging.Logger;

import static cn.whiteg.moeLogin.Setting.reload;


public class MoeLogin extends PluginBase {
    public static Logger logger;
    public static MoeLogin plugin;
    public static CommandSender console;
    public static MojangAPI mojangAPI = new MojangAPI();
    public final String authPath = "Player.premium";
    public final String yggdrasilTypeKey = "Player.yggdrasil";
    public CommandManage mainCommand;
    private AuthenticateListener authenticateListener;

    public MoeLogin() {
        plugin = this;
        console = Bukkit.getConsoleSender();
    }

    public static MojangAPI getMojangAPI() {
        return mojangAPI;
    }

    public void onLoad() {
        saveDefaultConfig();
        logger = getLogger();
        reload();
    }

    public void onEnable() {
        logger.info("开始加载插件");
        if (Setting.DEBUG) logger.info("§a调试模式已开启");
        mainCommand = new CommandManage();
        PluginCommand pc = getCommand("moelogin");
        pc.setExecutor(mainCommand);
        pc.setTabCompleter(mainCommand);
        regListener(new LoginListener());
        if (!Setting.viaVersion.isEmpty()){
            regListener(new ViaVersion());
        }
//        Plugin fastLogin = Bukkit.getPluginManager().getPlugin("FastLogin");
//        try{
//            if (fastLogin != null && fastLogin.isEnabled()){
//                ((FastLoginBukkit) fastLogin).getCore().setAuthPluginHook(new FastLoginHook());
//                logger.info("FastLogin Hooked");
//            }
//        }catch (Exception e){
//            logger.info("FastLogin Not Found");
//        }
        if (Setting.authenticate){
            authenticateListener = new AuthenticateListener();
            regListener(authenticateListener);
        }
        logger.info("全部加载完成");
        ConsoleFilter.setupConsoleFilter();
    }

    public void onDisable() {
        unregListener();
        //注销注册玩家加入服务器事件
        ConsoleFilter.unsetConsoleFilter();
        for (Map.Entry m : LoginManage.noLogin.entrySet()) {
            PlayerLogin p = (PlayerLogin) m.getValue();
            p.remove();
            if (p.getPlayer().isOnline()){
                p.getPlayer().kickPlayer("你没有登录");
            }
        }
        LoginManage.noLogin.clear();
        logger.info("插件已关闭");
    }

    public void onReload() {
        logger.info("--开始重载--");
        reload();
        logger.info("--重载完成--");
    }

    public boolean setPremium(String name,boolean var) {
        DataCon dc = MMOCore.getPlayerData(name);
        if (dc != null){
            dc.set(authPath,var);
            return true;
        }
        return false;
    }

    public boolean isPremium(String name) {
        DataCon dc = MMOCore.getPlayerData(name);
        if (dc != null){
            return dc.getConfig().getBoolean(authPath,Setting.defaultAuthenticate);
        } else {
            return Setting.autoRegister;
        }
    }

    public String getYggdrasil(String name) {
        DataCon dc = MMOCore.getPlayerData(name);
        if (dc == null) return null;
        return dc.getString(yggdrasilTypeKey);
    }

    public boolean setYggdrasil(String name, String url) {
        DataCon dc = MMOCore.getPlayerData(name);
        if (dc == null) return false;
        dc.set(yggdrasilTypeKey , url);
        return true;
    }

    public AuthenticateListener getAuthenticateListener() {
        return authenticateListener;
    }
}
