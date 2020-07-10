package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class mode extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (!(sender instanceof Player)){
            return false;
        }
        if (!sender.hasPermission("moelogin.mode")) return false;
        DataCon dc = MMOCore.getPlayerData(sender);
        if (dc == null) return false;
        String pat = "Player.online";
        boolean d = dc.getConfig().getBoolean(pat,false);
        dc.set(pat,!d);
        sender.sendMessage("修改登陆模式为" + (d ? "§b在线" : "§1离线"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }
}
