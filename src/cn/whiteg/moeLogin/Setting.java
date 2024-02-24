package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;

public class Setting {
    public final static int VER = 6;
    public static boolean DEBUG;
    public static FileConfiguration config;
    public static boolean AUTO_LOGIN;
    public static List<String> LoginCommands;
    public static List<String> RegisteredCommands;
    public static String passPat = "Authenticate.password.md5";
    public static String sha1Pat = "Authenticate.password.sha1";
    public static String authPath = "Authenticate.premium";
    public static String yggdrasilTypeKey = "Authenticate.yggdrasil";
    public static String successKey = "Authenticate.Success";
    public static String uuidKey = "Authenticate.UUID";
    final public static String banPath = "Player.ban";
    public static String loginmsg = "§b聊天输入 “/” + 密码 来登录";
    public static String regmsg = "§b聊天输入 “/” + 密码来注册 如 §3/mengbai520";
    public static boolean VERIFICATION_REG;
    public static boolean DISALL_NEWPLAYER;
    public static String DISALL_MESSAGE;
    public static String noLoginIp = "";
    public static boolean authenticate;
    public static boolean defaultAuthenticate;
    public static List<Integer> viaVersion;
    public static Map<String, String> yggdrasilMap = new HashMap<>();
    public static boolean antiDeathHandle = false;
    public static boolean keepSkin = true;
    public static String defaultYggdrasil = null;
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
                if (config.isSet(k)) continue;
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
                final String l_premiu = "Player.premium";
                final String l_yggdrasil = "Player.yggdrasil";
                final String l_passPat = "Player.password";
                final String l_sha1Pat = "Player.password_sha1";
                while (iterator.hasNext()) {
                    final DataCon next = iterator.next();
                    if (next.isLoaded()){
                        final FileConfiguration pd = next.getConfig();
                        if (pd.contains(l_premiu)){
                            pd.set(authPath,pd.get(l_premiu));
                            pd.set(l_premiu,null);
                        }
                        if (pd.contains(l_yggdrasil)){
                            pd.set(yggdrasilTypeKey,pd.get(l_yggdrasil));
                            pd.set(l_yggdrasil,null);
                        }
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

            //遗留的多余配置
            if (!DISALL_NEWPLAYER && defaultAuthenticate && authenticate && cs.isSet("autoRegister"))
                DISALL_NEWPLAYER = !cs.getBoolean("autoRegister");

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
                }catch (Exception e){
                    e.printStackTrace();
                }
            } else {
                proxy = Proxy.NO_PROXY;
            }
            MoeLogin.mojangAPI.setProxy(proxy);
        }
    }
}
