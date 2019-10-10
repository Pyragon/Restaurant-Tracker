package com.cryo.modules.search.impl;

import com.cryo.db.impl.LabourConnection;
import com.cryo.entities.Employee;
import com.cryo.entities.Filter;

import java.util.ArrayList;

public class FirstNameFilter extends Filter {

    public FirstNameFilter() {
        super("first_name");
    }

    @Override
    public String getFilter(String mod) {
        return mod.equals("hours") ? "employee_id IN (?)" : "first_name LIKE ?";
    }

    @Override
    public boolean setValue(String mod, String value) {
        value = value.toLowerCase();
        if (mod.equals("hours")) {
            Object[] data = LabourConnection.connection().handleRequest("get-employee-by-name", true, value);
            if (data == null) {
                this.value = "";
                return true;
            }
            this.value = "";
            ArrayList<Employee> list = (ArrayList<Employee>) data[0];
            for (int i = 0; i < list.size(); i++) {
                this.value += Integer.toString(list.get(i).getId());
                if (i != list.size() - 1) this.value += ", ";
            }
            return true;
        }
        this.value = "%" + value + "%";
        return true;
    }

    @Override
    public boolean appliesTo(String mod, boolean archived) {
        return mod.equals("employees") || mod.equals("hours");
    }
}
