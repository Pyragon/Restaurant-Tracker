package com.cryo.managers;

import com.cryo.Tracker;

import java.util.Timer;
import java.util.TimerTask;

public class TimerManager {

    private Timer timer;

    public TimerManager() {
        timer = new Timer();
    }

    public void run() {
        timer.scheduleAtFixedRate(new Task(), 0, 1000);
    }

    class Task extends TimerTask {

        @Override
        public void run() {
            Tracker.getInstance().getCronJobManager().run();
        }
    }

}
