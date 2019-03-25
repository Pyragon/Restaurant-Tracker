package com.smittys.modules.search.impl;

import com.smittys.entities.Filter;

public class OvertimeFilter extends Filter {

    private boolean overtime;

    public OvertimeFilter() {
        super("overtime");
    }

    @Override
    public String getFilter(String mod) {
        return "TIMESTAMPDIFF(MINUTE, start_time, end_time) " + (overtime ? ">" : "<") + " 480";
    }

    @Override
    public boolean setValue(String mod, String value) {
        try {
            Object parsed = parseBoolean(value);
            if (parsed == null) return false;
            overtime = (boolean) parsed;
            System.out.println(overtime);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("hours");
    }
}
