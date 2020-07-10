package cn.whiteg.moeLogin.hook;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.WhoisMessageProvider;
import cn.whiteg.moeLogin.MoeLogin;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WhoisLoginTypeMsgProvider extends WhoisMessageProvider {
    public WhoisLoginTypeMsgProvider(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        String ygg = MoeLogin.plugin.isPremium(dataCon) ? "正版" : MoeLogin.plugin.getYggdrasil(dataCon);
        if (ygg != null) return "§b登录方式:§f " +  ygg;
        return null;
    }
}
