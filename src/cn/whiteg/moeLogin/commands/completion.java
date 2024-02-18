package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class completion extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        boolean flag = !Setting.defaultAuthenticate;
        final Iterator<DataCon> it = MMOCore.iteratorPlayerData();
        while (it.hasNext()) {
            final DataCon dc = it.next();
            if (dc.isLoaded()){
                if (!dc.isSet(Setting.authPath) && !dc.isSet(Setting.yggdrasilTypeKey)){
                    dc.set(Setting.authPath,flag);
                    dc.save();
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return null;
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("whiteg.test");
    }

    @Override
    public String getDescription() {
        return "开关自动注册后，开关前如果有离线登录切未切换过登录方式的玩家也会收到影响变成正版登录。可以用这个指令修复";
    }
}
