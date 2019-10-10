package com.cryo.modules.search.impl;

import com.cryo.entities.Filter;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFilter extends Filter {

    private Object under;

    public DateFilter() {
        super("date");
    }

    @Override
    public String getFilter(String mod) {
        return "date " + (under == null ? "=" : ((boolean) under) ? "<" : ">") + " ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        if (value.contains("<")) {
            under = true;
            value = value.replace("<", "");
        } else if (value.contains(">")) {
            under = false;
            value = value.replace(">", "");
        }
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        Date date;
        try {
            date = format.parse(value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        this.value = new Timestamp(date.getTime());
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("hours") || mod.equals("sales");
    }
}
