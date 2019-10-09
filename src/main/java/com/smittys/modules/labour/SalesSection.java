package com.smittys.modules.labour;

import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.DailySales;
import com.smittys.entities.Note;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import lombok.Cleanup;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import static com.smittys.modules.WebModule.error;

public class SalesSection implements WebSection {

    @Override
    public String getName() {
        return "sales";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        switch (action) {
            case "load":
                String html = null;
                try {
                    html = WebModule.render("./source/modules/labour/sales/sales.jade", model, request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                    prop.put("success", false);
                    prop.put("error", e.getMessage());
                    break;
                }
                if (html == null) {
                    prop.put("success", false);
                    prop.put("error", "Unable to load section.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", html);
                break;
            case "load-list":
                int page = Integer.parseInt(request.queryParams("page"));
                Object[] data = LabourConnection.connection().handleRequest("get-all-sales-days", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading sales.");
                    break;
                }
                ArrayList<Timestamp> stamps = (ArrayList<Timestamp>) data[0];
                ArrayList<DailySales> dailySales = new ArrayList<>(stamps.size());
                for(Timestamp t : stamps) {
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    Object salesData = Tracker.getInstance().getCachingManager().get("daily-sales-cache").getCachedData(format.format(t));
                    if(salesData == null)
                        return error("Unable to load sales date: "+format.format(t));
                    dailySales.add((DailySales) salesData);
                }
                data = LabourConnection.connection().handleRequest("get-all-sales-days-count");
                model.put("daily", dailySales);
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/labour/sales/sales_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "add-sales":
                if(request.requestMethod().equals("GET")) {
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/labour/sales/add_sales.jade", model, request, response));
                    break;
                }
                System.out.println("Here");
                String dateString = request.queryParams("date");
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                Date date;
                System.out.println(request.queryParams());
                try {
                    format.parse(dateString);
                } catch(Exception e) {
                    e.printStackTrace();
                    return error("Unable to parse date.");
                }
                System.out.println("Here");
                File dir = new File("./source/uploaded_sales/");
                try {
                    Path tempFile = Files.createTempFile(dir.toPath(), "", "");
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
                    @Cleanup
                    InputStream stream = request.raw().getPart("file").getInputStream();
                    Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);

                    File file = tempFile.toFile();
                    File renamed = new File(dir.toPath()+"/"+dateString+".html");
                    file.renameTo(renamed);

                    prop.put("success", true);
                    prop.put("message", "Should have saved now.");

                } catch(IOException | ServletException e) {
                    e.printStackTrace();
                }
                break;
            case "add-note":
                if (request.requestMethod().equals("GET")) {
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/labour/sales/add_note.jade", model, request, response));
                    break;
                }
                dateString = request.queryParams("date");
                String note = request.queryParams("note");
                format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    date = format.parse(dateString);
                } catch (Exception e) {
                    prop.put("success", false);
                    prop.put("error", "Error parsing date for note.");
                    break;
                }
                if (note.length() > 50) {
                    prop.put("success", false);
                    prop.put("error", "Note cannot exceed 50 characters.");
                    break;
                }
                Timestamp stamp = new Timestamp(date.getTime());
                LabourConnection.connection().handleRequest("add-note", new Note(-1, stamp, note, null));
                prop.put("success", true);
                break;
            default:
                prop.put("success", false);
                prop.put("error", "Invalid action.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
