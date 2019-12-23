package com.cryo.db.impl;

import com.cryo.Tracker;
import com.cryo.db.DBConnectionManager;
import com.cryo.db.DatabaseConnection;
import com.cryo.entities.*;
import com.cryo.utils.Utilities;
import lombok.Cleanup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;

public class InventoryConnection extends DatabaseConnection {

    public InventoryConnection() {
        super("citycenter_global");
    }

    public static InventoryConnection connection() {
        return (InventoryConnection) Tracker.getConnectionManager().getConnection(DBConnectionManager.Connection.INVENTORY);
    }

    public static void dumpItemCodes() {
        try {
            @Cleanup BufferedWriter writer = new BufferedWriter(new FileWriter(new File("item-codes.txt")));
            Object[] data = connection().handleRequest("get-all-items");
            if (data == null) return;
            ArrayList<ItemData> items = (ArrayList<ItemData>) data[0];
            for (ItemData item : items) {
                writer.append(item.getItemCode() + ": " + item.getItemName());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object[] handleRequest(Object... data) {
        String opcode = (String) data[0];
        switch (opcode) {
            case "fix-pack-units":
                data = handleRequest("get-all-items");
                ArrayList<ItemData> items = (ArrayList<ItemData>) data[0];
                items.forEach(item -> {
                    double packSize = item.getPackSize();
                    String packUnit = item.getPackUnit();
                    if (packSize % 1 == 0) {
                        if (packUnit.contains(Integer.toString((int) packSize))) {
                            packUnit = packUnit.replace(Integer.toString((int) packSize), "");
                            packUnit = packUnit.trim();
                            set("item_data", "pack_unit=?", "id=?", packUnit, item.getId());
                        }
                    }
                });
                break;
            case "search":
                Properties queryValues = (Properties) data[1];
                int page = (int) data[2];
                String module = (String) data[5];
                String query = queryValues.getProperty("query");
                Object[] values = (Object[]) queryValues.get("values");
                if (page <= 0) page = 1;
                int offset = (page - 1) * 10;
                return select(module.contains("recipe") ? "recipe_items" : "item_data", query, "LIMIT " + offset + ",10", module.contains("recipe") ? GET_RECIPE_ITEMS : GET_ITEMS, values);
            case "search-results":
                queryValues = (Properties) data[1];
                values = (Object[]) queryValues.get("values");
                query = queryValues.getProperty("query");
                module = (String) data[4];
                return new Object[]{(int) Utilities.roundUp(selectCount(module.contains("recipe") ? "recipe_items" : "item_data", query, values), 10)};
            case "add-sales-item":
                insert("sales", ((SalesItem) data[1]).data());
                break;
            case "get-sales-items-by-date":
                return select("sales", "date=?", GET_SALES_ITEMS, data[1]);
            case "get-sales-items-by-date-and-item":
                return select("sales", "date=? AND (bill_name=? OR name=?)", GET_SALES_ITEMS, data[1], data[2], data[2]);
            case "delete-sales-items-by-date":
                delete("sales", "date=?", data[1]);
                break;
            case "get-sales-by-month":
                return select("sales", "MONTH(date)=MONTH(?)", GET_SALES_ITEMS, data[1]);
            case "get-sales-by-year":
                return select("sales", "YEAR(date)=YEAR(?)", GET_SALES_ITEMS, data[1]);
            case "create-invoice":
                insert("invoices", ((Invoice) data[1]).data());
                break;
            case "delete-invoice":
                delete("invoices", "invoice_id=?", (long) data[1]);
                break;
            case "get-invoices":
                page = (int) data[1];
                if (page <= 0) page = 1;
                offset = (page - 1) * 10;
                return select("invoices", null, "ORDER BY ship_date DESC LIMIT " + offset + ",10", GET_INVOICES);
            case "get-invoices-page-count":
                return new Object[]{Utilities.roundUp(selectCount("invoices", null), 10)};
            case "edit-invoice-size":
                long invoiceId = (long) data[1];
                int ordered = (int) data[2];
                int shipped = (int) data[3];
                double subtotal = (double) data[4];
                double gst = (double) data[5];
                double pst = (double) data[6];
                double discount = (double) data[7];
                set("invoices", "quantity_ordered=?, quantity_shipped=?, subtotal=?, gst=?, pst=?, discount=?", "invoice_id=?", ordered, shipped, subtotal, gst, pst, discount, invoiceId);
                break;
            case "get-biggest-truck-this-year":
                return select("invoices", "YEAR(ship_date) = YEAR(CURDATE())", "ORDER BY price DESC LIMIT 1", GET_INVOICE);
            case "get-biggest-truck-this-month":
                return select("invoices", "MONTH(ship_date) = MONTH(CURDATE())", "ORDER BY (subtotal+gst+pst+discount) DESC LIMIT 1", GET_INVOICE);
            case "get-total-orders":
                return new Object[]{selectCount("invoices", "YEAR(ship_date) = YEAR(CURDATE())")};
            case "get-total-orders-price":
                double total = 0;
                data = select("invoices", "YEAR(ship_date) = YEAR(CURDATE())", GET_INVOICES);
                if (data == null) return new Object[]{0};
                ArrayList<Invoice> list = (ArrayList<Invoice>) data[0];
                total = list.stream().mapToDouble(i -> i.getTotal()).sum();
                return new Object[]{total};
            case "get-kitchen-meals":
                page = (int) data[1];
                if (page <= 0) page = 1;
                offset = (page - 1) * 10;
                return select("meals", null, "LIMIT " + offset + ",10", GET_KITCHEN_MEALS);
            case "get-kitchen-meals-count":
                return new Object[]{(int) Utilities.roundUp(selectCount("meals", null), 10)};
            case "add-kitchen-meal":
                insert("meals", ((KitchenMeal) data[1]).data());
                break;
            case "get-recipe-items":
                page = (int) data[1];
                if (page <= 0) page = 1;
                offset = (page - 1) * 10;
                return select("recipe_items", null, "LIMIT " + offset + ",10", GET_RECIPE_ITEMS);
            case "get-recipe-items-count":
                return new Object[]{(int) Utilities.roundUp(selectCount("recipe_items", null), 10)};
            case "add-recipe-item":
                insert("recipe_items", ((RecipeItem) data[1]).data());
                break;
            case "update-recipe-item":
                updateExisting("recipe_items", (int) data[1], (String[]) data[2], (Object[]) data[3]);
                break;
            case "get-recipe-item-by-bill-name":
                return select("recipe_items", "bill_name=?", GET_RECIPE_ITEM, data[1]);
            case "get-recipe-item-by-name":
                return select("recipe_items", "name=?", GET_RECIPE_ITEM, data[1]);
            case "get-recipe-item-by-id":
                return select("recipe_items", "id=?", GET_RECIPE_ITEM, data[1]);
            case "remove-recipe-item":
                delete("recipe_items", "id=?", data[1]);
                break;
            case "get-last-truck":
                return select("invoices", "ship_date " + (opcode.contains("last") ? "<" : ">") + " CURDATE() ORDER BY ship_date " + (opcode.contains("last") ? "DESC" : "ASC") + " LIMIT 1", GET_INVOICE);
            case "get-invoice":
                return select("invoices", "invoice_id=?", GET_INVOICE, String.valueOf(data[1]));
            case "get-items":
                page = (int) data[1];
                if (page <= 0) page = 1;
                offset = (page - 1) * 10;
                return select("item_data", null, "LIMIT " + offset + ",10", GET_ITEMS);
            case "get-items-count":
                return new Object[]{(int) Utilities.roundUp(selectCount("item_data", null), 10)};
            case "get-item-by-id":
                return select("item_data", "id=?", GET_ITEM, data[1]);
            case "get-item-by-code":
                return select("item_data", "item_code=?", GET_ITEM, data[1]);
            case "get-item-by-upc":
                return select("item_data", "upc_code=?", GET_ITEM, data[1]);
            case "get-item-by-display":
                return select("item_data", "display_name=?", GET_ITEM, data[1]);
            case "get-item-in-invoice":
                return select("invoice_items", "invoice_id=? AND item_code=?", GET_INVOICE_ITEM, data[1], data[2]);
            case "get-all-items":
                return select("item_data", null, GET_ITEMS);
            case "set-display-name":
                set("item_data", "display_name=?", "item_code=?", data[2], data[1]);
                break;
            case "update-units":
                set("item_data", "units=?", "id=?", data[2], data[1]);
                break;
            case "create-item":
                insert("item_data", ((ItemData) data[1]).data());
                break;
            case "add-invoice-item":
                insert("invoice_items", ((InvoiceItem) data[1]).data());
                break;
            case "change-price":
                String itemCode = (String) data[1];
                double oldPrice = (double) data[2];
                double newPrice = (double) data[3];
                Timestamp date = (Timestamp) data[4];
                set("item_data", "price=?", "item_code=?", newPrice, itemCode);
                insert("price_changes", "DEFAULT", itemCode, oldPrice, newPrice, date);
                break;
        }
        return null;
    }

    private RecipeItem loadRecipeItem(ResultSet set) {
        int id = getInt(set, "id");
        String name = getString(set, "name");
        String billName = getString(set, "bill_name");
        String recipe = getString(set, "recipe");
        String modifiers = getString(set, "modifiers");
        double price = getDouble(set, "price");
        Timestamp added = getTimestamp(set, "added");
        return new RecipeItem(id, name, billName, recipe, modifiers, price, added);
    }

    private final InvoiceItem getInvoiceItem(ResultSet set) {
        int id = getInt(set, "id");
        long invoiceId = getLongInt(set, "invoice_id");
        String itemCode = getString(set, "item_code");
        double price = getDouble(set, "price");
        double extPrice = getDouble(set, "ext_price");
        int quantityOrdered = getInt(set, "quantity_ordered");
        int quantityShipped = getInt(set, "quantity_shipped");
        String shipUnit = getString(set, "ship_unit");
        String status = getString(set, "status");
        return new InvoiceItem(id, invoiceId, itemCode, price, extPrice, quantityOrdered, quantityShipped, shipUnit, status);
    }

    private final Invoice getInvoice(ResultSet set) {
        int id = getInt(set, "id");
        long invoiceId = getLongInt(set, "invoice_id");
        String vendorName = getString(set, "vendor");
        int quantityOrdered = getInt(set, "quantity_ordered");
        int quantityShipped = getInt(set, "quantity_shipped");
        double subtotal = getDouble(set, "subtotal");
        double gst = getDouble(set, "gst");
        double pst = getDouble(set, "pst");
        double discount = getDouble(set, "discount");
        Timestamp shipDate = getTimestamp(set, "ship_date");
        Timestamp dateEntered = getTimestamp(set, "date_entered");
        Timestamp lastUpdated = getTimestamp(set, "last_updated");
        return new Invoice(id, invoiceId, vendorName, quantityOrdered, quantityShipped, subtotal, gst, pst, discount, shipDate, dateEntered, lastUpdated);
    }

    private final ItemData getItemData(ResultSet set) {
        int id = getInt(set, "id");
        String itemCode = getString(set, "item_code");
        String upcCode = getString(set, "upc_code");
        double price = getDouble(set, "price");
        String itemName = getString(set, "item_name");
        String displayName = getString(set, "display_name");
        String brand = getString(set, "brand");
        String vendor = getString(set, "vendor");
        double packCount = getDouble(set, "pack_count");
        double packSize = getDouble(set, "pack_size");
        String packUnit = getString(set, "pack_unit");
        String units = getString(set, "units");
        Timestamp added = getTimestamp(set, "added");
        Timestamp updated = getTimestamp(set, "last_updated");
        return new ItemData(id, itemCode, upcCode, price, itemName, displayName, brand, vendor, packCount, packSize, packUnit, units, added, updated);
    }

    private final SalesItem getSalesItem(ResultSet set) {
        int id = getInt(set, "id");
        String billName = getString(set, "bill_name");
        int quantity = getInt(set, "quantity");
        double price = getDouble(set, "price");
        Timestamp date = getTimestamp(set, "date");
        return new SalesItem(id, billName, quantity, price, date);
    }

    private final SQLQuery GET_RECIPE_ITEM = set -> {
        if (empty(set)) return null;
        return new Object[]{loadRecipeItem(set)};
    };

    private final SQLQuery GET_RECIPE_ITEMS = set -> {
        ArrayList<RecipeItem> items = new ArrayList<>();
        if (wasNull(set)) return new Object[]{items};
        while (next(set)) items.add(loadRecipeItem(set));
        return new Object[]{items};
    };

    private final SQLQuery GET_INVOICES = (set) -> {
        ArrayList<Invoice> invoices = new ArrayList<>();
        if (wasNull(set)) return new Object[]{invoices};
        while (next(set)) invoices.add(getInvoice(set));
        return new Object[]{invoices};
    };

    private final SQLQuery GET_INVOICE = (set) -> {
        if (empty(set)) return null;
        return new Object[]{getInvoice(set)};
    };

    private final SQLQuery GET_ITEM = set -> {
        if (empty(set)) return null;
        return new Object[]{getItemData(set)};
    };

    private final SQLQuery GET_ITEMS = set -> {
        ArrayList<ItemData> items = new ArrayList<>();
        if (wasNull(set)) return new Object[]{items};
        while (next(set)) items.add(getItemData(set));
        return new Object[]{items};
    };

    private final SQLQuery GET_INVOICE_ITEM = set -> {
        if (empty(set)) return null;
        return new Object[]{getInvoiceItem(set)};
    };

    private final SQLQuery GET_INVOICE_ITEMS = set -> {
        ArrayList<InvoiceItem> items = new ArrayList<>();
        if (wasNull(set)) return new Object[]{items};
        while (next(set)) items.add(getInvoiceItem(set));
        return new Object[]{items};
    };

    private final SQLQuery GET_SALES_ITEMS = set -> {
        ArrayList<SalesItem> items = new ArrayList<>();
        if (wasNull(set)) return new Object[]{items};
        while (next(set)) items.add(getSalesItem(set));
        return new Object[]{items};
    };

    private final SQLQuery GET_KITCHEN_MEAL = set -> {
        if (empty(set)) return null;
        return new Object[]{loadKitchenMeal(set)};
    };

    private final SQLQuery GET_KITCHEN_MEALS = set -> {
        ArrayList<KitchenMeal> meals = new ArrayList<>();
        if (wasNull(set)) return new Object[]{meals};
        while (next(set)) meals.add(loadKitchenMeal(set));
        return new Object[]{meals};
    };

    private KitchenMeal loadKitchenMeal(ResultSet set) {
        int id = getInt(set, "id");
        int employeeId = getInt(set, "employee_id");
        String main = getString(set, "main");
        String side = getString(set, "side");
        Timestamp date = getTimestamp(set, "date");
        Timestamp lastUpdated = getTimestamp(set, "last_updated");
        return new KitchenMeal(id, employeeId, main, side, date, lastUpdated);
    }
}
