package cn.whiteg.moeLogin.commands;

import cn.whiteg.moeLogin.LoginManage;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.PlayerLogin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class login extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (!sender.hasPermission("whiteg.test")){
            sender.sendMessage("§b权限不足");
            return true;
        }
        if(args.length == 2){
            Player player = Bukkit.getPlayer(args[1]);
            if(player != null){
                PlayerLogin pl = LoginManage.noLogin.get(player.getUniqueId());
                if(pl== null){
                    sender.sendMessage("对方已登录");
                }else {
                    pl.onLogined();
                    sender.sendMessage("已为" + player.getDisplayName() + "登录");
                }
            }else {
                sender.sendMessage("找不到玩家");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }
}
