package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.event.DataConCreateEvent;
import cn.whiteg.mmocore.event.DataConDeleteEvent;
import cn.whiteg.mmocore.event.DataConRenameEvent;
import cn.whiteg.moeLogin.MoeLogin;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.*;

public class AliasManage implements Listener {
    private final File file;
    private final BiMap<String, String> bmap = HashBiMap.create();
    private boolean needSave = false;


    public AliasManage(MoeLogin plugin) {
        file = new File(plugin.getDataFolder(),"alias.map");
    }

    public boolean binding(String player,String alias) {
        try{
            synchronized (bmap) {
                bmap.put(player,alias);
                needSave = true;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public synchronized String deleteFormPlayer(String player) {
        synchronized (bmap) {
            String alias = bmap.remove(player);
            if (alias != null) needSave = true;
            return alias;
        }
    }

    public synchronized String deleteFormAlias(String alias) {
        synchronized (bmap) {
            String player = bmap.inverse().remove(alias);
            if (player != null) needSave = true;
            return player;
        }
    }

    public void load() {
        if (file.exists()){
            synchronized (bmap) {
                bmap.clear();
                try{
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);
                    //如果读取的内容不为空，则执行下面语句
                    String f;
                    while ((f = br.readLine()) != null) {
                        String[] arg = f.split(":");
                        if (arg.length < 1) continue;
                        bmap.put(arg[0],arg[1]);
                    }
                    br.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void save() {
        if (!needSave || bmap.isEmpty()){
            return;
        }
        synchronized (bmap) {
            try{
                if (!file.exists()){
                    File p = file.getParentFile();
                    if (!p.exists()){
                        p.mkdirs();
                    }
                    file.createNewFile();
                }
                //1、打开流
                Writer w = new FileWriter(file,false);
                var i = bmap.entrySet().iterator();
                //2、写入内容
                while (i.hasNext()) {
                    var e = i.next();
                    w.write(e.getKey() + ':' + e.getValue());
                    if (i.hasNext()) w.write('\n');
                }
                //3、关闭流
                w.close();
                needSave = false;
            }catch (IOException e){
                System.out.println("文件写入错误：" + e.getMessage());
            }

        }
    }

    public BiMap<String, String> getMap() {
        return bmap;
    }

    public boolean isNeedSave() {
        return needSave;
    }

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    public String getAlias(String player) {
        synchronized (bmap) {
            return bmap.get(player);
        }
    }

    public String getPlayer(String alias) {
        synchronized (bmap) {
            return bmap.inverse().get(alias);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDelete(DataConDeleteEvent event) {
        deleteFormPlayer(event.getDataCon().getName());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCreate(DataConCreateEvent event) {
        var player = event.getDataCon().getName();
        var alias = deleteFormAlias(player);
        if (alias != null){
            MoeLogin.logger.warning("创建的玩家数据" + player + "和" + alias + "的别名重复,已删除别名");
        }
    }

    public boolean isEnable() {
        return file.isFile();
    }
}
