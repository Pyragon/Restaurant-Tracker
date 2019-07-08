package com.smittys.entities;

import com.smittys.db.impl.LabourConnection;
import com.smittys.utils.BuzzSheetCreator;
import com.smittys.utils.Utilities;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

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

    public void createBuzzSheets() {
        for(int i = 0; i < 7; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.add(Calendar.DAY_OF_WEEK, i);
            Date date = new Date(cal.getTimeInMillis());

            Properties prop = new Properties();
            prop.put("date", date);
            prop.put("sandwich", "");
            prop.put("soup", "");
            prop.put("veg", "");

            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

            ArrayList<ScheduleTime> times = Utilities.getTimes(this, i);
            prop.put("times", times);
            BuzzSheetCreator.createBuzzSheet(prop);
        }
    }
}
