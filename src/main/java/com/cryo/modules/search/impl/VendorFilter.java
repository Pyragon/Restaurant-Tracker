package com.cryo.modules.search.impl;

import com.cryo.entities.Filter;

public class VendorFilter extends Filter {

    public VendorFilter() {
        super("vendor");
    }

    @Override
    public String getFilter(String mod) {
        return "vendor LIKE ?";
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
