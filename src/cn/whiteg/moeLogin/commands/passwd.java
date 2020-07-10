package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.LoginManage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class passwd extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (!(sender instanceof Player)){
            return true;
        }
        if (args.length == 2){
            Player player = (Player) sender;
            String old = args[0];
            String nw = args[1];
            final DataCon dc = MMOCore.getPlayerData(player);
            if (dc == null){
                return false;
            }
            if (LoginManage.hasPassword(dc,old)){
                sender.sendMessage(LoginManage.chanPassword(dc,nw) ? "§b密码修改成功" : "§b新密码格式有误(最短不能小于6位数");
            } else {
                sender.sendMessage("阁下旧密码填写不正确哦");
            }
            return true;
        }
        sender.sendMessage("输入指令§3/ml passwd + §b<旧密码> + <新密码> + <重复新密码> §f来修改密码哦");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        return PlayersList(args);
    }

    @Override
    public String getDescription() {
        return "修改密码:§7 <旧密码> <新密码>";
    }
}
