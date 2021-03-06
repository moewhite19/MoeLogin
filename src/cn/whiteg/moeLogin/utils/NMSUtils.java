package cn.whiteg.moeLogin.utils;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.util.Arrays;

public class NMSUtils {
    public static Entity getNmsEntity(org.bukkit.entity.Entity bukkitEntity) {
        try{
            //noinspection ResultOfMethodCallIgnored
            bukkitEntity.getClass().getField("getHandle").get(bukkitEntity);
        }catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    //根据类型获取Field
    public static Field getFieldFormType(Class<?> clazz,Class<?> type) throws NoSuchFieldException {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getType().equals(type)) return declaredField;
        }
        throw new NoSuchFieldException(type.getName());
    }

    //根据类型获取Field
    public static Field getFieldFormType(Class<?> clazz,String type) throws NoSuchFieldException {
        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getAnnotatedType().getType().getTypeName().equals(type)) return declaredField;
        }
        throw new NoSuchFieldException(type);
    }

    //从数组结构中查找Field
    public static Field getFieldFormStructure(Class<?> clazz,Class<?>[] st,int index) throws NoSuchFieldException {
        var fields = clazz.getDeclaredFields();
        int end = fields.length - st.length;

        loop:
        for (int i = 0; i < end; i++) {
            for (int i1 = 0; i1 < st.length; i1++) {
                var f1 = fields[i + i1];
                var aClass = st[i1];
                if (!f1.getType().equals(aClass)){
                    continue loop;
                }
            }
            return fields[i + index];
        }
        throw new NoSuchFieldException(Arrays.toString(st));
    }
}
