package com.smittys.utils;

import com.smittys.db.impl.LabourConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RoleNames {

    public static int HOST = 1, SERVER = 2, COOK = 3, PREP = 4, DISHWASHER = 5, HEAD_HOSTESS = 6;

    public static int[] BOH = { COOK, PREP, DISHWASHER };
    public static int[] FOH = { HOST, SERVER, HEAD_HOSTESS };

    private static HashMap<Integer, String> names = new HashMap<>();

    static {
        loadNames();
    }

    public static void loadNames() {
        try {
            Object[] data = LabourConnection.connection().handleRequest("load-roles");
            if (data == null)
                return;
            names = (HashMap<Integer, String>) data[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getName(int role) {
        if (names.containsKey(role))
            return names.get(new Integer(role));
        return "Not found.";
    }

    public static HashMap<Integer, String> getNames() {
        return names;
    }

    public static ArrayList<String> getNameList() {
        return new ArrayList<>(names.values());
    }

    public static int getId(String name) {
        for (Map.Entry<Integer, String> entry : names.entrySet())
            if (entry.getValue().toString().equalsIgnoreCase(name))
                return (int) entry.getKey();
        return -1;
    }

}
