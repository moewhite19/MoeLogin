package cn.whiteg.moeLogin;

import cn.whiteg.mmocore.DataCon;
import cn.whiteg.mmocore.MMOCore;
import cn.whiteg.moeLogin.event.PlayerLoginEvent;
import cn.whiteg.moeLogin.listener.AuthenticateListener;
import cn.whiteg.moeLogin.utils.PasswordUtils;
import cn.whiteg.moepacketapi.utils.EntityNetUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cn.whiteg.moeLogin.Setting.*;

public class LoginManage {
    final public static Map<UUID, PlayerLogin> noLogin = Collections.synchronizedMap(new HashMap<>());

    public static boolean isNoLogin(Entity player) {
        if (player instanceof Player){
            if (noLogin.isEmpty()) return false;
            return noLogin.containsKey(player.getUniqueId());
        }
        return false;
    }

    //检查是否自动登录
    public static boolean hasLogin(Player player) {
        try{
            Connection network = EntityNetUtils.getNetWork(EntityNetUtils.getPlayerConnection(EntityNetUtils.getNmsPlayer(player)));
            AuthenticateListener authListener = MoeLogin.plugin.getAuthenticateListener();
            DataCon dc = MMOCore.getPlayerData(player);
            if (authListener != null){
                AuthenticateListener.LoginSession loginSession = MoeLogin.plugin.getAuthenticateListener().getSessionMap().remove(network);
                if ((loginSession != null)){
                    if (loginSession.isPass()){
                        if (Setting.DEBUG){
                            MoeLogin.logger.info("玩家加入会话ID为: " + player.getName());
                            if (loginSession != null){
                                MoeLogin.logger.info("会话验证: " + loginSession.isPass());
                            }
                        }
                        //使用在线登录过记录信息
                        if (dc != null){
                            //记录正版认证
                            if (loginSession.getYggdrasil() == null && !dc.getConfig().getBoolean(successKey,false)){
                                dc.set(successKey,true);
                            }
                            //记录验证UUID
                            final GameProfile oloneGameProfile = loginSession.getOnlineGameProfile();
                            dc.set(uuidKey,oloneGameProfile.getId().toString());
                        }
                        //标记最近登录时间
                        if (dc != null){
                            dc.set("Player.login_time",System.currentTimeMillis());
                        }
                        return true;
                    } else {
                        //理论上如果没有完成的话会先被前面的AsyncLogin事件拦下
                        MoeLogin.plugin.getLogger().warning("玩家" + loginSession.getGameProfile().getName() + "会话验证没有完成，绕过了服务器验证登录系统");
                        noLogin.put(player.getUniqueId(),new PlayerLogin(player));
                        //直接kick会抛出异常
//                        try{
//                            network.close(new ChatMessage("登录失败"));
//                        }catch (Exception e){
//                            e.printStackTrace();
//                        }
                        return false;
                    }
                }
            }
            if (hasAddressLogin(player,player.getAddress().getHostString())){
                //标记最近登录时间
                if (dc != null){
                    dc.set("Player.login_time",System.currentTimeMillis());
                }
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        noLogin.put(player.getUniqueId(),new PlayerLogin(player));
        return false;
    }

    public static boolean hasAddressLogin(Player player,String addr) {
        if (!Setting.noLoginIp.isEmpty() && Setting.noLoginIp.equals(addr)){
            return true;
        }
        if (Setting.AUTO_LOGIN){
            DataCon dc = MMOCore.getPlayerData(player);
            String ip = dc.getString("Player.latest_login_ip");
            if (ip != null && ip.equals(addr)){
                PlayerLoginEvent e = new PlayerLoginEvent(player);
                Bukkit.getPluginManager().callEvent(e);
                return true;
            }
        }
        return false;
    }

    public static boolean hasPassword(DataCon dc,String passwd) {
        if (passwd.length() < 6){
            return false;
        }
        final String md = PasswordUtils.toMD5(passwd);
        if (md.equals(dc.getString(passPat))){
            final String s1 = PasswordUtils.toSha1(passwd);
            final String cs = dc.getString(sha1Pat,null);
            if (cs != null){
                return cs.equals(s1);
            }
            dc.setString(sha1Pat,s1);
            return true;
        }
        return false;
    }

    public static boolean chanPassword(DataCon dc,String password) {
        if (password.length() < 6){
            return false;
        } else if (password.contains(" ")){
            return false;
        } else {
            try{
                dc.set(Setting.passPat,PasswordUtils.toMD5(password));
                dc.set(Setting.sha1Pat,PasswordUtils.toSha1(password));
                MoeLogin.logger.info(dc.getName() + "已修改密码");
                return true;
            }catch (Exception e){
                return false;
            }
        }
    }

    public static boolean logOut(Player player) {
        if (player == null || noLogin.containsKey(player.getUniqueId())) return false;
        DataCon dc = MMOCore.getPlayerData(player);
        dc.setString("Player.latest_login_ip","0.0.0.0");
        noLogin.put(player.getUniqueId(),new PlayerLogin(player));
        return true;
    }

}
