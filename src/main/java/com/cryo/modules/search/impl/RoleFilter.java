package com.cryo.modules.search.impl;

import com.cryo.entities.Filter;
import com.cryo.utils.RoleNames;

public class RoleFilter extends Filter {

    public RoleFilter() {
        super("role_name");
    }

    @Override
    public String getFilter(String mod) {
        return "role_id = ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        int roleId = RoleNames.getId(value);
        if (roleId == -1) return false;
        this.value = roleId;
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("hours") || mod.equals("employees");
    }

}