package cn.whiteg.moeLogin.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

public class Verification {
    private String key;

    public Verification(Player player) {
        key = "red";
        ClickEvent whiteclickevn = new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/white");
        BaseComponent[] cb = new ComponentBuilder("§f使用鼠标点击").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new Text("点我")))
                .event(whiteclickevn)
                .append("§c红色").event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/red"))
                .append("§f字体").event(whiteclickevn).create();
        player.spigot().sendMessage(cb);
    }

    public boolean has(String string) {
        return string.equals(key);
    }
}
