package cn.whiteg.moeLogin.Filter;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import cn.whiteg.moeLogin.LoginManage;
import cn.whiteg.moeLogin.MoeLogin;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleFilter implements Filter {
    private static log4jFilter log4jFilter;

    public static void setupConsoleFilter() {
        try{
            Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
            setLogFilter();
        }catch (ClassNotFoundException | NoClassDefFoundError e){
            ConsoleFilter filter = new ConsoleFilter();
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);
        }
    }

    public static void unsetConsoleFilter() {
        if (log4jFilter != null){
            log4jFilter.stop();
        }
    }

    private static void setLogFilter() {
        org.apache.logging.log4j.core.Logger logger;
        logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        log4jFilter = new log4jFilter();
        logger.addFilter(log4jFilter);
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (isloginmsg(record.getMessage())){
            record.setMessage("");
        }
        return true;
    }

    public static boolean isloginmsg(String msg) {
        final String str = " issued server command: ";
        if (msg.contains(str)){
            String[] fg = msg.split(" issued server command: ");
            String n = fg[0];
            Player player = Bukkit.getPlayerExact(n);
            if (player == null) return false;
            String c = fg[1].toLowerCase();
            if (LoginManage.noLogin.containsKey(player.getUniqueId())){
                MoeLogin.logger.info(n + "尝试登陆");
                return true;
            }
            if (c.startsWith("/ml ") || c.startsWith("/moelogin ")) return true;
        }
        return false;
    }
}
