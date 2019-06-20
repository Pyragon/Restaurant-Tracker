package com.smittys.entities;

import lombok.Data;

import java.sql.Time;
import java.sql.Timestamp;

@Data
public class ScheduleTime extends MySQLDao {

    @MySQLDefault
    private final int id;
    private final int scheduleId;
    private final int employeeId;
    private final int day;
    private final Timestamp startTime;
    private final Timestamp endTime;
    @MySQLDefault
    private final Timestamp added;
    @MySQLDefault
    private final Timestamp updated;

    public String getTotalHours() {
        long millis = endTime.getTime() - startTime.getTime();
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
}
