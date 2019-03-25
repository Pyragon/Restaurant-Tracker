package com.smittys.entities;

import com.smittys.db.impl.LabourConnection;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@Data
@RequiredArgsConstructor
public class HourData {

    private final int id, employeeId;
    private final Timestamp date;
    private final Timestamp startTime, endTime;
    private final int roleId, breakLength;
    private final double wage;
    private final Timestamp added, lastUpdated;

    public String getTotalHours() {
        long millis = endTime.getTime() - startTime.getTime();
        millis -= breakLength * 60 * 1000;
        int seconds = (int) millis / 1000;
        int hours = seconds / 3600;
        float minutes = (seconds % 3600) / 60;
        minutes = 15 * (Math.round(minutes / 15));
        minutes = (minutes / 60) * 100;
        if (minutes == 50) minutes = 5;
        return hours + "." + (int) minutes;
    }

    public double getLength() {
        return Double.parseDouble(getTotalHours());
    }

    public String getEmployeeName() {
        Object[] data = LabourConnection.connection().handleRequest("get-employee", employeeId);
        if (data == null) return "Error";
        return ((Employee) data[0]).getFullName();
    }

    public Object[] data() {
        return new Object[]{"DEFAULT", employeeId, date, startTime, endTime, roleId, breakLength, wage, "DEFAULT", "DEFAULT"};
    }

}
