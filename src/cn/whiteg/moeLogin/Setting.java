package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import cn.whiteg.moeLogin.utils.logintype.MojangLogin;
import cn.whiteg.moeLogin.utils.logintype.OfflineLogin;
import cn.whiteg.moeLogin.utils.logintype.YggdrasilLogin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.regex.Pattern;

public class Setting {
    public final static int VER = 8;
    public static boolean DEBUG;
    public static FileConfiguration config;
    public static boolean AUTO_LOGIN;
    public static List<String> LoginCommands;
    public static List<String> RegisteredCommands;
    public static String passPat = "Authenticate.password.md5";
    public static String sha1Pat = "Authenticate.password.sha1";
    public static String successKey = "Authenticate.Success";
    //允许第三方登录的节点
    public static String allowLoginKey = "Authenticate.AllowLogin";
    public static String uuidKey = "Authenticate.UUID";
    final public static String banPath = "Player.ban";
    public static String loginmsg = "§b聊天输入 “/” + 密码 来登录";
    public static String regmsg = "§b聊天输入 “/” + 密码来注册 如 §3/mengbai520";
    public static boolean VERIFICATION_REG;
    public static boolean DISALL_NEWPLAYER;
    public static String DISALL_MESSAGE;
    public static String noLoginIp = "";
    public static boolean authenticate;
    public static List<Integer> viaVersion;
    public static List<LoginType> yggdrasilList = new ArrayList<>();
    static Map<String, LoginType> loginTypeMap = new HashMap<>();
    public static boolean antiDeathHandle = false;
    public static boolean keepSkin = true;
    public static Proxy proxy = Proxy.NO_PROXY;
    //用于验证会话的字符串，当前还没见到使用
    public static String serverId = "";

