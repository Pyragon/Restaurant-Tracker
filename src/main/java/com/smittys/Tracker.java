package com.smittys;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smittys.cache.CachingManager;
import com.smittys.db.DBConnectionManager;
import com.smittys.managers.ErrorManager;
import com.smittys.managers.TimerManager;
import com.smittys.managers.cron.CronJobManager;
import com.smittys.modules.WebModule;
import com.smittys.modules.search.SearchManager;
import com.smittys.utils.Utilities;
import lombok.Getter;
import spark.Spark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static spark.Spark.*;

public class Tracker {

    private static @Getter
    Tracker instance;
    private @Getter
    Properties properties;
    private static @Getter
    Gson gson;
    private static @Getter
    DBConnectionManager connectionManager;
    private @Getter
    ErrorManager errorManager;
    private @Getter
    SearchManager searchManager;
    private @Getter
    TimerManager timerManager;
    private @Getter
    CachingManager cachingManager;
    private @Getter
    CronJobManager cronJobManager;

    public void startConnections() {
        gson = buildGson();
        loadProperties();
        instance = this;
        connectionManager = new DBConnectionManager();
        connectionManager.init();
        errorManager = new ErrorManager();

    }


    public void startup() {
        gson = buildGson();
        loadProperties();
        searchManager = new SearchManager();
        connectionManager = new DBConnectionManager();
        cachingManager = new CachingManager();
        cronJobManager = new CronJobManager();
        timerManager = new TimerManager();
        errorManager = new ErrorManager();
        cachingManager.loadCachedItems();
        connectionManager.init();
        searchManager.load();
        cronJobManager.init();
        try {
            port(5558);
            staticFiles.externalLocation("source/");
            staticFiles.expireTime(0);
            staticFiles.header("Access-Control-Allow-Origin", "*");
            for (Class<?> c : Utilities.getClasses("com.smittys.modules")) {
                if (!WebModule.class.isAssignableFrom(c)) continue;
                if (c.getName().equals("com.smittys.modules.WebModule")) continue;
                Object o = c.newInstance();
                if (!(o instanceof WebModule)) continue;
                WebModule module = (WebModule) o;
                int i = 0;
                while (i < module.getEndpoints().length) {
                    String method = module.getEndpoints()[i++];
                    String path = module.getEndpoints()[i++];
                    if (method.equals("GET")) get(path, (req, res) -> module.decodeRequest(path, req, res));
                    else post(path, (req, res) -> module.decodeRequest(path, req, res));
                }
            }
            SearchManager.registerEndpoints();
            Utilities.registerEndpoints();
            errorManager.registerEndpoints();
            timerManager.run();
            System.out.println("Server started on " + java.net.InetAddress.getLocalHost() + ":" + Spark.port());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        instance = new Tracker();
        instance.startup();
    }

    public static Gson buildGson() {
        return new GsonBuilder().serializeNulls().setVersion(1.0).disableHtmlEscaping().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    }

    public void loadProperties() {
        File file = new File("props.json");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            StringBuilder json = new StringBuilder();
            while ((line = reader.readLine()) != null) json.append(line);
            properties = getGson().fromJson(json.toString(), Properties.class);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
