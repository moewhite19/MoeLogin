package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.listener.AliasManage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class delalias extends HasCommandInterface {
    final AliasManage aliasManage;

    public delalias(AliasManage aliasManage) {
        this.aliasManage = aliasManage;
    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            sender.sendMessage("§b请在指令后面加上<玩家ID>来删除玩家别名");
        } else if (args.length >= 1){
            var player = args[0];
            var alias = aliasManage.deleteFormPlayer(player);
            if (alias != null){
                sender.sendMessage("§b已删除玩家 §f" + player + " §b的别名 §f" + alias);
            } else {
                sender.sendMessage(player + " §b当前没有别名");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }

    @Override
    public boolean canUseCommand(CommandSender commandSender) {
        return commandSender.hasPermission("whiteg.test");
    }

    @Override
    public String getDescription() {
        return "删除别名";
    }
}
