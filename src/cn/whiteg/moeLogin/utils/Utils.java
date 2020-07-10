package cn.whiteg.moeLogin.utils;


import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;

import java.lang.reflect.Field;

public class Utils {
    public static double HorizontalDistance(Location l1,Location l2) {
        if (l1.getWorld() != l2.getWorld()) return Double.MAX_VALUE;
        double x = l1.getX();
        if (x < l2.getX()){
            x = l2.getX() - x;
        } else {
            x = x - l2.getX();
        }
        double z = l1.getZ();
        if (z < l2.getZ()){
            z = l2.getZ() - z;
        } else {
            z = z - l2.getZ();
        }
        return x + z;
    }

    public static void setOlineModele(boolean b) {
        Server ser = Bukkit.getServer();
        try{
            Field console_f = ser.getClass().getDeclaredField("console");
            console_f.setAccessible(true);
            DedicatedServer con = (DedicatedServer) console_f.get(ser);
            Field motdf = MinecraftServer.class.getDeclaredField("onlineMode");
            motdf.setAccessible(true);
            motdf.set(con,b);
            if (Setting.DEBUG){
                MoeLogin.plugin.getLogger().info("修改服务器登陆模式为 " + (Bukkit.getOnlineMode() ? "在线" : "离线"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
