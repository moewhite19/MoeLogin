package cn.whiteg.moeLogin.utils.logintype;

import cn.whiteg.moeLogin.utils.MojangAPI;

import java.util.regex.Pattern;

public class MojangLogin extends LoginType {
    public MojangLogin(Pattern pattern,boolean defAllow) {
        super("Mojang",pattern,defAllow);
    }

    public MojangLogin(Pattern pattern) {
        this(pattern,false);
    }

    @Override
    public boolean isMojang() {
        return true;
    }

    @Override
    public String getYggdrasilUrl() {
        return MojangAPI.MOJANG_BASE_URL;
    }
}
