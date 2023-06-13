package cn.whiteg.moeLogin.hook;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.WhoisMessageProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RealUUID extends WhoisMessageProvider {
    public RealUUID(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        final String str = dataCon.getString("Authenticate.UUID");
        return str == null ? null : ("§b在线UUID: §f" + str);
    }
}
