package cn.whiteg.moeLogin.commands;

import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.common.HasCommandInterface;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class yggdrasil extends HasCommandInterface {
    WeakReference<List<String>> keys = new WeakReference<>(null);

    @Override
    public boolean executo(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            if (sender instanceof ConsoleCommandSender){
                sender.sendMessage("控制台不能使用这个指令");
            } else {
                if (!sender.hasPermission("moelogin.yggdrasil.self")) return false;
                if (MoeLogin.plugin.setYggdrasil(sender.getName(),null)){
                    sender.sendMessage(" §b已关闭外置登录");
                    return true;
                }
            }
        } else if (args.length == 1){
            if (sender instanceof ConsoleCommandSender){
                sender.sendMessage("控制台不能使用这个指令");
            } else {
                if (!sender.hasPermission("moelogin.yggdrasil.self")) return false;
                if (Setting.yggdrasilMap.containsKey(args[0])){
                    if (MoeLogin.plugin.setYggdrasil(sender.getName(),args[0])){
                        sender.sendMessage(" §b已设置外置登录服务器为" + args[0]);
                        return true;
                    }
                } else {
                    sender.sendMessage(" §b无效服务器, 当前可用的外置服务器有: " + getKeys());
                }
            }

        } else if (args.length == 2){
            if (!sender.hasPermission("moelogin.yggdrasil.other")) return false;
            if (Setting.yggdrasilMap.containsKey(args[0])){
                if (MoeLogin.plugin.setYggdrasil(args[1],args[0])) sender.sendMessage(" §b已设置玩家外置登录");
                return true;
            } else {
                if (MoeLogin.plugin.setYggdrasil(args[1],null))
                    sender.sendMessage(" §b找不到服务器，已为玩家关闭外置登录");
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> complete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 1){
            return getKeys();
        } else if (args.length == 2) return getMatches(args,MMOCore.getLatelyPlayerList());
        return null;
    }

    @Override
    public boolean canUseCommand(CommandSender sender) {
        return Setting.authenticate;
    }

    List<String> getKeys() {
        List<String> list = keys.get();
        if (list == null){
            list = new ArrayList<>(6);
            for (Map.Entry<String, String> entry : Setting.yggdrasilMap.entrySet()) {
                list.add(entry.getKey());
            }
            keys = new WeakReference<>(list);
        }
        return list;
    }

    @Override
    public String getDescription() {
        return "外置登录";
    }
}
