package com.smittys.modules;

import com.smittys.Tracker;
import com.smittys.db.impl.UserConnection;
import com.smittys.entities.User;
import com.smittys.utils.DateUtils;
import com.smittys.utils.ModuleUtils;
import com.smittys.utils.NumberUtils;
import com.smittys.utils.RoleNames;
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
            boolean loggedIn = isLoggedIn(request);
            model.put("loggedIn", loggedIn);
            if (loggedIn) model.put("user", getUser(request));
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
        if (request.cookies().containsKey("smittys-sess")) {
            String sessionId = request.cookie("smittys-sess");
            Object[] data = UserConnection.connection().handleRequest("get-user-from-sess", sessionId);
            if (data == null) {
                request.cookies().remove("smittys-sess");
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
