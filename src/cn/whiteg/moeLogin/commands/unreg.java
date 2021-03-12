package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class unreg extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 2){
            DataCon dc = MMOCore.getPlayerData(args[1]);
            if (dc != null){
                dc.set("Player.password",null);
                sender.sendMessage("§b已重置玩家§f " + dc.getName() + " §b的密码");
                dc.save();
            } else {
                sender.sendMessage("找不到玩家");
            }
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String label,String[] args) {
        return getMatches(MMOCore.getLatelyPlayerList(),args);
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("whiteg.test");
    }
}
