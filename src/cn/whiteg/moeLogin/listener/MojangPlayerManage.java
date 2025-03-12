package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.mmocore.event.DataConDeleteEvent;
import cn.whiteg.mmocore.event.DataConRenameEvent;
import cn.whiteg.moeLogin.MoeLogin;
import cn.whiteg.moeLogin.Setting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MojangPlayerManage implements Listener {
    static int VER = 1;
    static final byte[] HEAD = "MLP".getBytes(StandardCharsets.UTF_8);
    private final File file;
    boolean change = false;


    private final BiMap<UUID, String> premiumMap = HashBiMap.create(Collections.synchronizedMap(new HashMap<>()));

    public MojangPlayerManage(File file) {
        this.file = file;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void load() {
        if (!file.exists()){
            final Iterator<DataCon> it = MMOCore.iteratorPlayerData();
            int i = 0;
            while (it.hasNext()) {
                final DataCon dc = it.next();
                final String uuidStr = dc.getString(Setting.uuidKey);
                if (uuidStr == null) continue;
                UUID uuid = UUID.fromString(uuidStr);
                final String name = dc.getName();
                final String latePlayer = getPlayer(uuid);
                if ((latePlayer == null || !latePlayer.equals(name))){
                    addPlayer(name,UUID.fromString(uuidStr));
                    i++;
                }
            }
            MoeLogin.logger.info("§b已加载§f" + i + "§b个正版玩家id");
            save();
            return;
        }
        try (DataInputStream input = new DataInputStream(new FileInputStream(file))){
            if (Arrays.equals(input.readNBytes(HEAD.length),HEAD) && input.readInt() == VER){
                while (input.available() > 0) {
                    String readName = new String(input.readNBytes(input.read()),StandardCharsets.UTF_8);
                    UUID uuid = new UUID(input.readLong(),input.readLong());
                    if (input.readShort() == '\n'){
                        try{
                            premiumMap.put(uuid,readName);
                        }catch (IllegalArgumentException e){
                            e.printStackTrace();
                        }
                    } else {
                        MoeLogin.logger.warning("加载文件出错, 位置: " + readName);
                        break;
                    }
                }
            } else {
                MoeLogin.logger.warning("无效的文件: " + file);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void save() {
        if (file.exists() && (premiumMap.isEmpty() || !change)) return; //空的不管
        try{
            if (!file.exists() && !file.createNewFile()){
                MoeLogin.logger.warning("无法创建文件: " + file);
                return;
            }
            try (DataOutputStream output = new DataOutputStream(new FileOutputStream(file,false))){
                //写入头和版本号
                output.write(HEAD);
                output.writeInt(VER);
                premiumMap.forEach((uuid,name) -> {
                    final byte[] nameArray = name.getBytes(StandardCharsets.UTF_8);
                    try{
                        output.write(nameArray.length & 0xff);
                        output.write(nameArray);
                        output.writeLong(uuid.getMostSignificantBits());
                        output.writeLong(uuid.getLeastSignificantBits());
                        output.writeShort('\n');
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                });
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String getPlayer(UUID uuid) {
        return premiumMap.get(uuid);
    }

    public UUID getUUID(String player) {
        return premiumMap.inverse().get(player);
    }

    public void addPlayer(String player,UUID uuid) {
        if (premiumMap.inverse().containsKey(player)) removePlayer(player); //如果有绑定其他UUID就先删除

        if (premiumMap.put(uuid,player) == null) change = true;
    }

    public String removePlayer(UUID player) {
        final String removed = premiumMap.remove(player);
        if (removed != null) change = true;
        return removed;
    }

    public UUID removePlayer(String player) {
        final UUID removed = premiumMap.inverse().remove(player);
        if (removed != null) change = true;
        return removed;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        DataCon dc = MMOCore.getPlayerData(event.getPlayer());

        //更新正版的UUID绑定
        final String nowUUID = dc.getString(Setting.uuidKey);
        if (nowUUID != null){
            UUID uuid = UUID.fromString(nowUUID);
            final String name = dc.getName();
            final String latePlayer = getPlayer(uuid);
            //如果找到的正版UUID绑定的玩家不是这个，就自动重新绑定吧
            if ((latePlayer == null || !latePlayer.equals(name))){
                addPlayer(name,UUID.fromString(nowUUID));
                MoeLogin.logger.info("已更新玩家UUID绑定");
            }
        }
        save();

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDelete(DataConDeleteEvent event) {
        removePlayer(event.getDataCon().getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRename(DataConRenameEvent event) {
        final String uuid = event.getDataCon().getString(Setting.uuidKey);
        if (uuid != null){
            addPlayer(event.getNewName(),UUID.fromString(uuid));
            MoeLogin.logger.info("已重命名玩家UUID绑定");
        }
    }
}
