package cn.whiteg.moeLogin.utils.logintype;

import java.util.regex.Pattern;

public class OfflineLogin extends LoginType {
    public OfflineLogin(Pattern pattern,boolean defAllow) {
        super("Offline",pattern,defAllow);
    }

    public OfflineLogin(Pattern pattern) {
        this(pattern,false);
    }

    @Override
    public boolean isOnline() {
        return false;
    }
}
