package com.smittys.db.impl;

import com.smittys.Tracker;
import com.smittys.db.DBConnectionManager;
import com.smittys.db.DatabaseConnection;
import com.smittys.entities.Error;
import com.smittys.entities.InventoryError;
import com.smittys.entities.LabourError;
import com.smittys.entities.SQLQuery;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;

public class ErrorConnection extends DatabaseConnection {

    public ErrorConnection() {
        super("smittys_global");
    }

    public static ErrorConnection connection() {
        return (ErrorConnection) Tracker.getConnectionManager().getConnection(DBConnectionManager.Connection.ERROR);
    }

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.startConnections();
        connection().handleRequest("fix-errors");
    }

    @Override
    public Object[] handleRequest(Object... data) {
        String opcode = (String) data[0];
        switch (opcode) {
            case "fix-errors":
                data = handleRequest("get-errors", true);
                ArrayList<Error> errors = (ArrayList<Error>) data[0];
                errors.forEach((e) -> {
                    if (!e.getKey().toLowerCase().contains("missing-unit")) return;
                    int id = Integer.parseInt(e.getKey().replace("missing-unit-", ""));
                    //Missing unit fl oz for TEA ICED
                    String unit = e.getShortMessage().replace("Missing unit ", "");
                    unit = unit.substring(0, unit.indexOf(" for"));
                    Properties prop = new Properties();
                    prop.put("unit", unit);
                    prop.put("id", id);
                    String parameters = Tracker.getGson().toJson(prop);
                    set("`errors`", "parameters=?", "`key`=?", parameters, e.getKey());
                });
                break;
            case "add-error":
                insert("errors", ((Error) data[1]).data());
                break;
            case "get-errors":
                return select("errors", "solved " + (((boolean) data[1]) ? "IS" : "IS NOT") + " NULL", GET_ERRORS);
            case "get-error":
                return select("errors", "`key`=?", GET_ERROR, (String) data[1]);
            case "set-solved":
                Error error = (Error) data[1];
                data = handleRequest("get-error", error.getKey());
                if (data == null) break;
                set("errors", "solved=DEFAULT", "`key`=?", error.getKey());
                break;
            case "delete":
                String key = (String) data[1];
                data = handleRequest("get-error", key);
                if (data == null) break;
                delete("errors", "`key`=?", key);
                break;
        }
        return null;
    }

    private final SQLQuery GET_ERRORS = set -> {
        ArrayList<Error> errors = new ArrayList<>();
        if (wasNull(set)) return new Object[]{errors};
        while (next(set)) {
            Error error = loadError(set);
            if (error != null) errors.add(error);
        }
        return new Object[]{errors};
    };

    private final SQLQuery GET_ERROR = set -> {
        if (empty(set)) return null;
        return new Object[]{loadError(set)};
    };

    private Error loadError(ResultSet set) {
        int id = getInt(set, "id");
        int type = getInt(set, "type");
        Error.ErrorParent parent = Error.ErrorParent.getError(type);
        if (parent == null)
            return null;
        try {
            String key = getString(set, "key");
            String shortMessage = getString(set, "short_message");
            String longMessage = getString(set, "long_message");
            boolean active = getInt(set, "active") == 1;
            Timestamp added = getTimestamp(set, "added");
            Timestamp solved = getTimestamp(set, "solved");
            String parameterString = getString(set, "parameters");
            Error error = null;
            switch (parent) {
                case LABOUR:
                    error = new LabourError(id, key, shortMessage, longMessage, active);
                    break;
                case INVENTORY:
                    error = new InventoryError(id, key, shortMessage, longMessage, active);
                    break;
            }
            if (error == null)
                return null;
            error.setAdded(added);
            error.setSolved(solved);
            Properties parameters = new Properties();
            if (parameterString != null) {
                parameters = Tracker.getGson().fromJson(parameterString, Properties.class);
                error.setParameters(parameters);
            }
            return error;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
