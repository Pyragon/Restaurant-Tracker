package com.smittys.utils;

import com.mysql.jdbc.StringUtils;
import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.managers.EmailManager;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Utilities {

    private static String[] ROUTES = {"GET", "/utilities/:action", "POST", "/utilities/:action"};

    public static void registerEndpoints() {
        int index = 0;
        while (index < ROUTES.length) {
            String method = ROUTES[index++];
            String route = ROUTES[index++];
            Route sparkRoute = (req, res) -> handleEndpoints(req, res);
            if (method.equals("GET")) Spark.get(route, sparkRoute);
            else Spark.post(route, sparkRoute);
        }
    }

    public static String handleEndpoints(Request request, Response response) {
        String action = request.params(":action");
        Properties prop = new Properties();
        switch (action) {
            case "clear-cache":
                Tracker.getInstance().getCachingManager().clear();
                prop.put("success", true);
                break;
            case "check-emails":
                prop.put("success", true);
//                prop.put("error", "Needs to be added to a queue.");
                EmailManager.checkEmails();
                break;
            default:
                prop.put("success", false);
                prop.put("error", "Invalid action.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.startConnections();
        InventoryConnection.connection().handleRequest("fix-pack-units");
    }

    public static boolean isNullOrEmpty(String... strings) {
        for (String s : strings)
            if (StringUtils.isNullOrEmpty(s)) return true;
        return false;
    }

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    public static long roundUp(long num, long divisor) {
        return (num + divisor - 1) / divisor;
    }

    public static String toJadeArray(Collection<String> list) {
        StringBuilder builder = new StringBuilder();
        list.stream().map(s -> "\"" + s + "\", ").forEach(builder::append);
        String s = builder.toString();
        s = replaceLast(s, ", ", "");
        return "[ " + s + " ]";
    }

    @SuppressWarnings({"rawtypes"})
    public static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile().replaceAll("%20", " ")));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    @SuppressWarnings("rawtypes")
    private static List<Class> findClasses(File directory, String packageName) {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                } catch (Throwable e) {

                }
            }
        }
        return classes;
    }

    public static String formatNameForProtocol(String name) {
        if (name == null) return "";
        name = name.replaceAll(" ", "_");
        name = name.toLowerCase();
        return name;
    }

    public static String formatNameForDisplay(String name) {
        if (name == null) return "";
        name = name.replaceAll("_", " ");
        name = name.toLowerCase();
        StringBuilder newName = new StringBuilder();
        boolean wasSpace = true;
        for (int i = 0; i < name.length(); i++) {
            if (wasSpace) {
                newName.append(("" + name.charAt(i)).toUpperCase());
                wasSpace = false;
            } else {
                newName.append(name.charAt(i));
            }
            if (name.charAt(i) == ' ') {
                wasSpace = true;
            }
        }
        return newName.toString();
    }
}
