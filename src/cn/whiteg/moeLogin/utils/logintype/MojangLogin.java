package cn.whiteg.moeLogin.utils.logintype;

import cn.whiteg.moeLogin.utils.MojangAPI;

import java.util.regex.Pattern;

public class MojangLogin extends LoginType {
    public MojangLogin(Pattern pattern) {
        super("Mojang",pattern);
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
