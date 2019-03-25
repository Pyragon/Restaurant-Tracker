package com.smittys.modules.search.impl;

import com.smittys.entities.Filter;

public class HoursWorkedFilter extends Filter {

    private String formula;

    public HoursWorkedFilter() {
        super("hours_worked");
    }

    @Override
    public String getFilter(String mod) {
        return "TIMESTAMPDIFF(HOUR, start_time, end_time) " + formula;
    }

    @Override
    public boolean setValue(String mod, String value) {
        String[] split = value.split(" ");
        String equation;
        String lengthString;
        if (split.length < 2) {
            equation = value.substring(0, 1);
            lengthString = value.substring(1);
        } else {
            equation = split[0];
            lengthString = split[1];
        }
        int length;
        try {
            length = Integer.parseInt(lengthString);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        switch (equation) {
            case "<":
                formula = "< " + length;
                break;
            case ">":
                formula = "> " + length;
                break;
            case "<=":
                formula = "<= " + length;
                break;
            case ">=":
                formula = ">= " + length;
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("hours");
    }
}
