package com.smittys.entities;

import com.smittys.db.impl.LabourConnection;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;

@Data
public class Schedule {

    private final int id;
    @MySQLRead("is_boh")
    private final boolean boh;
    private final Date startDate;
    private final Timestamp added;
    private final Timestamp updated;

    public Object[] data() {
        return new Object[] { "DEFAULT", boh, startDate, "DEFAULT", "DEFAULT" };
    }

    public ArrayList<ScheduleTime> getTimes(int day) {
        return LabourConnection.connection().selectList("schedule_times", "schedule_id=? AND day=?", ScheduleTime.class, id, day);
    }

    public Object getHours() {
        ArrayList<ScheduleTime> times = LabourConnection.connection().selectList("schedule_times", "schedule_id=?", ScheduleTime.class, id);
        if(times == null) return 0;
        double time =  times.stream().mapToDouble(t -> t.getLength()).sum();
        if(Math.ceil(time) == time) return (int) time;
        return time;
    }
}
