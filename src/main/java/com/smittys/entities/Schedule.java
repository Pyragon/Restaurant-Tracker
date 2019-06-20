package com.smittys.entities;

import com.smittys.db.impl.LabourConnection;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;

@Data
public class Schedule {

    private final int id;
    private final boolean boh;
    private final Timestamp startDate;
    private final Timestamp added;
    private final Timestamp updated;

    public Object[] data() {
        return new Object[] { "DEFAULT", boh, startDate, "DEFAULT", "DEFAULT" };
    }

    public Object getHours() {
        ArrayList<ScheduleTime> times = LabourConnection.connection().selectList("schedule_times", "schedule_id=?", ScheduleTime.class, id);
        if(times == null) return 0;
        double time =  times.stream().mapToDouble(t -> t.getLength()).sum();
        if(Math.ceil(time) == time) return (int) time;
        return time;
    }
}
