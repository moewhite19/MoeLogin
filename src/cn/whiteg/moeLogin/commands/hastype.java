package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class hastype extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length > 0){
            final LoginType loginType = MoeLogin.plugin.getLoginType(args[0]);
            sender.sendMessage(" §f" + args[0] + "§b的登录类型为: §f" + loginType.getName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return null;
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("whiteg.test");
    }

    @Override
    public String getDescription() {
        return "输入域名来测试登录类型";
    }
}
