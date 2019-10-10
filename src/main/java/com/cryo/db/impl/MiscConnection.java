package com.cryo.db.impl;

import com.cryo.Tracker;
import com.cryo.db.DBConnectionManager;
import com.cryo.db.DatabaseConnection;
import com.cryo.entities.MiscData;
import com.cryo.entities.SQLQuery;

import java.sql.Timestamp;

public class MiscConnection extends DatabaseConnection {

    public MiscConnection() {
        super("smittys_global");
    }

    public static MiscConnection connection() {
        return (MiscConnection) Tracker.getConnectionManager().getConnection(DBConnectionManager.Connection.MISC);
    }

    @Override
    public Object[] handleRequest(Object... data) {
        String opcode = (String) data[0];
        switch(opcode) {
            case "get-value":
                return select("misc", "key=?", GET_VALUE, (String) data[1]);
            case "set-value":
                String key = (String) data[1];
                Object value = data[2];
                data = handleRequest("get-value", key);
                if(data == null)
                    insert("misc", "DEFAULT", key, value, "DEFAULT");
                else
                    set("misc", "value=?", "key=?", value, key);
                break;
        }
        return null;
    }

    private final SQLQuery GET_VALUE = set -> {
        if(empty(set)) return null;
        int id = getInt(set, "id");
        String key = getString(set, "key");
        Object value = getObject(set, "value");
        Timestamp added = getTimestamp(set, "added");
        return new Object[] { new MiscData(id, key, value, added) };
    };
}
