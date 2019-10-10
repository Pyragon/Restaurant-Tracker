package com.cryo.modules.search.impl;

import com.cryo.entities.Filter;

public class FullNameFilter extends Filter {

    public FullNameFilter() {
        super("full_name");
    }

    @Override
    public String getFilter(String mod) {
        return "first_name LIKE ? AND last_name LIKE ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        String[] split = value.split(" ");
        this.value = new Object[split.length];
        System.arraycopy(split, 0, this.value, 0, split.length);
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("employees") || mod.equals("hours");
    }
}
