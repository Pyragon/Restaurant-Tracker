package com.smittys.entities;

import com.smittys.db.impl.LabourConnection;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EnterHoursError extends Error {

    public EnterHoursError(String key, String message) {
        super(-1, key, message, message, ErrorParent.LABOUR, true);
        setLink("/labour/hours?day=" + key.replace("missing-hours-", ""));
    }

    @Override
    public boolean recheck() {
        System.out.println("Rechecking");
        Object[] data = LabourConnection.connection().handleRequest("get-hours-for-day", getTimestamp());
        if (data != null) {
            ArrayList<HourData> hData = (ArrayList<HourData>) data[0];
            if (hData.size() > 0) return false;
        }
        return true;
    }

    public Timestamp getTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        try {
            return new Timestamp(format.parse(key.replace("missing-hours-", "")).getTime());
        } catch (Exception e) {
            return new Timestamp(new Date().getTime());
        }
    }
}
