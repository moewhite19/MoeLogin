package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class alias extends CommandInterface {
    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            if (sender instanceof ConsoleCommandSender){
                sender.sendMessage("控制台不能使用这个指令");
            } else {
                var alias = MoeLogin.plugin.getAliasManage().getAlias(sender.getName());
                if (alias == null){
                    sender.sendMessage(" §b阁下当前没有设置别名");
                } else {
                    sender.sendMessage(" §b阁下当前登录别名为: §f" + alias);
                }
                return true;
            }
        } else if (args.length >= 1){
            var dc = MMOCore.getPlayerData(sender);
            if (dc != null && dc.isLoaded()){
                String alias;
                if (args.length > 1){
                    var coped = new String[args.length];
                    System.arraycopy(args,0,coped,0,coped.length);
                    alias = String.join(" ",coped);
                } else {
                    alias = args[0];
                }
                if (alias == null || alias.isEmpty() || alias.equals(dc.getName()) || alias.contains(":") || MMOCore.hasPlayerData(alias)){
                    sender.sendMessage("无效别名");
                    return false;
                }
                MoeLogin.plugin.getAliasManage().binding(dc.getName(),alias);
                sender.sendMessage("已设置别名为: " + alias);
                return true;

            } else {
                sender.sendMessage("无法获取到PlayerData");
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }
}
