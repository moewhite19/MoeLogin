package cn.whiteg.moeLogin.listener;

import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ViaVersion implements Listener {
    public static int serverProtocolVersion;

    static {
        for (Field f : SharedConstants.class.getDeclaredFields()) {
            if (f.getType().equals(WorldVersion.class) && Modifier.isStatic(f.getModifiers())){
                f.setAccessible(true);
                try{
                    WorldVersion worldVersion = (WorldVersion) f.get(null);
                    serverProtocolVersion = worldVersion.getProtocolVersion();
                    break;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        ;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientIntentionPacket packet){
            int ver = packet.protocolVersion();
            if (ver != serverProtocolVersion){
                if (Setting.viaVersion.contains(ver)){
                    String name = packet.hostName();
                    int port = packet.port();
                    event.setPacket(new ClientIntentionPacket(ver,name,port,packet.intention()));
                    MoeLogin.logger.fine("已允许使用协议版本: " + ver);
                } else if (Setting.DEBUG){
                    MoeLogin.logger.info("未知版本协议: " + ver);
                }
            }
        }
    }
}
