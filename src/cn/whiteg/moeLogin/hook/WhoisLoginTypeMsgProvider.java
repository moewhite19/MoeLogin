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
        if (dataCon.getPlayer() != null){
            return "§b登录方式:§f " + MoeLogin.plugin.getLoginType(dataCon.getPlayer()).getName();
        }
        return null;
    }
}
