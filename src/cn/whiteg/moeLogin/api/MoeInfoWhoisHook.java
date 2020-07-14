package cn.whiteg.moeLogin.api;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.moeInfo.api.MessagerAbs;
import cn.whiteg.moeLogin.MoeLogin;
import org.bukkit.command.CommandSender;

public class MoeInfoWhoisHook extends MessagerAbs {
    @Override
    public String getMsg(CommandSender player,DataCon dataCon) {
        if (MoeLogin.plugin.isPremium(dataCon)) return "§b登录方式: 正版";
        String ygg = MoeLogin.plugin.getYggdrasil(dataCon);
        if (ygg != null) return "§b登录方式: " + ygg;
        return null;
    }
}
