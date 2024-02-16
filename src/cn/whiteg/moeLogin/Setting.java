package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.MMOCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Setting {
    public final static int VER = 5;
    final public static String banPath = "Player.ban";
    public static boolean DEBUG;
    public static FileConfiguration config;
    public static boolean AUTO_LOGIN;
    public static List<String> LoginCommands;
    public static List<String> RegisteredCommands;
    public static String passPat = "Player.password";
    public static String sha1Pat = "Player.password_sha1";
    public static String loginmsg = "§b聊天输入 “/” + 密码 来登录";
    public static String regmsg = "§b聊天输入 “/” + 密码来注册 如 §3/mengbai520";
    public static boolean VERIFICATION_REG;
    public static boolean DISALL_NEWPLAYER;
    public static String DISALL_MESSAGE;
    public static String noLoginIp = "";
    public static boolean authenticate;
    public static boolean autoRegister;
    public static boolean defaultAuthenticate;
    public static List<Integer> viaVersion;
    public static Map<String, String> yggdrasilMap = new HashMap<>();
    public static boolean antiDeathHandle = false;
    public static boolean keepSkin = true;
    public static String defaultYggdrasil = null;
    public static Proxy proxy = null;

    public static void reload() {
        MoeLogin plugin = MoeLogin.plugin;
        File file = new File(plugin.getDataFolder(),"config.yml");
        config = YamlConfiguration.loadConfiguration(file);
        if (config.getInt("ver") != VER){
            plugin.getLogger().info("更新配置文件");
            plugin.saveResource("config.yml",true);
            config.set("ver",VER);
            final FileConfiguration newcon = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = newcon.getKeys(true);
            for (String k : keys) {
                if (config.isSet(k)) continue;
                config.set(k,newcon.get(k));
                MMOCore.logger.info("在配置文件新增值: " + k);
            }
            try{
                config.save(file);
            }catch (IOException e){
                e.printStackTrace();
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

        ConfigurationSection cs = config.getConfigurationSection("Authenticate");
        if (cs != null){
            authenticate = cs.getBoolean("enable");
            keepSkin = cs.getBoolean("keepSkin");
            defaultAuthenticate = cs.getBoolean("defaultAuthenticate");
            defaultYggdrasil = cs.getString("defaultYggdrasil");
            autoRegister = cs.getBoolean("autoRegister");
            cs = cs.getConfigurationSection("Yggdrasil");
            if (cs != null){
                for (String key : cs.getKeys(false)) {
                    yggdrasilMap.put(key,cs.getString(key));
                }
            }
        }
        viaVersion = config.getIntegerList("ViaVersion");

        proxy:
        {
            cs = config.getConfigurationSection("HttpProxy");
            if (cs != null && cs.getBoolean("Enable")){
                try{
                    proxy = new Proxy(Proxy.Type.valueOf(cs.getString("Type","SOCKS")),new InetSocketAddress(cs.getString("IP","127.0.0.1"),cs.getInt("Port")));
                    plugin.logger.info("当前已启用代理: " + proxy.address().toString());
                    break proxy;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            proxy = Proxy.NO_PROXY;
            MoeLogin.mojangAPI.setProxy(proxy);
        }

    }
}
