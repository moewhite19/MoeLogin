package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class online extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        Utils.setOlineModele(!Bukkit.getOnlineMode());
        sender.sendMessage("修改服务器登陆模式为" + (Bukkit.getOnlineMode() ? "§b在线" : "§1离线"));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("whiteg.test");
    }

    @Override
    public String getDescription() {
        return "开关服务器在线状态";
    }
}
