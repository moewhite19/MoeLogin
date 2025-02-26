package cn.whiteg.moeLogin.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerLoginEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final LoginType loginType;

    public PlayerLoginEvent(Player who,LoginType loginType) {
        super(who);
        this.loginType = loginType;
    }

    public LoginType getLoginType() {
        return loginType;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public enum LoginType {
        ONLINE,
        OFFLINE,
        OFFLINE_AUTO,
        REGISTER
    }
}