    public static void reload() {
        MoeLogin plugin = MoeLogin.plugin;
        File file = new File(plugin.getDataFolder(),"config.yml");
        config = YamlConfiguration.loadConfiguration(file);
        final int loadVer = config.getInt("ver");
        if (loadVer != VER){
            plugin.getLogger().info("更新配置文件");
            plugin.saveResource("config.yml",true);
            config.set("ver",VER);
            final FileConfiguration newcon = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = newcon.getKeys(true);
            for (String k : keys) {
                if (config.contains(k)) continue;
                config.set(k,newcon.get(k));
                MMOCore.logger.info("在配置文件新增值: " + k);
            }
            try{
                config.save(file);
            }catch (IOException e){
                e.printStackTrace();
            }

            if (loadVer <= 5){
                //todo 重命名储存配置名
                final Iterator<DataCon> iterator = MMOCore.iteratorPlayerData();
                final String l_passPat = "Player.password";
                final String l_sha1Pat = "Player.password_sha1";
                while (iterator.hasNext()) {
                    final DataCon next = iterator.next();
                    if (next.isLoaded()){
                        final FileConfiguration pd = next.getConfig();
                        if (pd.contains(l_passPat)){
                            pd.set(passPat,pd.get(l_passPat));
                            pd.set(l_passPat,null);
                            next.onSet();
                        }
                        if (pd.contains(l_sha1Pat)){
                            pd.set(sha1Pat,pd.get(l_sha1Pat));
                            pd.set(l_sha1Pat,null);
                        }
                        next.save();
                    }
                }
            }
            if (loadVer <= 7){
                final Iterator<DataCon> iterator = MMOCore.iteratorPlayerData();
                final String yggdrasilType = "Authenticate.yggdrasil";
                final String onlie = "Authenticate.premium";
                while (iterator.hasNext()) {
                    final DataCon next = iterator.next();
                    if (next.isLoaded()){
                        final FileConfiguration pd = next.getConfig();
                        if (pd.getBoolean(onlie,false)){
                            plugin.allowLogin(next,LoginType.ONLINE,true);
                            plugin.getLogger().info(" 为玩家" + next.getName() + "添加登录方式: Online");
                            pd.set(onlie,null);
                        }
                        if (pd.contains(sha1Pat)){
                            plugin.allowLogin(next,LoginType.OFFLINE,true);
                            plugin.getLogger().info(" 为玩家" + next.getName() + "添加登录方式: Offline");
                        }
                        String yggdrasil = pd.getString(yggdrasilType);
                        if (yggdrasil != null){
                            if (yggdrasil.equals("Blessing")){
                                plugin.allowLogin(next,"skin.prinzeugen.net",true);
                                plugin.getLogger().info(" 为玩家" + next.getName() + "添加登录方式: Blessing");
                            }
                            if (yggdrasil.equals("LittleSkin")){
                                plugin.allowLogin(next,"mcskin.littleservice.cn",true);
                                plugin.getLogger().info(" 为玩家" + next.getName() + "添加登录方式: LittleSkin");
                            }
                            pd.set(yggdrasilType,null);
                        }
                        next.save();
                    }
                }
            }


        }
        AUTO_LOGIN = config.getBoolean("AutoLogin",false);
        DEBUG = config.getBoolean("debug");
        LoginCommands = config.getStringList("LoginCommands");
        RegisteredCommands = config.getStringList("RegisteredCommands");
        noLoginIp = config.getString("NoLoginIp",noLoginIp);
        VERIFICATION_REG = config.getBoolean("RegisterVerification",false);
        DISALL_NEWPLAYER = config.getBoolean("NewPlayerJoin.DisallowPlayer");
        DISALL_MESSAGE = config.getString("NewPlayerJoin.DisallowMessage");
        antiDeathHandle = config.getBoolean("AntiDeathHandle",false);

        viaVersion = config.getIntegerList("ViaVersion");

        ConfigurationSection cs = config.getConfigurationSection("Authenticate");
        if (cs != null){
            authenticate = cs.getBoolean("enable");
            keepSkin = cs.getBoolean("keepSkin");

            //noinspection unchecked
            final List<LinkedHashMap<String, Object>> yggdrasilList = (List<LinkedHashMap<String, Object>>) cs.getList("YggdrasilList");
            if (yggdrasilList != null){
                for (LinkedHashMap<String, Object> section : yggdrasilList) {
                    final String action = section.get("action").toString();
                    final String pattern = section.get("pattern").toString();
                    final Boolean defaultAllow = (Boolean) section.getOrDefault("default-allow",false);
                    if (action == null || pattern == null) continue;
                    final Pattern compile = Pattern.compile(pattern);
                    if (action.equals("MOJANG")){
                        final MojangLogin e = new MojangLogin(compile,defaultAllow);
                        loginTypeMap.put(e.getName(),e);
                        Setting.yggdrasilList.add(e);
                        LoginType.ONLINE = e;
                    } else if (action.equals("OFFLINE")){
                        final OfflineLogin e = new OfflineLogin(compile,defaultAllow);
                        Setting.yggdrasilList.add(e);
                        loginTypeMap.put(e.getName(),e);
                        LoginType.OFFLINE = e;
                    } else if (action.startsWith("http")){
                        final YggdrasilLogin e = new YggdrasilLogin(action,compile,defaultAllow);
                        Setting.yggdrasilList.add(e);
                        loginTypeMap.put(e.getName(),e);
                    } else {
                        plugin.getLogger().warning("YggdrasilList配置错误: " + pattern + " = " + action);
                        continue;
                    }

                    if (DEBUG){
                        plugin.getLogger().info("YggdrasilList配置: " + pattern + " = " + action);
                    }
                }
            }
        }

        proxy:
        {
            cs = config.getConfigurationSection("HttpProxy");
            if (cs != null && cs.getBoolean("Enable")){
                try{
                    proxy = new Proxy(Proxy.Type.valueOf(cs.getString("Type","SOCKS")),new InetSocketAddress(cs.getString("IP","127.0.0.1"),cs.getInt("Port")));
                    plugin.logger.info("使用代理: " + proxy);
                }catch (Exception e){
                    e.printStackTrace();
                    break proxy;
                }
            } else {
                proxy = Proxy.NO_PROXY;
            }
            MoeLogin.mojangAPI.setProxy(proxy);
        }
    }

    public static Map<String, LoginType> getLoginTypeMap() {
        return loginTypeMap;
    }
}
