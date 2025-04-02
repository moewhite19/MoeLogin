package cn.whiteg.moeLogin.hook;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.WhoisMessageProvider;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class WhoisAllowLoginTypeMsgProvider extends WhoisMessageProvider {
    public WhoisAllowLoginTypeMsgProvider(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        StringBuilder sb = new StringBuilder("§b可用登录方式:§f ");
        final Map<String, LoginType> loginTypeMap = Setting.getLoginTypeMap();
        for (Map.Entry<String, LoginType> entry : loginTypeMap.entrySet()) {
            if (MoeLogin.plugin.canLogin(dataCon,entry.getValue())){
                sb.append("§f").append(entry.getKey()).append("§7,");
            }
        }
        return sb.substring(0,sb.length() - 3);
    }
}
