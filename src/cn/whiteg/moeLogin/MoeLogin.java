package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandManage;
import cn.whiteg.mmocore.common.PluginBase;
import cn.whiteg.moeLogin.Filter.ConsoleFilter;
import cn.whiteg.moeLogin.hook.RealUUID;
import cn.whiteg.moeLogin.hook.WhoisBannedProvider;
import cn.whiteg.moeLogin.hook.WhoisLoginTypeMsgProvider;
import cn.whiteg.moeLogin.listener.*;
import cn.whiteg.moeLogin.utils.MojangAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cn.whiteg.moeLogin.Setting.reload;


public class MoeLogin extends PluginBase {
    public static Logger logger;
    public static MoeLogin plugin;
    public static CommandSender console;
    public static MojangAPI mojangAPI = new MojangAPI();
    public PremiumPlayerManage premiumPlayerManage;
    public CommandManage mainCommand;
    AliasManage aliasManage;
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
        Setting.reload();
        if (Setting.DEBUG){
            logger.setLevel(Level.ALL);
        }
    }

    public void onEnable() {
        logger.info("开始加载插件");
//        logger.info("当前日志级别: " + logger.getLevel().getName());
        if (Setting.DEBUG) logger.info("§a调试模式已开启");
        mainCommand = new CommandManage(this);
        mainCommand.setExecutor();
        regListener(new LoginListener());
        //别名系统
        final AliasManage alias = new AliasManage(this);
        if (alias.isEnable()){
            aliasManage = alias;
            aliasManage.load();
            regListener(aliasManage);
        }
        //允许的旧协议
        if (!Setting.viaVersion.isEmpty()){
            regListener(new ViaVersion());
        }
        //验证系统
        if (Setting.authenticate){
            authenticateListener = new AuthenticateListener();
            regListener(authenticateListener);
            premiumPlayerManage = new PremiumPlayerManage(new File(plugin.getDataFolder(),"premium.map"));
            regListener(premiumPlayerManage);
            premiumPlayerManage.load();
        }
        ConsoleFilter.setupConsoleFilter();
        //注册whois指令的登录方式
        if (Bukkit.getPluginManager().getPlugin("MoeInfo") != null){
            Bukkit.getScheduler().runTask(this,() -> {
                new WhoisLoginTypeMsgProvider(this).register();
//                new WhoisAliasMsgProvider(this).register();
                new WhoisBannedProvider(this).register();
                new RealUUID(this).register();
            });
        }
        logger.info("全部加载完成");
    }

    public void onDisable() {
        unregListener();
//        aliasManage.save();
        //注销注册玩家加入服务器事件
        ConsoleFilter.shutdown();
        for (Map.Entry<UUID, PlayerLogin> m : LoginManage.noLogin.entrySet()) {
            PlayerLogin p = m.getValue();
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
        return setPremium(MMOCore.getPlayerData(name),var);
    }

    public boolean setPremium(DataCon dc,boolean var) {
        if (dc != null){
            dc.set(Setting.authPath,var);
            return true;
        }
        return false;
    }

    public boolean isPremium(String name) {
        return isPremium(MMOCore.getPlayerData(name));
    }

    public boolean isPremium(DataCon dc) {
        if (dc != null){
            return dc.getConfig().getBoolean(Setting.authPath,Setting.defaultAuthenticate);
        } else {
            return Setting.defaultAuthenticate;
        }
    }

    public String getYggdrasil(String name) {
        return getYggdrasil(MMOCore.getPlayerData(name));
    }

    public String getYggdrasil(DataCon dc) {
        if (dc == null) return null;
        return dc.getString(Setting.yggdrasilTypeKey,Setting.defaultYggdrasil);
    }

    public boolean setYggdrasil(String name,String url) {
        return setYggdrasil(MMOCore.getPlayerData(name),url);
    }

    public boolean setYggdrasil(DataCon dc,String url) {
        if (dc == null) return false;
        dc.set(Setting.yggdrasilTypeKey,url);
        return true;
    }

    public AuthenticateListener getAuthenticateListener() {
        return authenticateListener;
    }

    public AliasManage getAliasManage() {
        return aliasManage;
    }

    public PremiumPlayerManage getPremiumPlayerManage() {
        return premiumPlayerManage;
    }
}
