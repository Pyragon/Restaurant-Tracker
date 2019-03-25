package com.smittys.managers.cron.impl;

import java.util.Date;

import com.smittys.entities.CronJob;
import com.smittys.managers.EmailManager;

public class EmailJob implements CronJob {

    private int lastMinuteCheck;

    @Override
    public boolean checkTime(Date date, int dayOfWeek, int hour, int minute, int second) {
        if (minute % 5 != 0)
            return false;
        if (minute == lastMinuteCheck)
            return false;
        lastMinuteCheck = minute;
        return true;
    }

    @Override
    public void run() {
        EmailManager.checkEmails();
    }

}