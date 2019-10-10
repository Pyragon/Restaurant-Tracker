package com.cryo.entities;

import java.util.Date;

public interface CronJob {

    boolean checkTime(Date date, int dayOfWeek, int hour, int minute, int second);

    void run();
}
