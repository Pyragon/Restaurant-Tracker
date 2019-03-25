package com.smittys.modules.labour;

import com.mysql.jdbc.StringUtils;
import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.Employee;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import com.smittys.utils.RoleNames;
import com.smittys.utils.Utilities;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import static com.smittys.modules.WebModule.error;

public class EmployeesSection implements WebSection {

    @Override
    public String getName() {
        return "employees";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html = null;
                try {
                    html = WebModule.render("./source/modules/labour/employees/employees.jade", model, request, response);
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
                boolean archive = Boolean.parseBoolean(request.queryParams("archive"));
                int page = Integer.parseInt(request.queryParams("page"));
                Object[] data = LabourConnection.connection().handleRequest("get-employees", archive, page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading employees.");
                    break;
                }
                ArrayList<Employee> employees = (ArrayList<Employee>) data[0];
                model.put("employees", employees);
                data = LabourConnection.connection().handleRequest("get-employees-count", archive);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                model.put("archive", archive);
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/labour/employees/employees_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "view":
                if (!StringUtils.isNullOrEmpty(request.queryParams("id"))) {
                    int id = Integer.parseInt(request.queryParams("id"));
                    data = LabourConnection.connection().handleRequest("get-employee", id);
                    if (data == null) return error("Error getting employee.");
                    Employee employee = (Employee) data[0];
                    model.put("editing", employee);
                }
                html = WebModule.render("./source/modules/labour/employees/employee_modal.jade", model, request, response);
                prop.put("success", true);
                prop.put("html", html);
                break;
            case "add":
                String first = request.queryParams("first");
                String last = request.queryParams("last");
                String defRole = request.queryParams("defRole");
                String start = request.queryParams("start");
                String wageString = request.queryParams("wage");
                double wage = 0;
                if (Utilities.isNullOrEmpty(first, last, defRole, start, wageString)) {
                    prop.put("success", false);
                    prop.put("error", "All fields other than end date must be filled out!");
                    break;
                }
                try {
                    wage = Double.parseDouble(wageString);
                    if (wage < 0) {
                        prop.put("success", false);
                        prop.put("error", "Wage cannot be less than $0.");
                        break;
                    }
                } catch (Exception e) {
                    prop.put("success", false);
                    prop.put("error", "Wage must be a number!");
                    break;
                }
                int roleId = RoleNames.getId(defRole);
                String end = request.queryParams("end");
                Timestamp startDate;
                Timestamp endDate;
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    startDate = new Timestamp(format.parse(start).getTime());
                } catch (ParseException e) {
                    prop.put("success", false);
                    prop.put("error", "Error parsing start date.");
                    break;
                }
                if (StringUtils.isNullOrEmpty(end)) endDate = null;
                else {
                    try {
                        endDate = new Timestamp(format.parse(end).getTime());
                    } catch (ParseException e) {
                        prop.put("success", false);
                        prop.put("error", "Error parsing end date.");
                        break;
                    }
                }
                Employee employee = new Employee(-1, first, last, startDate, roleId, null);
                if (endDate != null) employee.setEndDate(endDate);
                employee.setWage(wage);
                LabourConnection.connection().handleRequest("add-employee", employee);
                prop.put("success", true);
                break;
            case "edit":
                int id = Integer.parseInt(request.queryParams("id"));
                data = LabourConnection.connection().handleRequest("get-employee", id);
                if (data == null) return error("Error getting employee.");
                employee = (Employee) data[0];
                if (request.requestMethod().equals("GET")) {
                    model.put("editing", employee);
                    html = WebModule.render("./source/modules/labour/employees/edit_employee.jade", model, request, response);
                    prop.put("success", true);
                    prop.put("html", html);
                    break;
                } else {
                    first = request.queryParams("first");
                    last = request.queryParams("last");
                    defRole = request.queryParams("defRole");
                    start = request.queryParams("start");
                    wageString = request.queryParams("wage");
                    if (Utilities.isNullOrEmpty(first, last, defRole, start, wageString)) {
                        prop.put("success", false);
                        prop.put("error", "All fields other than end date must be filled out!");
                        break;
                    }
                    try {
                        wage = Double.parseDouble(wageString);
                        if (wage < 0) {
                            prop.put("success", false);
                            prop.put("error", "Wage cannot be less than $0.");
                            break;
                        }
                    } catch (Exception e) {
                        prop.put("success", false);
                        prop.put("error", "Wage must be a number!");
                        break;
                    }
                    roleId = RoleNames.getId(defRole);
                    if (roleId == -1) return error("Role id is not valid!");
                    end = request.queryParams("end");
                    format = new SimpleDateFormat("MM/dd/yyyy");
                    try {
                        startDate = new Timestamp(format.parse(start).getTime());
                    } catch (ParseException e) {
                        prop.put("success", false);
                        prop.put("error", "Error parsing start date.");
                        break;
                    }
                    if (StringUtils.isNullOrEmpty(end)) endDate = null;
                    else {
                        try {
                            endDate = new Timestamp(format.parse(end).getTime());
                        } catch (ParseException e) {
                            prop.put("success", false);
                            prop.put("error", "Error parsing end date.");
                            break;
                        }
                    }
                    ArrayList<String> toUpdate = new ArrayList<>();
                    ArrayList<Object> values = new ArrayList<>();
                    if (!first.equals(employee.getFirstName())) {
                        toUpdate.add("first_name");
                        values.add(first);
                    }
                    if (!last.equals(employee.getLastName())) {
                        toUpdate.add("last_name");
                        values.add(last);
                    }
                    if (roleId != employee.getDefaultRole()) {
                        toUpdate.add("default_role");
                        values.add(roleId);
                    }
                    if (startDate != employee.getStartDate()) {
                        toUpdate.add("start_date");
                        values.add(startDate);
                    }
                    if (endDate != employee.getEndDate()) {
                        toUpdate.add("end_date");
                        values.add(endDate == null ? "NULL" : endDate);
                        if (endDate == null) values.add(Types.TIMESTAMP);
                    }
                    if (wage != employee.getWage()) {
                        toUpdate.add("wage");
                        values.add(wage);
                    }
                    if (toUpdate.size() > 0)
                        LabourConnection.connection().handleRequest("update-employee", employee.getId(), toUpdate.toArray(new String[toUpdate.size()]), values.toArray());
                    prop.put("success", true);
                }
                break;
            default:
                prop.put("success", false);
                prop.put("error", "Invalid action specified.");
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
