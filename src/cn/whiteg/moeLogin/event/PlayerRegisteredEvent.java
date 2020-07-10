package cn.whiteg.moeLogin.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerRegisteredEvent extends PlayerEvent implements Cancellable {
    private static HandlerList handler = new HandlerList();
    private boolean cancelled = false;

    public PlayerRegisteredEvent(Player who) {
        super(who);
    }

    public static HandlerList getHandlerList() {
        return handler;
    }

    @Override
    public HandlerList getHandlers() {
        return handler;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
