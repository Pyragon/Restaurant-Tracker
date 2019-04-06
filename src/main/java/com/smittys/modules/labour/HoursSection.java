package com.smittys.modules.labour;

import com.mysql.jdbc.StringUtils;
import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.Employee;
import com.smittys.entities.Error;
import com.smittys.entities.HourData;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import com.smittys.utils.RoleNames;
import com.smittys.utils.Utilities;
import org.apache.commons.lang3.math.NumberUtils;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.smittys.modules.WebModule.error;

public class HoursSection implements WebSection {

    @Override
    public String getName() {
        return "hours";
    }


    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        String method = request.requestMethod();
        switch (action) {
            case "load":
                String html;
                String query = request.queryParams("query");
                String day = request.queryParams("day");
                if (query != null) model.put("query", query);
                if (day != null) model.put("day", day);
                try {
                    html = WebModule.render("./source/modules/labour/hours/hours.jade", model, request, response);
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
                String filter = request.queryParams("filter");
                Object[] data = LabourConnection.connection().handleRequest("get-hours", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading hours.");
                    break;
                }
                ArrayList<HourData> hours = (ArrayList<HourData>) data[0];
                model.put("hourdata", hours);
                data = LabourConnection.connection().handleRequest("get-hours-count");
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/labour/hours/lists/hours_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "delete":
                LabourConnection.connection().handleRequest("delete-hours", Integer.parseInt(request.queryParams("id")));
                prop.put("success", true);
                break;
            case "add":
                if (method.equals("GET")) {
                    String idString = request.queryParams("id");
                    String defaultDateString = request.queryParams("defaultDate");
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                    Date date;
                    if (StringUtils.isNullOrEmpty(defaultDateString)) {
                        cal.setTime(new Date());
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        date = new Date(cal.getTimeInMillis());
                    } else {
                        try {
                            date = format.parse(defaultDateString);
                        } catch (Exception e) {
                            cal.setTime(new Date());
                            cal.add(Calendar.DAY_OF_MONTH, -1);
                            date = new Date(cal.getTimeInMillis());
                            prop.put("errorMessage", "Error parsing default date.");
                        }
                    }
                    Employee employee = null;
                    if (!StringUtils.isNullOrEmpty(idString)) {
                        try {
                            int id = Integer.parseInt(idString);
                            data = LabourConnection.connection().handleRequest("get-employee", id);
                            if (data != null) employee = (Employee) data[0];
                        } catch (Exception e) {
                            e.printStackTrace();
                            employee = null;
                        }
                    }
                    model.put("date", date);
                    model.put("employee", employee);
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/labour/hours/add_hours.jade", model, request, response));
                    break;
                }
                String name = request.queryParams("name");
                String dateString = request.queryParams("date");
                String startTime = request.queryParams("startTime");
                String endTime = request.queryParams("endTime");
                String breakString = request.queryParams("breakTime");
                String roleString = request.queryParams("role");
                if (Utilities.isNullOrEmpty(name, dateString, startTime, endTime, roleString)) {
                    prop.put("success", false);
                    prop.put("error", "All fields other than break time must be filled out.");
                    break;
                }
                data = LabourConnection.connection().handleRequest("get-employee-by-name", name);
                if (data == null) return error("Unable to find employee for that name.");
                Employee employee = (Employee) data[0];
                Timestamp date;
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    date = new Timestamp(format.parse(dateString).getTime());
                } catch (Exception e) {
                    return error("Error parsing date. Make sure it's in MM/dd/yyyy");
                }
                Calendar startCal = Calendar.getInstance();
                Calendar endCal = Calendar.getInstance();
                Timestamp startStamp = formatTime(date, startTime);
                Timestamp endStamp = formatTime(date, endTime);
                if (startStamp == null || endStamp == null)
                    return error("Start time or end time is invalid. Ensure they are both hh:mm in 24 hour format.");
                startCal.setTimeInMillis(startStamp.getTime());
                endCal.setTimeInMillis(endStamp.getTime());
                if (endCal.get(Calendar.HOUR_OF_DAY) < startCal.get(Calendar.HOUR_OF_DAY)) {
                    endCal.add(Calendar.DATE, 1);
                    endStamp.setTime(endCal.getTimeInMillis());
                }
                int breakLength = 0;
                if (!StringUtils.isNullOrEmpty(breakString)) {
                    if (!NumberUtils.isNumber(breakString)) return error("Break length must be a number.");
                    breakLength = Integer.parseInt(breakString);
                }
                int roleId = RoleNames.getId(roleString);
                if (roleId == -1) return error("Role id is not valid!");
                HourData hourData = new HourData(-1, employee.getId(), date, startStamp, endStamp, roleId, breakLength, employee.getWage(), null, null);
                LabourConnection.connection().handleRequest("add-hours", hourData);
                Tracker.getInstance().getErrorManager().rebuild(Error.ErrorParent.LABOUR);
                if (roleId == RoleNames.getId("server")) {
                    dateString = format.format(date);
                    Error error = Tracker.getInstance().getErrorManager().getError("missing-tips-" + employee.getId() + "-" + dateString);
                    if (error == null) {

                    }
                }
                prop.put("success", true);
                break;
            case "check-role":
                name = request.queryParams("name");
                data = LabourConnection.connection().handleRequest("get-employee-by-name", name);
                if (data == null) return error("");
                employee = (Employee) data[0];
                prop.put("success", true);
                prop.put("id", employee.getDefaultRole());
                prop.put("name", RoleNames.getName(employee.getDefaultRole()));
                break;
            case "edit":
                String idString = request.queryParams("id");
                if (StringUtils.isNullOrEmpty(idString) || !NumberUtils.isNumber(idString))
                    return error("Error getting hours.");
                int id = Integer.parseInt(idString);
                data = LabourConnection.connection().handleRequest("get-hours-by-id", id);
                if (data == null) return error("Error getting hour data.");
                hourData = (HourData) data[0];
                if (request.requestMethod().equals("GET")) {
                    data = LabourConnection.connection().handleRequest("get-employee", hourData.getEmployeeId());
                    if (data == null) return error("Error getting employee.");
                    employee = (Employee) data[0];
                    model.put("hours", hourData);
                    model.put("employee", employee);
                    prop.put("success", true);
                    prop.put("html", WebModule.render("./source/modules/labour/hours/add_hours.jade", model, request, response));
                    break;
                }
                name = request.queryParams("name");
                dateString = request.queryParams("date");
                startTime = request.queryParams("startTime");
                endTime = request.queryParams("endTime");
                breakString = request.queryParams("breakTime");
                roleString = request.queryParams("role");
                if (Utilities.isNullOrEmpty(name, dateString, startTime, endTime, roleString)) {
                    prop.put("success", false);
                    prop.put("error", "All fields other than break time must be filled out.");
                    break;
                }
                data = LabourConnection.connection().handleRequest("get-employee-by-name", name);
                if (data == null) return error("Unable to find employee for that name.");
                employee = (Employee) data[0];
                format = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    date = new Timestamp(format.parse(dateString).getTime());
                } catch (Exception e) {
                    return error("Error parsing date. Make sure it's in MM/dd/yyyy");
                }
                startCal = Calendar.getInstance();
                endCal = Calendar.getInstance();
                startStamp = formatTime(date, startTime);
                endStamp = formatTime(date, endTime);
                if (startStamp == null || endStamp == null)
                    return error("Start time or end time is invalid. Ensure they are both hh:mm in 24 hour format.");
                startCal.setTimeInMillis(startStamp.getTime());
                endCal.setTimeInMillis(endStamp.getTime());
                if (endCal.get(Calendar.HOUR_OF_DAY) < startCal.get(Calendar.HOUR_OF_DAY)) {
                    endCal.add(Calendar.DATE, 1);
                    endStamp.setTime(endCal.getTimeInMillis());
                }
                breakLength = 0;
                if (!StringUtils.isNullOrEmpty(breakString)) {
                    if (!NumberUtils.isNumber(breakString)) return error("Break length must be a number.");
                    breakLength = Integer.parseInt(breakString);
                }
                roleId = RoleNames.getId(roleString);
                if (roleId == -1) return error("Role id is not valid!");
                ArrayList<String> toUpdate = new ArrayList<>();
                ArrayList<Object> values = new ArrayList<>();
                if (!name.equals(hourData.getEmployeeName()))
                    return error("You cannot edit the name. Please delete the entry and create a new one.");
                if (!date.equals(hourData.getDate())) {
                    toUpdate.add("date");
                    values.add(date);
                }
                if (!startStamp.equals(hourData.getStartTime())) {
                    toUpdate.add("start_time");
                    values.add(startStamp);
                }
                if (!endStamp.equals(hourData.getEndTime())) {
                    toUpdate.add("end_time");
                    values.add(endStamp);
                }
                if (roleId != hourData.getRoleId()) {
                    toUpdate.add("role_id");
                    values.add(roleId);
                }
                if (breakLength != hourData.getBreakLength()) {
                    toUpdate.add("break_length");
                    values.add(breakLength);
                }
                if (toUpdate.size() > 0)
                    LabourConnection.connection().handleRequest("update-hours", hourData.getId(), toUpdate.toArray(new String[toUpdate.size()]), values.toArray());
                prop.put("success", true);
                break;
            default:
                return error("Invalid action provided.");
        }
        return Tracker.getGson().toJson(prop);
    }

    public Timestamp formatTime(Date date, String stamp) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int hour = 0;
            int minute = 0;
            String[] spl;
            if (stamp.contains("AM") || stamp.contains("PM")) {
                boolean pm = stamp.contains("PM");
                stamp = stamp.substring(0, stamp.length() - 3);
                if (stamp.contains(":")) spl = stamp.split(":");
                else {
                    spl = new String[stamp.length() > 2 ? 2 : 1];
                    spl[0] = stamp.length() <= 2 ? stamp : stamp.length() == 4 ? stamp.substring(0, 2) : stamp.substring(0, 1);
                    if (stamp.length() > 2) spl[1] = stamp.length() == 4 ? stamp.substring(2) : stamp.substring(1);
                }
                if (pm) {
                    hour = Integer.parseInt(spl[0]);
                    spl[0] = Integer.toString(hour + 12);
                }
            } else if (stamp.contains(":")) spl = stamp.split(":");
            else {
                spl = new String[stamp.length() > 2 ? 2 : 1];
                spl[0] = stamp.length() <= 2 ? stamp : stamp.length() == 4 ? stamp.substring(0, 2) : stamp.substring(0, 1);
                if (stamp.length() > 2) spl[1] = stamp.length() == 4 ? stamp.substring(2) : stamp.substring(1);
            }
            hour = Integer.parseInt(spl[0]);
            if (spl.length > 1) minute = Integer.parseInt(spl[1]);
            if (hour < 0 || hour > 23) return null;
            if (minute < 0 || minute > 59) return null;
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            return new Timestamp(cal.getTimeInMillis());
        } catch (Exception e) {
            return null;
        }
    }
}
