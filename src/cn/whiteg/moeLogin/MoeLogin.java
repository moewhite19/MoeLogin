package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandManage;
import cn.whiteg.mmocore.common.PluginBase;
import cn.whiteg.moeInfo.Settin;
import cn.whiteg.moeLogin.Filter.ConsoleFilter;
import cn.whiteg.moeLogin.hook.RealUUID;
import cn.whiteg.moeLogin.hook.WhoisAllowLoginTypeMsgProvider;
import cn.whiteg.moeLogin.hook.WhoisBannedProvider;
import cn.whiteg.moeLogin.hook.WhoisLoginTypeMsgProvider;
import cn.whiteg.moeLogin.listener.*;
import cn.whiteg.moeLogin.utils.MojangAPI;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import cn.whiteg.moepacketapi.utils.EntityNetUtils;
import net.minecraft.network.Connection;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
    public MojangPlayerManage premiumPlayerManage;
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
        //验证系统
        if (Setting.authenticate){
            authenticateListener = new AuthenticateListener();
            regListener(authenticateListener);
            premiumPlayerManage = new MojangPlayerManage(new File(plugin.getDataFolder(),"premium.map"));
            regListener(premiumPlayerManage);
            premiumPlayerManage.load();
        }
        ConsoleFilter.setupConsoleFilter();
        //注册whois指令的登录方式
        if (Bukkit.getPluginManager().getPlugin("MoeInfo") != null){
            Bukkit.getScheduler().runTask(this,() -> {
                new WhoisLoginTypeMsgProvider(this).register();
                new WhoisAllowLoginTypeMsgProvider(this).register();
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

    public LoginType getLoginType(String hostname) {
        for (LoginType loginType : Setting.yggdrasilList) {
            if (loginType.getPattern().matcher(hostname).matches()){
                return loginType;
            }
        }
        return LoginType.OFFLINE;
    }

    public LoginType getLoginType(Connection con) {
        final String hostname = con.hostname;
        return getLoginType(hostname);
    }

    public LoginType getLoginType(Player player) {
        final net.minecraft.world.entity.player.Player nmsPlayer = EntityNetUtils.getNmsPlayer(player);
        return getLoginType(EntityNetUtils.getPlayerConnection(nmsPlayer).connection);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean canLogin(DataCon dc,LoginType loginType) {
        final ConfigurationSection allows = dc.getSection(Setting.allowLoginKey);
        final String name = loginType.getName().replace('.','_');
        if (allows != null){
            if (allows.getBoolean(name,false)){
                return true;
            }
        }
        //如果有使用密码注册过 允许离线登录。 除非强制设置
        if (!loginType.isOnline() && dc.contarins(Setting.passPat)){
            return true;
        }
        //在所有登录类型都没启用的情况下，默认允许正版
        if (loginType.isMojang()){
            return true;
        }
        //可能需要其他操作,预留
        return false;
    }

    //设置可用的登录类型，返回之前的值
    public boolean allowLogin(DataCon dc,LoginType loginType,Boolean allow) {
        return allowLogin(dc,loginType.getName(),allow);
    }

    //设置可用的登录类型，返回之前的值
    //# allow可以为null ,代表恢复成未设置状态。 true或者false为强制设置
    public boolean allowLogin(DataCon dc,String loginType,Boolean allow) {
        final String replaced = loginType.replace('.','_');
        ConfigurationSection section = dc.getSection(Setting.allowLoginKey);
        if (section == null){
            section = dc.createSection(Setting.allowLoginKey);
        }
        boolean old = section.getBoolean(replaced,false);
        section.set(replaced,allow);
        return old;
    } //设置可用的登录类型，返回之前的值

    public void onReload() {
        logger.info("--开始重载--");
        reload();
        logger.info("--重载完成--");
    }

//    public boolean isPremium(String name) {
//        return isPremium(MMOCore.getPlayerData(name));
//    }

//    public boolean hostIsPremium(Connection con) {
//
//    }

//    public boolean isPremium(DataCon dc) {
//        return dc == null ? Setting.defaultAuthenticate : dc.getConfig().getBoolean(Setting.authPath,Setting.defaultAuthenticate);
//    }


    public AuthenticateListener getAuthenticateListener() {
        return authenticateListener;
    }

    public AliasManage getAliasManage() {
        return aliasManage;
    }

    public MojangPlayerManage getMojangPlayerManage() {
        return premiumPlayerManage;
    }
}
