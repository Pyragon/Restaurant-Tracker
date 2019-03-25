package com.smittys.modules.search.impl;

import com.smittys.entities.Filter;

public class BillName extends Filter {

    public BillName() {
        super("bill_name");
    }

    @Override
    public String getFilter(String mod) {
        return "bill_name LIKE ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        this.value = "%" + value.toLowerCase() + "%";
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("recipe-items");
    }
}
