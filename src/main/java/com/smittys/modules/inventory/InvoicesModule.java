package com.smittys.modules.inventory;

import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.Invoice;
import com.smittys.entities.WebSection;
import com.smittys.modules.WebModule;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import static com.smittys.modules.WebModule.error;

public class InvoicesModule implements WebSection {

    @Override
    public String getName() {
        return "invoices";
    }

    @Override
    public String decode(String action, Request request, Response response) {
        HashMap<String, Object> model = new HashMap<>();
        Properties prop = new Properties();
        switch (action) {
            case "load":
                String html;
                try {
                    html = WebModule.render("./source/modules/inventory/invoices/invoices.jade", model, request, response);
                } catch (Exception e) {
                    e.printStackTrace();
                    prop.put("success", false);
                    prop.put("error", e.getMessage());
                    break;
                }
                if (html == null) {
                    prop.put("success", false);
                    prop.put("error", "Unable to load section.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", html);
                break;
            case "load-list":
                int page = Integer.parseInt(request.queryParams("page"));
                Object[] data = InventoryConnection.connection().handleRequest("get-invoices", page);
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error loading invoices.");
                    break;
                }
                ArrayList<Invoice> invoices = (ArrayList<Invoice>) data[0];
                model.put("invoices", invoices);
                data = InventoryConnection.connection().handleRequest("get-invoices-page-count");
                if (data == null) {
                    prop.put("success", false);
                    prop.put("error", "Error getting page total.");
                    break;
                }
                prop.put("success", true);
                prop.put("html", WebModule.render("./source/modules/inventory/invoices/invoices_list.jade", model, request, response));
                prop.put("pageTotal", data[0]);
                break;
            case "view-invoice":
                String idString = request.queryParams("id");
                int id;
                try {
                    id = Integer.parseInt(idString);
                } catch(Exception e) {
                    return error("Error parsing id.");
                }
                Invoice invoice = InventoryConnection.connection().selectClass("invoices", "id=?", Invoice.class, id);
                if(invoice == null) return error("Unable to find invoice with that ID.");
                model.put("invoice", invoice);
                prop.put("html", WebModule.render("./source/modules/inventory/invoices/view_invoice.jade", model, request, response));
                prop.put("success", true);
                break;
        }
        return Tracker.getGson().toJson(prop);
    }
}
