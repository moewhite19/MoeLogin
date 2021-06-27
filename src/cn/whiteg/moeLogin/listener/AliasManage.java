package cn.whiteg.moeLogin.listener;

import cn.whiteg.mmocore.event.DataConDeleteEvent;
import cn.whiteg.moeLogin.MoeLogin;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.event.EventHandler;

import java.io.*;

public class AliasManage {
    private final File file;
    private final BiMap<String, String> bmap = HashBiMap.create();
    private boolean needSave = false;


    public AliasManage(MoeLogin bot) {
        file = new File(bot.getDataFolder(),"alias.map");
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

    public synchronized String unbinding(String player) {
        synchronized (bmap) {
            String qqid = bmap.remove(player);
            if (qqid != null) needSave = true;
            return qqid;
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

    @EventHandler
    public void onDelete(DataConDeleteEvent event) {
        unbinding(event.getDataCon().getName());
    }
}
