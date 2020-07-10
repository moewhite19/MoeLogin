package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class online extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (!sender.hasPermission("whiteg.test")) return false;
        Utils.setOlineModele(!Bukkit.getOnlineMode());
        sender.sendMessage("修改服务器登陆模式为" + (Bukkit.getOnlineMode() ? "§b在线" : "§1离线"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }
}
