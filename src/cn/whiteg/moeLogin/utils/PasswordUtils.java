package cn.whiteg.moeLogin.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class PasswordUtils {
    private final static Pattern MC_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    public static String toMD5(String inStr) {
        final MessageDigest md5;
        try{
            md5 = MessageDigest.getInstance("MD5");
        }catch (Exception e){
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }

        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++) {
            byteArray[i] = (byte) charArray[i];
        }

        byte[] md5Bytes = md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();

        for (int i = 0; i < md5Bytes.length; i++) {

            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();

    }

    public static String toSha1(String str) {
        final MessageDigest md5;
        try{
            md5 = MessageDigest.getInstance("SHA");
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
            return "";
        }
        md5.update(str.getBytes());
        byte[] summery = md5.digest();
        StringBuffer md5StrBuff = new StringBuffer();
        for (int i = 0; i < summery.length; i++) {
            if (Integer.toHexString(0xFF & summery[i]).length() == 1){
                md5StrBuff.append("0").append(
                        Integer.toHexString(0xFF & summery[i]));
            } else {
                md5StrBuff.append(Integer.toHexString(0xFF & summery[i]));
            }
        }
        return md5StrBuff.toString();
    }

    public static boolean checkName(String str) {
        return MC_USERNAME_PATTERN.matcher(str).matches();
    }
}
