package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.LoginManage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class logout extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            if (sender instanceof Player){
                if (!sender.hasPermission("moelogin.logout.self")) return false;
                if (LoginManage.logOut((Player) sender)) sender.sendMessage("已为阁下安全登出");
                else sender.sendMessage("登出失败");
                return true;
            }
        }
        if (args.length == 1){
            if (!sender.hasPermission("moelogin.logout.other")) return false;
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null){
                if (LoginManage.logOut(player)){
                    sender.sendMessage("以为" + player.getName() + "登出");
                } else {
                    sender.sendMessage("未知原因，没有为" + player.getName() + "登出");
                }
            } else {
                sender.sendMessage("找不到玩家");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }

    @Override
    public String getDescription() {
        return "登出";
    }
}
