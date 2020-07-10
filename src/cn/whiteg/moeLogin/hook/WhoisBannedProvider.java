package cn.whiteg.moeLogin.hook;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.WhoisMessageProvider;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;

public class WhoisBannedProvider extends WhoisMessageProvider {
    public WhoisBannedProvider(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        ConfigurationSection sc = dataCon.getConfig().getConfigurationSection(Setting.banPath);
        if (sc != null){
            long time = sc.getLong("time");
            if (time > System.currentTimeMillis()){
//                String msg = sc.getString("message");
//                String source = sc.getString("source");
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return "§b封禁时间:§f " + dateFormat.format(time);
            }
        }
        return null;
    }
}
