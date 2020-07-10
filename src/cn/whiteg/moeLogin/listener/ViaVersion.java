package cn.whiteg.moeLogin.listener;

import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import com.mojang.bridge.game.GameVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ViaVersion implements Listener {
    public static final int serverProtocolVersion;

    static {
//        GameVersion ver = SharedConstants.getGameVersion();
        GameVersion ver = null;
        for (Field f : SharedConstants.class.getDeclaredFields()) {
            if (f.getType().equals(WorldVersion.class) && Modifier.isStatic(f.getModifiers())){
                f.setAccessible(true);
                try{
                    ver = (GameVersion) f.get(null);
                    break;
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
        serverProtocolVersion = ver.getProtocolVersion();
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PacketHandshakingInSetProtocol packet){
            int ver = packet.c();
            if (ver != serverProtocolVersion){
                if (Setting.viaVersion.contains(ver)){
                    var name = packet.d();
                    var protocol = packet.b();
                    event.setPacket(new PacketHandshakingInSetProtocol(name,ver,protocol));
                    MoeLogin.logger.fine("已允许使用协议版本: " + ver);
                } else if (Setting.DEBUG){
                    MoeLogin.logger.info("未知版本协议: " + ver);
                }
            }
        }
    }
}
