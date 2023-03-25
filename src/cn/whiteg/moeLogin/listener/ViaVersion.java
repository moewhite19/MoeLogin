package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.util.NMSUtils;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import cn.whiteg.moepacketapi.api.event.PacketReceiveEvent;
import net.minecraft.MinecraftVersion;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.network.EnumProtocol;
import net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol;
import net.minecraft.world.level.storage.DataVersion;
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
                    MinecraftVersion version = (MinecraftVersion) f.get(null);
                    final Field field = NMSUtils.getFieldFormStructure(MinecraftVersion.class,DataVersion.class,int.class)[1];
                    field.setAccessible(true);
                    serverProtocolVersion = (int) field.get(version);
                    break;
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        ;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogin(PacketReceiveEvent event) {
        if (event.getPacket() instanceof PacketHandshakingInSetProtocol packet){
            int ver = packet.c();
            if (ver != serverProtocolVersion){
                if (Setting.viaVersion.contains(ver)){
                    String name = packet.d();
                    EnumProtocol protocol = packet.a();
                    event.setPacket(new PacketHandshakingInSetProtocol(name,ver,protocol));
                    MoeLogin.logger.fine("已允许使用协议版本: " + ver);
                } else if (Setting.DEBUG){
                    MoeLogin.logger.info("未知版本协议: " + ver);
                }
            }
        }
    }
}
