package cn.whiteg.moeLogin.utils.logintype;

import java.util.regex.Pattern;

public abstract class LoginType {
    public final static LoginType OFFLINE = new OfflineLogin(Pattern.compile(".*"));
    public final static LoginType ONLINE = new MojangLogin(Pattern.compile(".*"));
    final Pattern pattern;
    final String name;

    protected LoginType(String name,Pattern pattern) {
        this.pattern = pattern;
        this.name = name;
    }

    public boolean isMojang() {
        return false;
    }

    public boolean isYggdrasil() {
        return false;
    }

    public boolean isOnline() {
        return isMojang() || isYggdrasil();
    }

    //Yggdrasil地址
    public String getYggdrasilUrl() {
        return null;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
