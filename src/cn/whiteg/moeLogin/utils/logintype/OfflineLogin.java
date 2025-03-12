package cn.whiteg.moeLogin.utils.logintype;

import java.util.regex.Pattern;

public class OfflineLogin extends LoginType {
    public OfflineLogin(Pattern pattern) {
        super("Offline",pattern);
    }

    @Override
    public boolean isOnline() {
        return false;
    }
}
