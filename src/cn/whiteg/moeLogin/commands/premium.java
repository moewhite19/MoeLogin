package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class premium extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1){
            if (sender instanceof ConsoleCommandSender){
                sender.sendMessage("控制台不能使用这个指令");
            } else {
                if (!sender.hasPermission("moelogin.premium.self")) return false;
                MoeLogin plugin = MoeLogin.plugin;
                DataCon dc = MMOCore.getPlayerData(sender);
                if (dc == null){
                    sender.sendMessage("找不到玩家");
                    return false;
                }
                boolean flag = !dc.getConfig().getBoolean(plugin.authPath,Setting.defaultAuthenticate);

                if (flag && !(sender instanceof Player)){
                    if (!dc.getConfig().getBoolean("Authenticate.Success",false)){
                        sender.sendMessage("没有完成正版验证无法使用这个指令");
                        return false;
                    }
                }

                dc.set(plugin.authPath,flag);
                sender.sendMessage("§b已为阁下设置为: " + (flag ? "§a正版登录" : "§c离线登录"));
                return true;
            }
        } else if (args.length == 2){
            if (!sender.hasPermission("moelogin.premium.other")) return false;
            MoeLogin plugin = MoeLogin.plugin;
            DataCon dc = MMOCore.getPlayerData(args[1]);
            if (dc == null){
                sender.sendMessage("找不到玩家");
                return false;
            }
            boolean flag = dc.getConfig().getBoolean(plugin.authPath,Setting.defaultAuthenticate);
            dc.set(plugin.authPath,!flag);
            sender.sendMessage("§b已将" + dc.getName() + "设置为: " + (flag ? "§c离线登录" : "§a正版登录"));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 2) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }
}
