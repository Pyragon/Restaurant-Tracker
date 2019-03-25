package com.smittys.modules.search.impl;

import com.smittys.entities.Filter;

public class DisplayNameFilter extends Filter {

    public DisplayNameFilter() {
        super("display_name");
    }

    @Override
    public String getFilter(String mod) {
        return "display_name LIKE ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        value = value.toLowerCase();
        this.value = "%" + value + "%";
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("invoice-items");
    }
}
