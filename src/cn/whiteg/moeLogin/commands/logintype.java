package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.CommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moeLogin.utils.logintype.LoginType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public class logintype extends CommandInterface {

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length >= 3){
            String opt = args[0];
            String type = args[1];
            String player = args[2];
            DataCon dc = MMOCore.getPlayerData(player);
            if (dc == null){
                sender.sendMessage(" §c未找到玩家:§f " + player);
                return false;
            }
            if (opt.equals("add")){
                if (Setting.getLoginTypeMap().containsKey(type)){
                    if (!MoeLogin.plugin.allowLogin(dc,type,true)){
                        sender.sendMessage(" §b允许登录:§f " + type);
                    } else {
                        sender.sendMessage(" §c登录方式未禁用:§f " + type);
                    }
                }
            } else if (opt.equals("del")){
                if (MoeLogin.plugin.allowLogin(dc,type,null)){
                    sender.sendMessage(" §b禁用登录:§f " + type);
                } else {
                    sender.sendMessage(" §c登录方式未启用:§f " + type);
                }
            }
            return true;
        }


        //仅限玩家的指令
        if (!(sender instanceof Player player)) return true;
        DataCon dc = MMOCore.getPlayerData(player);
        if (args.length == 0){
            StringBuilder sb = new StringBuilder(" §b当前阁下已启用登录方式:§f ");
            for (LoginType loginType : Setting.getLoginTypeMap().values()) {
                if (MoeLogin.plugin.canLogin(dc,loginType)){
                    sb.append("§a").append(loginType.getName()).append(" §7,");
                }
            }
            player.sendMessage(sb.substring(0,sb.length() - 2));
        } else if (args.length == 2){
            String opt = args[0];
            String type = args[1];

            if (opt.equals("add")){
                if (Setting.getLoginTypeMap().containsKey(type)){
                    if (!MoeLogin.plugin.allowLogin(dc,type,true)){
                        player.sendMessage(" §b允许登录方式:§f " + type);
                    } else {
                        player.sendMessage(" §c登录方式未禁用:§f " + type);
                    }
                } else {
                    player.sendMessage(" §c未找到登录方式:§f " + type);
                }
            } else if (opt.equals("del")){
                if (MoeLogin.plugin.allowLogin(dc,type,null)){
                    player.sendMessage(" §b禁用登录方式:§f " + type);
                } else {
                    player.sendMessage(" §c登录方式未启用:§f " + type);
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1){
            return getMatches(List.of("add","del"),args);
        } else if (args.length == 2){
            final List<LoginType> yggdrasilMap = Setting.yggdrasilList;
            List<String> list = new ArrayList<>(yggdrasilMap.size());
            for (LoginType loginType : yggdrasilMap) {
                list.add(loginType.getName());
            }
            return getMatches(list,args);
        }
        return null;
    }

    @Override
    public String getDescription() {
        return "管理登录方式";
    }
}
