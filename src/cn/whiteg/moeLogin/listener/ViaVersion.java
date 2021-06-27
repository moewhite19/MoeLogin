package cn.whiteg.moeLogin.listener;

import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import com.mojang.bridge.game.GameVersion;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ViaVersion implements Listener {
    public static final int serverProtocolVersion;

    static {
        GameVersion ver = SharedConstants.getGameVersion();
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
