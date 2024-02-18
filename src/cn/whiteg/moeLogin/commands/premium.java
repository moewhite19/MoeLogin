package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class premium extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
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
                boolean flag;
                final String yggdrasil = MoeLogin.plugin.getYggdrasil(dc);
                if (yggdrasil != null){
                    //如果存在外置登录，关闭外置登录
                    flag = false;
                    MoeLogin.plugin.setYggdrasil(dc,null);
                } else {
                    //开关正版登录
                    flag = !dc.getConfig().getBoolean(Setting.authPath,Setting.defaultAuthenticate);
                    if (flag){
                        if (!sender.getClass().getSimpleName().startsWith("Craft")){ //如果是使用机器人之类的来执行指令就必须完成正版验证
                            if (!dc.getConfig().getBoolean("Authenticate.Success",false)){
                                sender.sendMessage("没有完成正版验证无法使用这个指令,请在游戏内开启正版登录完成正版验证");
                                return false;
                            }
                        } else if (sender instanceof Player player){
                            player.kickPlayer("§b退出使用正版登录");
                        }
                    }
                    dc.set(Setting.authPath,flag);
                }
                sender.sendMessage("§b已为阁下设置为: " + (flag ? "§a正版登录" : "§c离线登录"));
                return true;
            }
        } else if (args.length == 1){
            if (!sender.hasPermission("moelogin.premium.other")) return false;
            MoeLogin plugin = MoeLogin.plugin;
            DataCon dc = MMOCore.getPlayerData(args[0]);
            if (dc == null){
                sender.sendMessage("找不到玩家");
                return false;
            }
            boolean flag;
            final String yggdrasil = MoeLogin.plugin.getYggdrasil(dc);
            if (yggdrasil != null){
                //如果存在外置登录，关闭外置登录
                flag = false;
                MoeLogin.plugin.setYggdrasil(dc,null);
            } else {
                flag = dc.getConfig().getBoolean(Setting.authPath,Setting.defaultAuthenticate);
                dc.set(Setting.authPath,!flag);
            }
            sender.sendMessage("§b已将" + dc.getName() + "设置为: " + (flag ? "§c离线登录" : "§a正版登录"));
            return true;
        }
        return false;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return Setting.authenticate;
    }

    @Override
    public String getDescription() {
        return "将登录方式设置为正版验证登录(仅限正版玩家使用),第一次使用正版登录将会完成正版验证。同时也可以用来关闭外置登录";
    }
}
