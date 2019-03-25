package com.smittys.managers.cron;

import com.smittys.entities.CronJob;
import com.smittys.utils.Utilities;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

@Data
public class CronJobManager {

    private ArrayList<CronJob> cronJobs;
    private HashMap<CronJob, RunData> lastRunData;

    public CronJobManager() {
        cronJobs = new ArrayList<>();
        lastRunData = new HashMap<>();
    }

    public void init() {
        try {
            for (Class<?> c : Utilities.getClasses("com.smittys.managers.cron.impl")) {
                if (c.isAnonymousClass()) continue;
                Object obj = c.newInstance();
                if (obj == null || !(obj instanceof CronJob)) continue;
                CronJob item = (CronJob) obj;
                cronJobs.add(item);
            }
        } catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to load cron job.", e);
        }
    }

    public void run() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        cronJobs.stream()
                .filter(job -> job.checkTime(date, dayOfWeek, hour, minute, second))
                .filter(job -> {
                    if (!lastRunData.containsKey(job)) return true;
                    RunData data = lastRunData.get(job);
                    return hour != data.hour || minute != data.minute || second != data.second || dayOfWeek != data.dayOfWeek;
                })
                .forEach(job -> {
                    lastRunData.put(job, new RunData(hour, minute, second, dayOfWeek));
                    job.run();
                });
    }

    @RequiredArgsConstructor
    @Data
    class RunData {
        private final int hour, minute, second, dayOfWeek;
    }
}
