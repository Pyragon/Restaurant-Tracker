package com.cryo.entities;

import com.cryo.db.impl.LabourConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class ScheduleTime extends MySQLDao {

    @MySQLDefault
    private final int id;
    @MySQLRead
    private int scheduleId;
    private final int employeeId;
    private final int day;
    private final Timestamp startTime;
    private final Timestamp endTime;
    private final boolean close;
    @MySQLDefault
    private final Timestamp added;
    @MySQLDefault
    private final Timestamp updated;

    public String getTotalHours() {
        Timestamp endTime;
        if(this.endTime == null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(startTime.getTime()));
            cal.set(Calendar.HOUR_OF_DAY, 16);
            endTime = new Timestamp(cal.getTimeInMillis());
        } else endTime = this.endTime;
        long millis = endTime.getTime() - startTime.getTime();
        int seconds = (int) millis / 1000;
        int hours = seconds / 3600;
        float minutes = (seconds % 3600) / 60;
        minutes = 15 * (Math.round(minutes / 15));
        minutes = (minutes / 60) * 100;
        if (minutes == 50) minutes = 5;
        return hours + "." + (int) minutes;
    }

    public String getEmployeeName() {
        Employee employee = LabourConnection.connection().selectClass("employees", "id=?", Employee.class, employeeId);
        if(employee == null) return "Not found";
        return employee.getFirstName();
    }

    public double getLength() {
        return Double.parseDouble(getTotalHours());
    }
}
