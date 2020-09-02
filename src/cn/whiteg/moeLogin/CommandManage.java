package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.common.CommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandManage extends CommandInterface {
    final public SubCommand subCommand = new SubCommand();
    public Map<String, CommandInterface> commandMap = new HashMap<>();
    public List<String> AllCmd;

    public CommandManage() {
        AllCmd = Arrays.asList("reload","unreg","login","passwd","logout","ban","mode","online","premium","yggdrasil");
        for (String s : AllCmd) {
            try{
                Class<?> c = Class.forName("cn.whiteg.moeLogin.commands." + s);
                regCommand(s,(CommandInterface) c.newInstance());
            }catch (ClassNotFoundException | InstantiationException | IllegalAccessException e){
                e.printStackTrace();
            }
        }
    }

    public static List<String> getMatches(String[] args,List<String> list) {
        return getMatches(args[args.length - 1],list);
    }

    public static List<String> getMatches(List<String> list,String[] args) {
        return getMatches(args[args.length - 1],list);
    }

    public static List<String> getMatches(String value,List<String> list) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String str = list.get(i).intern().toLowerCase();
            if (str.startsWith(value.toLowerCase())){
                result.add(list.get(i));
            }
        }
        return result;
    }

    public static List<String> getMatches(List<String> list,String value) {
        return getMatches(list,value);
    }

    public static List<String> PlayersList(String arg) {
        List<String> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());
        return getMatches(arg,players);
    }

    public static List<String> PlayersList(String[] arg) {
        return PlayersList(arg[arg.length - 1]);
    }

    @Override
    public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length == 0){
            sender.sendMessage("§2[§bMoeLogin§2]");
            return true;
        }
        if (commandMap.containsKey(args[0])){
            return commandMap.get(args[0]).onCommand(sender,cmd,label,args);
        } else {
            sender.sendMessage("无效指令");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,Command cmd,String label,String[] args) {
        if (args.length > 1){
            List ls = null;
            if (commandMap.containsKey(args[0])) ls = commandMap.get(args[0]).onTabComplete(sender,cmd,label,args);
            if (ls != null){
                return getMatches(args[args.length - 1],ls);
            }
        }
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].toLowerCase();
        }
        if (args.length == 1){
            return getMatches(args[0],AllCmd);
        }
        return null;
    }

    public void regCommand(String name,CommandInterface cmd) {
        commandMap.put(name,cmd);
        PluginCommand pc = MoeLogin.plugin.getCommand(name);
        if (pc != null){
            pc.setExecutor(subCommand);
            pc.setTabCompleter(subCommand);
        }
    }

    public class SubCommand extends CommandInterface {
        @Override
        public boolean onCommand(CommandSender commandSender,Command command,String s,String[] strings) {
            CommandInterface ci = commandMap.get(command.getName());
            if (ci == null) return false;
            String[] args = new String[strings.length + 1];
            args[0] = command.getName();
            System.arraycopy(strings,0,args,1,strings.length);
            ci.onCommand(commandSender,command,s,args);
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender commandSender,Command command,String s,String[] strings) {
            CommandInterface ci = commandMap.get(command.getName());
            if (ci == null) return null;
            String[] args = new String[strings.length + 1];
            args[0] = command.getName();
            System.arraycopy(strings,0,args,1,strings.length);
            return ci.onTabComplete(commandSender,command,s,args);
        }
    }
}
