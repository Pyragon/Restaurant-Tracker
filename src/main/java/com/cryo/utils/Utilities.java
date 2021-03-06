package com.cryo.utils;

import com.cryo.Tracker;
import com.cryo.db.impl.LabourConnection;
import com.cryo.db.impl.UserConnection;
import com.cryo.entities.Schedule;
import com.cryo.entities.ScheduleTime;
import com.cryo.entities.User;
import com.cryo.managers.EmailManager;
import com.cryo.modules.WebModule;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static com.cryo.modules.WebModule.error;

public class Utilities {

    public static String[] ROUTES = {"GET", "/utilities/:action", "POST", "/utilities/:action"};

    public static void registerEndpoints(String[] routes) {
        int index = 0;
        while (index < routes.length) {
            String method = routes[index++];
            String route = routes[index++];
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
            case "create-user":
                if (!WebModule.isLoggedIn(request)) {
                    prop.put("success", false);
                    prop.put("error", "You must be logged in to do this.");
                    break;
                }
                User user = WebModule.getUser(request);
                if (!user.getUsername().equalsIgnoreCase("cody")) {
                    prop.put("success", false);
                    prop.put("error", "Only Cody is allowed to do this.");
                    break;
                }
                if (request.requestMethod().equals("GET")) {
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/utils/create_user.jade", new HashMap<>(), request, response));
                    break;
                }
                String username = request.queryParams("username");
                String firstName = request.queryParams("firstName");
                String lastName = request.queryParams("lastName");
                String password = request.queryParams("password");
                String salt = BCrypt.generateSalt();
                String hash = BCrypt.hashPassword(password, salt);
                Object[] data = UserConnection.connection().handleRequest("create-user", new User(-1, username, firstName, lastName, hash, salt, null));
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error adding user.");
                    break;
                }
                prop.put("success", true);
                break;
            case "change-pass":
                if (request.requestMethod().equals("GET")) {
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/utils/change_pass.jade", new HashMap<>(), request, response));
                    break;
                }
                String oldPass = request.queryParams("oldPass");
                String newPass = request.queryParams("newPass");
                user = WebModule.getUser(request);
                data = UserConnection.connection().handleRequest("compare", user.getUsername(), oldPass);
                if (data == null) return error("Error loading employee data. Please report this to Cody.");
                boolean correct = (boolean) data[0];
                if (!correct) return error("Invalid current password. Please try again.");
                hash = BCrypt.hashPassword(newPass, user.getSalt());
                UserConnection.connection().handleRequest("change-pass", hash, user.getId());
                UserConnection.connection().handleRequest("remove-all-sess", user.getUsername());
                prop.put("success", true);
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
        User user = UserConnection.connection().selectClass("user_data", "username=?", User.class, "cody");
        if(user == null) return;
        String salt = user.getSalt();
        String hash = BCrypt.hashPassword("sdfsdf", salt);
        UserConnection.connection().set("user_data", "hash=?", "id=?", hash, user.getId());
        System.out.println("Saved");
    }

    public static ArrayList<ScheduleTime> getTimes(Schedule schedule, int day) {
        Object[] data = LabourConnection.connection().handleRequest("get-schedule-times-by-id-and-day", schedule.getId(), day);
        if(data == null) return new ArrayList<>();
        return (ArrayList<ScheduleTime>) data[0];
    }

    public static boolean isNullOrEmpty(String... strings) {
        for (String s : strings)
            if (StringUtils.isBlank(s)) return true;
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
