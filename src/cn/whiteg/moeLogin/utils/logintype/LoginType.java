package cn.whiteg.moeLogin.utils.logintype;

import java.util.regex.Pattern;

public abstract class LoginType {
    public static LoginType OFFLINE = new OfflineLogin(Pattern.compile(".*"));
    public static LoginType ONLINE = new MojangLogin(Pattern.compile(".*"));
    final Pattern pattern;
    final String name;
    final boolean defaultAllow;

    protected LoginType(String name,Pattern pattern,boolean allow) {
        this.pattern = pattern;
        this.name = name;
        this.defaultAllow = allow;
    }

    protected LoginType(String name,Pattern pattern) {
        this(name,pattern,true);
    }

    public boolean isMojang() {
        return false;
    }

    public boolean isYggdrasil() {
        return false;
    }

    public boolean isOnline() {
        return true;
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

    public boolean defaultAllow() {
        return defaultAllow;
    }
}
