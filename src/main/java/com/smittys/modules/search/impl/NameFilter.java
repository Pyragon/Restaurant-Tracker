package com.smittys.modules.search.impl;

import com.smittys.entities.Filter;

public class NameFilter extends Filter {

    public NameFilter() {
        super("name");
    }

    @Override
    public String getFilter(String mod) {
        return mod.equals("invoice-items") ? "(item_name LIKE ? OR display_name LIKE ?)" : "name LIKE ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        value = value.toLowerCase();
        this.value = new Object[mod.equals("invoice-items") ? 2 : 1];
        ((Object[]) this.value)[0] = "%" + value + "%";
        if (mod.equals("invoice-items")) ((Object[]) this.value)[1] = "%" + value + "%";
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("recipe-items") || mod.equals("invoice-items");
    }
}
