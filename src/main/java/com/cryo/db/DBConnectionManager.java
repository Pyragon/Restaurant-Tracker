package com.cryo.db;

import com.cryo.db.impl.*;

import java.util.HashMap;

/**
 * @author Cody Thompson <eldo.imo.rs@hotmail.com>
 * <p>
 * Created on: Mar 7, 2017 at 7:35:35 PM
 */
public class DBConnectionManager {

    private HashMap<Connection, DatabaseConnection> connections;

    public DBConnectionManager() {
        loadDriver();
        init();
    }

    public DatabaseConnection getConnection(Connection connection) {
        return connections.get(connection);
    }

    public void loadDriver() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
        connections = new HashMap<>();
        connections.put(Connection.USERS, new UserConnection());
        connections.put(Connection.LABOUR, new LabourConnection());
        connections.put(Connection.INVENTORY, new InventoryConnection());
        connections.put(Connection.ERROR, new ErrorConnection());
        connections.put(Connection.MISC, new MiscConnection());
    }

    public enum Connection {
        USERS, LABOUR, INVENTORY, ERROR, MISC
    }

}
