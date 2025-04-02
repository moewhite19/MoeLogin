package cn.whiteg.moeLogin.utils.logintype;

import cn.whiteg.moeLogin.utils.IpUtils;

import java.util.regex.Pattern;

public class YggdrasilLogin extends LoginType {
    public final String URL;

    public YggdrasilLogin(String url,Pattern pattern,boolean defAllow) {
        super(IpUtils.getHost(url),pattern,defAllow);
        URL = url;
    }

    public YggdrasilLogin(String url,Pattern pattern) {
        this(url,pattern,false);
    }

    @Override
    public boolean isYggdrasil() {
        return true;
    }

    @Override
    public String getYggdrasilUrl() {
        return URL;
    }
}
