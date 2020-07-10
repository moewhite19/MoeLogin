package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.LoginManage;
import cn.whiteg.moeLogin.PlayerLogin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class login extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1){
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null){
                PlayerLogin pl = LoginManage.noLogin.get(player.getUniqueId());
                if (pl == null){
                    sender.sendMessage("对方已登录");
                } else {
                    pl.onLogined();
                    sender.sendMessage("已为" + player.getDisplayName() + "登录");
                }
            } else {
                sender.sendMessage("找不到玩家");
            }
        }
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
        return "将玩家设为登录状态: §7<玩家ID>";
    }
}
