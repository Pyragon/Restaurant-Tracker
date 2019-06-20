package com.smittys.modules.labour;

import com.google.gson.internal.LinkedTreeMap;
import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.*;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.smittys.modules.WebModule.error;

public class ScheduleSection implements WebSection {

    @Override
    public String getName() {
        return "schedules";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        Properties prop = new Properties();
        HashMap<String, Object> model = new HashMap<>();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/labour/schedules/schedule.jade", model, request, response);
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
                Object[] data = LabourConnection.connection().handleRequest("get-schedules", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading schedules.");
                    break;
                }
                ArrayList<Schedule> schedules = (ArrayList<Schedule>) data[0];
                data = LabourConnection.connection().handleRequest("get-schedules-count");
                model.put("schedules", schedules);
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/labour/schedules/schedules_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "add-schedule":
                if(request.requestMethod().equals("GET")) {
                    html = WebModule.render("./source/modules/labour/schedules/add_schedule.jade", model, request, response);
                    prop.put("success", true);
                    prop.put("html", html);
                    break;
                }
                int insertId = -1;
                try {
                    String weekStartString = request.queryParams("weekStart");
                    DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    Date date;
                    try {
                        date = format.parse(weekStartString);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return error("Week Start must be in MM/dd/yyyy format");
                    }
                    boolean isBoh = Boolean.parseBoolean(request.queryParams("isBoh"));
                    String times = request.queryParams("times");
                    ArrayList<ArrayList<LinkedTreeMap<String, String>>> list = Tracker.getGson().fromJson(times, ArrayList.class);
                    Schedule schedule = new Schedule(-1, isBoh, new Timestamp(date.getTime()), null, null);
                    insertId = LabourConnection.connection().insert("schedules", schedule.data());
                    if (insertId == -1) {
                        prop.put("success", false);
                        prop.put("error", "Error inserting schedule.");
                        deleteSchedule(insertId);
                        break;
                    }
                    ArrayList<ScheduleTime>[] days = new ArrayList[7];
                    dayLoop:
                    for (int i = 0; i < 7; i++) {
                        days[i] = new ArrayList<>();
                        if (i >= list.size()) continue;
                        ArrayList<LinkedTreeMap<String, String>> mList = list.get(i);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);
                        cal.add(Calendar.DAY_OF_WEEK, i);
                        for (LinkedTreeMap<String, String> map : mList) {
                            String name = map.get("name");
                            String start = map.get("start");
                            String end = map.get("end");
                            data = LabourConnection.connection().handleRequest("get-employee-by-name", name);
                            if (data == null) {
                                prop.put("success", false);
                                prop.put("error", "Unable to find employee for that name.");
                                deleteSchedule(insertId);
                                break dayLoop;
                            }
                            Employee employee = (Employee) data[0];
                            Timestamp startStamp = HoursSection.formatTime(cal.getTime(), start);
                            if (startStamp == null) {
                                prop.put("success", false);
                                prop.put("error", "Error in parsing starting time for " + name + " on day " + i);
                                deleteSchedule(insertId);
                                break dayLoop;
                            }
                            if (end.equals("") && isBoh) {
                                prop.put("success", false);
                                prop.put("error", "BOH schedules must have an endtime. Missing endtime for " + name + " on day " + i);
                                deleteSchedule(insertId);
                                break dayLoop;
                            }
                            Timestamp endStamp = HoursSection.formatTime(cal.getTime(), end);
                            if (endStamp == null) {
                                prop.put("success", false);
                                prop.put("error", "Error in parsing ending time for " + name + " on day " + i);
                                deleteSchedule(insertId);
                                break dayLoop;
                            }
                            ScheduleTime time = new ScheduleTime(-1, insertId, employee.getId(), i, startStamp, endStamp, null, null);
                            days[i].add(time);
                        }
                    }
                    for (int i = 0; i < 7; i++) {
                        ArrayList<ScheduleTime> day = days[i];
                        for (ScheduleTime time : day)
                            LabourConnection.connection().insert("schedule_times", time.data());
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    if(insertId != -1) deleteSchedule(insertId);
                    prop.put("success", false);
                    prop.put("error", "Error inserting schedule");
                    break;
                }
                prop.put("success", false);
                prop.put("error", "Testing");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }

    public void deleteSchedule(int id) {
        LabourConnection.connection().delete("schedules", "id=?", id);
    }
}
