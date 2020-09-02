package cn.whiteg.moeLogin.listener;

import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import com.mojang.bridge.game.GameVersion;
import net.minecraft.server.v1_16_R2.PacketHandshakingInSetProtocol;
import net.minecraft.server.v1_16_R2.SharedConstants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ViaVersion implements Listener {
    public static int serverProtocolVersion;
    public static Field handshakinProtocol;

    static {
        GameVersion ver = SharedConstants.getGameVersion();
        try{
            Method method = ver.getClass().getDeclaredMethod("getProtocolVersion");
            method.setAccessible(true);
            serverProtocolVersion = (int) method.invoke(ver);
            handshakinProtocol = PacketHandshakingInSetProtocol.class.getDeclaredField("a");
            handshakinProtocol.setAccessible(true);
        }catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e){
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onLogin(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PacketHandshakingInSetProtocol){
            PacketHandshakingInSetProtocol packet = (PacketHandshakingInSetProtocol) event.getPacket();
            try{
                int protocol = (int) handshakinProtocol.get(packet);
                if (protocol != serverProtocolVersion){
                    if (Setting.viaVersion.contains(protocol)){
                        handshakinProtocol.set(packet,serverProtocolVersion);
                        MoeLogin.logger.info("已允许使用协议版本: " + protocol);
                    } else if (Setting.DEBUG){
                        MoeLogin.logger.info("未知版本协议: " + protocol);
                    }
                }
            }catch (IllegalAccessException e){
                e.printStackTrace();
            }
        }
    }
}
