package cn.whiteg.moeLogin.Filter;

import cn.whiteg.moeLogin.LoginManage;
import cn.whiteg.moeLogin.MoeLogin;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ConsoleFilter implements Filter {
    static Filter originalFilter = null;
    private static log4jFilter log4jFilter;

    //屏蔽玩家在登录时输入的指令，使其后台无法看到。
    //但是在spigot已经无法工作了
    public static void setupConsoleFilter() {
        try{
            Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
            setLogFilter();
        }catch (ClassNotFoundException | NoClassDefFoundError e){
            ConsoleFilter filter = new ConsoleFilter();
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);

        }

        final Logger logger = Logger.getLogger("Minecraft");
        originalFilter = logger.getFilter();
        ConsoleFilter filter = new ConsoleFilter();
        logger.setFilter(filter);
    }

    //Hook控制台
    private static void setLogFilter() {
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        log4jFilter = new log4jFilter();
        logger.addFilter(log4jFilter);
    }

    //注销控制台Hook
    public static void unsetConsoleFilter() {
        if (log4jFilter != null){
            log4jFilter.stop();
        }
    }

    public static void shutdown() {
        Logger.getLogger("Minecraft").setFilter(originalFilter);
    }


    //是否为登录指令
    public static boolean isLoginMessage(String msg) {
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
            return c.startsWith("/ml ") || c.startsWith("/moelogin ");
        }
        return false;
    }

    //隐藏登录指令
    @Override
    public boolean isLoggable(LogRecord record) {
        if (isLoginMessage(record.getMessage())){
            record.setMessage("");
            return false;
        }
        return true;
    }

}
