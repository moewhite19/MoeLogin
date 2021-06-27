package cn.whiteg.moeLogin.hook;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.WhoisMessageProvider;
import cn.whiteg.moeLogin.MoeLogin;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WhoisAliasMsgProvider extends WhoisMessageProvider {
    public WhoisAliasMsgProvider(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        if (MoeLogin.plugin.isPremium(dataCon)) return "§b登录方式: 正版";
        String alias = MoeLogin.plugin.getAliasManage().getAlias(dataCon.getName());
        if (alias != null) return "§b别名: " + alias;
        return null;
    }
}
