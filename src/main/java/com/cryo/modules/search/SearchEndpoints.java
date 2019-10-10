package com.cryo.modules.search;

import com.cryo.db.DatabaseConnection;
import com.cryo.db.impl.InventoryConnection;
import com.cryo.db.impl.LabourConnection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
public enum SearchEndpoints {

    // TODO: Sales Searching

    EMPLOYEES("employees", "employees", 0, LabourConnection.connection(),
            "./source/modules/labour/employees/employees_list.jade"),
    HOURS("hours", "hourdata", 0, LabourConnection.connection(), "./source/modules/labour/hours/lists/hours_list.jade"),
    // SALES("sales", "sales", 0, LabourConnection.connection(),
    // "./source/modules/labour/sales/sales_list.jade"),
    RECIPE_ITEM("recipe-items", "recipeitems", 0, InventoryConnection.connection(),
            "./source/modules/inventory/items/recipe_items_list.jade"),
    INVOICE_ITEMS("invoice-items", "items", 0, InventoryConnection.connection(), "./source/modules/inventory/items/invoice/invoice_items_list.jade");

    private @Getter
    String name, key;
    private @Getter
    int rights;
    private @Getter
    DatabaseConnection connection;
    private @Getter
    String jadeFile;

    public static Optional<SearchEndpoints> getEndpoint(String name) {
        return Stream.of(SearchEndpoints.values()).filter(e -> e.getName().equals(name)).findAny();
    }

}
