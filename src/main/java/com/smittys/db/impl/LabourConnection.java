package com.smittys.db.impl;

import com.smittys.Tracker;
import com.smittys.db.DBConnectionManager;
import com.smittys.db.DatabaseConnection;
import com.smittys.entities.Employee;
import com.smittys.entities.HourData;
import com.smittys.entities.SQLQuery;
import com.smittys.utils.Utilities;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class LabourConnection extends DatabaseConnection {

    public LabourConnection() {
        super("smittys_global");
    }

    public static LabourConnection connection() {
        return (LabourConnection) Tracker.getConnectionManager().getConnection(DBConnectionManager.Connection.LABOUR);
    }

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.startConnections();
        ResultSet set = connection().callProcedure("TESTPROC");
        try {
            while (set.next()) {
                System.out.println(connection().getString(set, "id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object[] handleRequest(Object... data) {
        String opcode = (String) data[0];
        switch (opcode) {
            case "set-wage-for-hours":
                data = handleRequest("get-all-hours");
                if (data == null)
                    return null;
                ArrayList<HourData> hours = (ArrayList<HourData>) data[0];
                hours.forEach(h -> {
                    Object[] emp = handleRequest("get-employee", h.getEmployeeId());
                    if (emp == null)
                        return;
                    Employee employee = (Employee) emp[0];
                    set("hours", "wage=?", "id=?", employee.getWage(), h.getId());
                });
                break;
            case "get-employee":
                return select("employees", "id=?", GET_EMPLOYEE, (int) data[1]);
            case "get-employee-by-name":
                if (data.length > 2) {
                    boolean first = (boolean) data[1];
                    String name = (String) data[2];
                    return select("employees", (first ? "first" : "last") + "_name LIKE ?", GET_EMPLOYEES, name);
                } else {
                    String valueString = (String) data[1];
                    if (valueString.contains(" ")) {
                        String[] values = ((String) data[1]).split(" ");
                        return select("employees", "first_name LIKE ?" + (values.length > 1 ? "AND last_name LIKE ?" : ""),
                                "LIMIT 1", GET_EMPLOYEE, (Object[]) values);
                    } else
                        return select("employees", "(first_name LIKE ? OR last_name LIKE ?)", GET_EMPLOYEE, valueString, valueString);
                }
            case "add-employee":
                Employee employee = (Employee) data[1];
                insert("employees", employee.getData());
                return null;
            case "update-employee":
                updateExisting("employees", (int) data[1], (String[]) data[2], (Object[]) data[3]);
                break;
            case "get-employees":
                boolean archive = (boolean) data[1];
                int page = (int) data[2];
                if (page <= 0)
                    page = 1;
                int offset = (page - 1) * 10;
                String query = "";
                if (archive)
                    query = "end_date IS NOT NULL ";
                else
                    query = "end_date IS NULL ";
                query += "LIMIT " + offset + ",10";
                return select("employees", query, GET_EMPLOYEES);
            case "get-employees-count":
                archive = (boolean) data[1];
                query = "end_date IS " + (archive ? "NOT NULL " : "NULL");
                return new Object[]{(int) Utilities.roundUp(selectCount("employees", query), 10)};
            case "search":
                Properties queryValues = (Properties) data[1];
                page = (int) data[2];
                archive = (boolean) data[3];
                HashMap<String, String> params = (HashMap<String, String>) data[4];
                query = queryValues.getProperty("query");
                Object[] values = (Object[]) queryValues.get("values");
                String module = (String) data[5];
                if (page <= 0)
                    page = 1;
                offset = (page - 1) * 10;
                if (module.equals("employees"))
                    query += "AND end_date IS " + (archive ? "NOT NULL " : "NULL");
                else if (module.equals("hours") || module.equals("sales"))
                    query += " ORDER BY date DESC";
                query += " LIMIT " + offset + ",10";
                SQLQuery queryResponse = null;
                switch (module) {
                    case "hours":
                        queryResponse = GET_HOURS;
                        break;
                    case "employees":
                        queryResponse = GET_EMPLOYEES;
                        break;
                    // case "sales":
                    // queryResponse = GET_SALES;
                    // break;
                }
                return select(module, query, queryResponse, values);
            case "search-results":
                queryValues = (Properties) data[1];
                archive = (boolean) data[2];
                module = (String) data[4];
                query = queryValues.getProperty("query");
                values = (Object[]) queryValues.get("values");
                if (module.equals("employees"))
                    query += " AND end_date IS " + (archive ? "NOT NULL " : "NULL");
                return new Object[]{(int) Utilities.roundUp(selectCount(module, query, values), 10)};
            case "get-all-sales-days":
                page = (int) data[1];
                if (page <= 0)
                    page = 1;
                offset = (page - 1) * 10;
                return selectDistinct("sales", "date", "date DESC LIMIT " + offset + ",10", GET_SALES_DAYS);
            case "get-all-sales-days-count":
                return new Object[]{Utilities.roundUp(selectDistinctCount("sales", "date"), 10)};
            case "add-hours":
                insert("hours", ((HourData) data[1]).data());
                break;
            case "delete-hours":
                delete("hours", "id=?", data[1]);
                break;
            case "update-hours":
                updateExisting("hours", (int) data[1], (String[]) data[2], (Object[]) data[3]);
                break;
            case "get-hours-for-day":
                return select("hours", "date=?", GET_HOURS, data[1]);
            case "get-hours-by-date-id":
                return select("hours", "id=? AND date=?", GET_HOUR_DATA, data[1], data[2]);
            case "get-hours-by-id":
                return select("hours", "id=?", GET_HOUR_DATA, data[1]);
            case "get-hours-for-year":
                return select("hours", "YEAR(date) = YEAR(CURDATE())", GET_HOURS);
            case "get-all-hours":
                return select("hours", null, "ORDER BY updated DESC", GET_HOURS);
            case "get-hours":
                page = (int) data[1];
                if (page <= 0)
                    page = 1;
                offset = (page - 1) * 10;
                return select("hours ORDER BY updated DESC LIMIT " + offset + ",10", null, GET_HOURS);
            case "get-hours-count":
                return new Object[]{Utilities.roundUp(selectCount("hours", null), 10)};
            case "load-roles":
                return select("roles", null, GET_ROLES);
        }
        return null;
    }

    private SQLQuery GET_SALES_DAYS = (set) -> {
        ArrayList<Timestamp> stamps = new ArrayList<>();
        if (wasNull(set))
            return new Object[]{stamps};
        while (next(set)) {
            Timestamp stamp = getTimestamp(set, "date");
            stamps.add(stamp);
        }
        return new Object[]{stamps};
    };

    private SQLQuery GET_ROLES = (set) -> {
        if (wasNull(set))
            return null;
        HashMap<Integer, String> names = new HashMap<>();
        while (next(set)) {
            int id = getInt(set, "id");
            String name = getString(set, "name");
            names.put(id, name);
        }
        return new Object[]{names};
    };

    private SQLQuery GET_EMPLOYEE = (set) -> {
        if (empty(set))
            return null;
        return new Object[]{getEmployee(set)};
    };

    private SQLQuery GET_EMPLOYEES = (set) -> {
        ArrayList<Employee> employees = new ArrayList<>();
        if (wasNull(set))
            return new Object[]{employees};
        while (next(set)) {
            employees.add(getEmployee(set));
        }
        return new Object[]{employees};
    };

    private SQLQuery GET_HOUR_DATA = (set) -> {
        if (empty(set))
            return null;
        return new Object[]{getHourData(set)};
    };

    private SQLQuery GET_HOURS = (set) -> {
        ArrayList<HourData> hours = new ArrayList<>();
        if (wasNull(set))
            return new Object[]{hours};
        while (next(set))
            hours.add(getHourData(set));
        return new Object[]{hours};
    };

    private HourData getHourData(ResultSet set) {
        int id = getInt(set, "id");
        int employeeId = getInt(set, "employee_id");
        Timestamp date = getTimestamp(set, "date");
        Timestamp startTime = getTimestamp(set, "start_time");
        Timestamp endTime = getTimestamp(set, "end_time");
        int roleId = getInt(set, "role_id");
        int breakLength = getInt(set, "break_length");
        double wage = getDouble(set, "wage");
        Timestamp added = getTimestamp(set, "added");
        Timestamp updated = getTimestamp(set, "updated");
        return new HourData(id, employeeId, date, startTime, endTime, roleId, breakLength, wage, added, updated);
    }

    private Employee getEmployee(ResultSet set) {
        int id = getInt(set, "id");
        String firstName = getString(set, "first_name");
        String lastName = getString(set, "last_name");
        Timestamp startDate = getTimestamp(set, "start_date");
        int defaultRole = getInt(set, "default_role");
        double wage = getDouble(set, "wage");
        Timestamp lastUpdated = getTimestamp(set, "last_updated");
        Employee employee = new Employee(id, firstName, lastName, startDate, defaultRole, lastUpdated);
        employee.setWage(wage);
        if (containsRow(set, "end_date")) {
            Timestamp endDate = getTimestamp(set, "end_date");
            if (endDate != null)
                employee.setEndDate(endDate);
        }
        return employee;
    }
}
