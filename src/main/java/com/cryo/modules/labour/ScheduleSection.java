package com.cryo.modules.labour;

import com.google.gson.internal.LinkedTreeMap;
import com.cryo.Tracker;
import com.cryo.db.impl.LabourConnection;
import com.cryo.entities.*;
import com.cryo.modules.WebModule;
import org.apache.commons.lang3.time.DateUtils;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.cryo.modules.WebModule.error;

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
            case "print-buzz-sheets":
                String idString = request.queryParams("id");
                int id;
                try {
                    id = Integer.parseInt(idString);
                } catch(Exception e) {
                    return error("Unable to parse id.");
                }
                Schedule schedule = LabourConnection.connection().selectClass("schedules", "id=?", Schedule.class, id);
                if(schedule == null) return error("Cannot find schedule with that id.");
                schedule.createBuzzSheets();
                prop.put("success", true);
                prop.put("message", "Buzz sheets have been successfully created.");
                break;
            case "print-schedule":
                break;
            case "add-edit-schedule":
                if(request.requestMethod().equals("GET")) {
                    if(request.queryParams().contains("id")) {
                        idString = request.queryParams("id");
                        try {
                            id = Integer.parseInt(idString);
                        } catch(Exception e) {
                            prop.put("success", false);
                            prop.put("error", "Error parsing id.");
                            break;
                        }
                        schedule = LabourConnection.connection().selectClass("schedules", "id=?", Schedule.class, id);
                        if(schedule == null) {
                            prop.put("success", false);
                            prop.put("error", "Unable to find schedule with that id.");
                            break;
                        }
                        model.put("schedule", schedule);
                    }
                    html = WebModule.render("./source/modules/labour/schedules/schedule_modal.jade", model, request, response);
                    prop.put("success", true);
                    prop.put("html", html);
                    break;
                }
                schedule = null;
                if(request.queryParams().contains("id")) {
                    idString = request.queryParams("id");
                    try {
                        id = Integer.parseInt(idString);
                    } catch (Exception e) {
                        prop.put("success", false);
                        prop.put("error", "Error parsing id.");
                        break;
                    }
                    schedule = LabourConnection.connection().selectClass("schedules", "id=?", Schedule.class, id);
                    if (schedule == null) {
                        prop.put("success", false);
                        prop.put("error", "Unable to find schedule with that id.");
                        break;
                    }
                }
                int insertId = -1;
                boolean changeBOH = false;
                boolean boh = false;
                try {
                    String weekStartString = request.queryParams("weekStart");
                    DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    Date date;
                    try {
                        date = new Date(format.parse(weekStartString).getTime());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return error("Week Start must be in MM/dd/yyyy format");
                    }
                    if(schedule != null && !DateUtils.isSameDay(date, schedule.getStartDate())) {
                        prop.put("success", false);
                        prop.put("error", "Unable to change start date for schedules. Please delete and recreate schedule.");
                        break;
                    }
                    boolean isBoh = Boolean.parseBoolean(request.queryParams("isBoh"));
                    String times = request.queryParams("times");
                    ArrayList<ArrayList<LinkedTreeMap<String, String>>> list = Tracker.getGson().fromJson(times, ArrayList.class);
                    if(schedule != null) {
                        if(isBoh != schedule.isBoh()) {
                            changeBOH = true;
                            boh = isBoh;
                        }
                    } else {
                        insertId = LabourConnection.connection().insert("schedules", new Schedule(-1, isBoh, new Date(date.getTime()), null, null).data());
                        if (insertId == -1) {
                            prop.put("success", false);
                            prop.put("error", "Error inserting schedule.");
                            deleteSchedule(insertId);
                            break;
                        }
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
                                if(schedule == null) deleteSchedule(insertId);
                                break dayLoop;
                            }
                            Employee employee = (Employee) data[0];
                            Timestamp startStamp = HoursSection.formatTime(cal.getTime(), start);
                            if (startStamp == null) {
                                prop.put("success", false);
                                prop.put("error", "Error in parsing starting time for " + name + " on day " + i);
                                if(schedule == null) deleteSchedule(insertId);
                                break dayLoop;
                            }
                            if (end.equals("") && isBoh) {
                                prop.put("success", false);
                                prop.put("error", "BOH schedules must have an endtime. Missing endtime for " + name + " on day " + i);
                                if(schedule == null) deleteSchedule(insertId);
                                break dayLoop;
                            }
                            boolean close = false;
                            if(end.equalsIgnoreCase("cl")) close = true;
                            Timestamp endStamp = null;
                            if(!end.equals("") && !close) {
                                endStamp = HoursSection.formatTime(cal.getTime(), end);
                                if (endStamp == null) {
                                    prop.put("success", false);
                                    prop.put("error", "Error in parsing ending time for " + name + " on day " + i);
                                    if(schedule == null) deleteSchedule(insertId);
                                    break dayLoop;
                                }
                            }
                            ScheduleTime time = new ScheduleTime(-1, insertId, employee.getId(), i, startStamp, endStamp, close,null, null);
                            days[i].add(time);
                        }
                    }
                    if(schedule != null)
                        LabourConnection.connection().delete("schedule_times", "schedule_id=?", schedule.getId());
                    for (int i = 0; i < 7; i++) {
                        ArrayList<ScheduleTime> day = days[i];
                        for (ScheduleTime time : day) {
                            if(schedule != null) time.setScheduleId(schedule.getId());
                            LabourConnection.connection().insert("schedule_times", time.data());
                        }
                    }
                    if(changeBOH)
                        LabourConnection.connection().set("schedules", "is_boh=?", "id=?", new Object[] { boh, schedule.getId() });
                } catch(Exception e) {
                    e.printStackTrace();
                    if(insertId != -1 && schedule == null) deleteSchedule(insertId);
                    prop.put("success", false);
                    prop.put("error", "Error inserting schedule");
                    break;
                }
                prop.put("success", true);
                break;
        }
        return Tracker.getGson().toJson(prop);
    }

    public void deleteSchedule(int id) {
        LabourConnection.connection().delete("schedules", "id=?", id);
    }
}
