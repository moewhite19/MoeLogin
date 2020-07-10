package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.listener.AliasManage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;
import java.util.regex.Pattern;

public class alias extends HasCommandInterface {
    Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,18}$");

    public alias() {

    }

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            if (sender instanceof ConsoleCommandSender){
                sender.sendMessage("控制台不能使用这个指令");
            } else {
                final AliasManage aliasManage = MoeLogin.plugin.getAliasManage();
                var alias = aliasManage.getAlias(sender.getName());
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
                final AliasManage aliasManage = MoeLogin.plugin.getAliasManage();
                String alias;
//                if (args.length > 1){
//                    var coped = new String[args.length];
//                    System.arraycopy(args,0,coped,0,coped.length);
//                    alias = String.join(" ",coped);
//                } else {
//                    alias = args[0];
//                }
                alias = args[0];
                if (alias == null || alias.isBlank() || !PATTERN.matcher(alias).matches() || alias.equals(dc.getName()) || alias.contains(":") || MMOCore.hasPlayerData(alias) || aliasManage.getPlayer(alias) != null){
                    sender.sendMessage("无效别名");
                    return false;
                }
                aliasManage.binding(dc.getName(),alias);
                sender.sendMessage("已设置别名为: " + alias);
                return true;

            } else {
                sender.sendMessage("无法获取到PlayerData");
            }
        }
        return false;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }

    @Override
    public String getDescription() {
        return "别名,可以使用另一个ID来登录。: §7<名称>";
    }

    @Override
    public boolean canUseCommand(CommandSender commandSender) {
        if (MoeLogin.plugin.getAliasManage() == null) return false;
        return super.canUseCommand(commandSender);
    }
}
