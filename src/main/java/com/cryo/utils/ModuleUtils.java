package com.cryo.utils;

import com.cryo.entities.User;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class ModuleUtils {

    public static String[] RANDOM_GREETINGS = {
            "Good %time%, %first_name%!",
            "Hello, %first_name%!",
            "Fancy seeing you here, %first_name%.",
            "G'day, %first_name%.",
            "Wotcher, guv'nor.",
            "Is it really %time% already, %first_name%?"
    };

    public String getRandomGreeting(User user) {
        String greeting = RANDOM_GREETINGS[new Random().nextInt(RANDOM_GREETINGS.length)];
        greeting = greeting.replace("%first_name%", user.getFirstName());
        greeting = greeting.replace("%last_name%", user.getLastName());
        if (greeting.contains("%time%")) {
            String replace = "";
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour < 12) replace = "morning";
            else if (hour < 17) replace = "afternoon";
            else replace = "evening";
            greeting = greeting.replace("%time%", replace);
        }
        return greeting;
    }
}
