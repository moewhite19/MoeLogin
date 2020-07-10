package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.mmocore.util.CommonUtils;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ban extends HasCommandInterface {

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length >= 2){
            DataCon dc = MMOCore.getPlayerData(args[0]);
            if (dc == null){
                sender.sendMessage("找不到玩家");
                return false;
            }
            long time = CommonUtils.getTime(args[1]);
            if (time == 0){
                sender.sendMessage("无效时间");
                return false;
            }
            String msg;
            if (args.length > 2){
                msg = args[2];
            } else {
                msg = null;
            }
            String ts = CommonUtils.tanMintoh(time);
            time = time + System.currentTimeMillis();
            ConfigurationSection sc = dc.getConfig().createSection(Setting.banPath);
            sc.set("time",time);
            if (msg != null) sc.set("message",msg);
            String source = sender.getName();
            sc.set("source",source);
            Player p = dc.getPlayer();
            if (p != null){
                SimpleDateFormat timeform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                p.kickPlayer("§b阁下已被§f" + source + "§b关入小黑屋至§f" + timeform.format(new Date(time)) + (msg == null ? "" : "§b原因: §f" + msg));
            }
            sender.sendMessage(dc.getName() + "§b已被关入小黑屋§f" + ts + (msg == null ? "" : "§b原因: §f" + msg));
            dc.onSet(); //放置未保存
            return true;
        }
        sender.sendMessage("使用指令/ml ban <玩家> <时间> <选填:原因>");
        return false;
    }

    @Override
    public List<String> complete(CommandSender commandSender,Command command,String s,String[] strings) {
        return getMatches(strings,MMOCore.getLatelyPlayerList());
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return sender.hasPermission("mmo.ban");
    }

    @Override
    public String getDescription() {
        return "封禁: §f<玩家ID> <时间> §7示例: /ban MoeWhite 1d(一天)";
    }
}
