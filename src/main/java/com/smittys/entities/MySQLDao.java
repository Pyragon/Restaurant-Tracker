package com.smittys.entities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public abstract class MySQLDao {

    public Object[] data() {
        ArrayList<Object> list = new ArrayList<>();
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if(!Modifier.isFinal(field.getModifiers())) continue;
                if (field.isAnnotationPresent(MySQLDefault.class)) {
                    list.add("DEFAULT");
                    continue;
                }
                field.setAccessible(true);
                list.add(field.get(this));
            }
        } catch(IllegalAccessException e) {
            e.printStackTrace();
        }
        return list.toArray();
    }

}
