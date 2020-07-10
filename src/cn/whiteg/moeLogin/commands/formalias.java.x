package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class formalias extends CommandInterface {
    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            sender.sendMessage("§b请在指令后面加上<玩家别名>来查询玩家ID");
        } else if (args.length >= 1){
            String alias;
            if (args.length > 1){
                var coped = new String[args.length];
                System.arraycopy(args,0,coped,0,coped.length);
                alias = String.join(" ",coped);
            } else {
                alias = args[0];
            }
            var player = MoeLogin.plugin.getAliasManage().getPlayer(alias);
            if (player != null){
                sender.sendMessage("§b别名 §f" + alias + " §b的玩家ID为 §f" + player);
            } else {
                sender.sendMessage("找不到玩家");
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
    public String getDescription() {
        return "查询别名";
    }
}
