package com.cryo.entities;

import com.google.gson.reflect.TypeToken;
import com.cryo.Tracker;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.managers.ErrorManager;
import com.cryo.modules.WebModule;
import com.cryo.modules.inventory.InvoiceItemsModule;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class UnitError extends Error {

    private String unit;
    private int id;

    private String name;

    public UnitError(String key, String message, String unit, int id) {
        this(key, message, message);
        this.unit = unit;
        this.id = id;
        this.parameters = new Properties();
        this.parameters.put("id", id);
        this.parameters.put("unit", unit);
        Object[] data = InventoryConnection.connection().handleRequest("get-item-by-id", id);
        if (data == null) return;
        name = ((ItemData) data[0]).getItemName();
    }

    public UnitError(String key, String shortMessage, String longMessage) {
        super(-1, key, shortMessage, longMessage, ErrorParent.INVENTORY, true);
    }

    public boolean hasLeftClick() {
        return true;
    }

    public boolean refreshAfterClick() {
        return true;
    }

    public boolean opensModal() {
        return true;
    }

    @Override
    public Properties getModalData(Request request, Response response) {
        Properties prop = new Properties();
        prop.put("success", true);
        prop.put("html", loadModalTemplate(request, response));
        prop.put("buttons", getButtons());
        prop.put("title", "Missing unit " + unit + " for " + name);
        return prop;
    }

    public String loadModalTemplate(Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        model.put("unitName", unit);
        return WebModule.render("./source/modules/inventory/items/invoice/missing_unit.jade", model, request, response);
    }

    public ArrayList<Properties> getButtons() {
        ArrayList<Properties> props = new ArrayList<>();
        Properties submit = new Properties();
        submit.put("text", "Submit");
        submit.put("class", "btn btn-primary submit-btn");
        submit.put("key", key);
        submit.put("getData", true);
        props.add(submit);
        Properties close = new Properties();
        close.put("text", "Cancel");
        close.put("class", "btn btn-danger");
        close.put("key", key);
        close.put("closeOnClick", true);
        props.add(close);
        return props;
    }

    @Override
    public boolean recheck() {
        Object[] data = InventoryConnection.connection().handleRequest("get-item-by-id", id);
        if (data == null) return true;
        ItemData item = (ItemData) data[0];
        return !item.getUnitProps().containsKey(unit);
    }

    @Override
    public String buttonClick(String button, Properties data) {
        Properties prop = new Properties();
        prop.put("success", false);
        prop.put("error", "Read server printout.");
        TypeToken<?> token = new TypeToken<List<Properties>>() {
        };
        List<Properties> list = Tracker.getGson().fromJson(data.getProperty("units"), token.getType());
        Properties values = list.get(0);
        String unit = values.getProperty("key");
        String value = values.getProperty("value");
        return InvoiceItemsModule.addUnit(id, unit, value);
    }

    @Override
    public String click() {
        ErrorManager manager = Tracker.getInstance().getErrorManager();
        manager.recheck(key);
        Properties prop = new Properties();
        ArrayList<Properties> retList = new ArrayList<>();
        List<Error> errorList = manager.getErrors(parent, 5);
        for (Error error : errorList) {
            Properties errorProp = new Properties();
            errorProp.put("key", error.getKey());
            errorProp.put("shortMessage", error.getShortMessage());
            errorProp.put("type", error.getType());
            errorProp.put("hasLeftClick", error.hasLeftClick());
            errorProp.put("refreshAfterClick", error.refreshAfterClick());
            errorProp.put("opensModal", error.opensModal());
            if (error.getLink() != null) errorProp.put("link", error.getLink());
            retList.add(errorProp);
        }
        prop.put("success", true);
        prop.put("errors", retList);
        return Tracker.getGson().toJson(prop);
    }
}
