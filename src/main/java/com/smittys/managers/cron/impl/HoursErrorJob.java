package com.smittys.managers.cron.impl;

import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.CronJob;
import com.smittys.entities.EnterHoursError;
import com.smittys.entities.HourData;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class HoursErrorJob implements CronJob {

    private String lastRun;
    private boolean run;

    @Override
    public boolean checkTime(Date date, int dayOfWeek, int hour, int minute, int second) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        String formatted = format.format(date);
        if (lastRun != null && lastRun.equals(formatted)) return false;
        if (hour != 11) return false;
        lastRun = formatted;
        return true;
    }

    @Override
    public void run() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Object[] data = LabourConnection.connection().handleRequest("get-hours-for-day", new Timestamp(cal.getTimeInMillis()));
        if (data != null) {
            ArrayList<HourData> hData = (ArrayList<HourData>) data[0];
            if (hData.size() > 0) return;
        }
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        String date = format.format(cal.getTime());
        format = new SimpleDateFormat("EEEEE MMMMM dd, yyyy");
        String formatted = format.format(cal.getTime());
        EnterHoursError error = new EnterHoursError("missing-hours-" + date, "Missing hours for " + formatted);
        Tracker.getInstance().getErrorManager().add(error);
    }
}
