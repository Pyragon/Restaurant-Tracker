package com.cryo.modules.search.impl;

import com.cryo.entities.Filter;

public class HasUnitsFilter extends Filter {

    private boolean value;

    public HasUnitsFilter() {
        super("has_units");
    }

    @Override
    public String getFilter(String mod) {
        if (this.value)
            return "(units IS NOT NULL AND units != \"\")";
        return "(units IS NULL OR units = \"\")";
    }

    @Override
    public boolean setValue(String mod, String value) {
        Object val = parseBoolean(value);
        if (val == null) return false;
        this.value = (boolean) val;
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("invoice-items");
    }
}
