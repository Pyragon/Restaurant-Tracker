package com.cryo.modules;

import com.cryo.Tracker;
import com.cryo.db.impl.UserConnection;
import com.cryo.entities.User;
import com.cryo.utils.DateUtils;
import com.cryo.utils.ModuleUtils;
import com.cryo.utils.NumberUtils;
import com.cryo.utils.RoleNames;
import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.exceptions.JadeCompilerException;
import lombok.Synchronized;
import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public abstract class WebModule {

    public abstract String[] getEndpoints();

    public abstract Object decodeRequest(String endpoint, Request request, Response response);

    @Synchronized
    public static String render(String file, HashMap<String, Object> model, Request request, Response response) {
        try {
            model.put("formatter", new DateUtils());
            model.put("numUtils", new NumberUtils());
            model.put("roles", new RoleNames());
            model.put("utils", new ModuleUtils());
            model.put("name", Tracker.getProperties().get("name"));
            boolean loggedIn = isLoggedIn(request);
            model.put("loggedIn", loggedIn);
            if (loggedIn) model.put("user", getUser(request));
            User user = getUser(request);
            if (user != null && user.getUsername().equalsIgnoreCase("cody")) model.put("isCody", true);
            return Jade4J.render(file, model);
        } catch (JadeCompilerException | IOException e) {
            e.printStackTrace();
            return error("Error loading module template.");
        }
    }

    public static String render404(Request request, Response response) {
        response.status(404);
        HashMap<String, Object> model = new HashMap<>();
        model.put("random", getRandomImageLink());
        try {
            return Jade4J.render("./source/modules/404.jade", model);
        } catch (JadeCompilerException | IOException e) {
            e.printStackTrace();
        }
        return error("Error rendering 404 page! Don't worry, we have put the hamsters back on their wheels! Shouldn't be long...");
    }

    public static String getRandomImageLink() {
        File[] files = new File("./source/images/404/").listFiles();
        File random = files[new Random().nextInt(files.length)];
        return String.format("%simages/404/%s", "http://localhost:5558/", random.getName());
    }

    public static String error(String error) {
        Properties prop = new Properties();
        prop.put("success", false);
        prop.put("error", error);
        return Tracker.getGson().toJson(prop);
    }

    public static boolean isLoggedIn(Request request) {
        return getUser(request) != null;
    }

    public static User getUser(Request request) {
        if (request.cookies().containsKey("cryo-sess")) {
            String sessionId = request.cookie("cryo-sess");
            Object[] data = UserConnection.connection().handleRequest("get-user-from-sess", sessionId);
            if (data == null) {
                request.cookies().remove("cryo-sess");
                return null;
            }
            return (User) data[0];
        }
        return null;
    }

    public static String redirect(String redirect, Request request, Response response) {
        return redirect(redirect, 5, request, response);
    }

    public static String redirect(String redirect, int time, Request request, Response response) {
        if (redirect == null || redirect == "")
            redirect = "/";
        if (time == -1) {
            response.redirect(redirect);
            return "";
        }
        HashMap<String, Object> model = new HashMap<>();
        model.put("redirect", redirect);
        model.put("time", time);
        return render("./source/modules/redirect.jade", model, request, response);
    }
}
