package com.cryo.db.impl;

import com.cryo.Tracker;
import com.cryo.db.DBConnectionManager;
import com.cryo.db.DatabaseConnection;
import com.cryo.entities.SQLQuery;
import com.cryo.entities.User;
import com.cryo.utils.BCrypt;
import com.cryo.utils.SessionIDGenerator;

import java.sql.Timestamp;
import java.util.Calendar;

public class UserConnection extends DatabaseConnection {

    public UserConnection() {
        super("citycenter_global");
    }

    public static UserConnection connection() {
        return (UserConnection) Tracker.getConnectionManager().getConnection(DBConnectionManager.Connection.USERS);
    }

    @Override
    public Object[] handleRequest(Object... data) {
        String opcode = (String) data[0];
        switch (opcode) {
            case "compare":
                String username = (String) data[1];
                String password = (String) data[2];
                User user = selectClass("user_data", "username=?", User.class, username);
                if (user == null) return null;
                String hash = user.getHash();
                String salt = user.getSalt();
                return new Object[]{BCrypt.hashPassword(password, salt).equals(hash), salt};
            case "create-user":
                try {
                    insert("user_data", ((User) data[1]).data());
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return new Object[]{};
            case "change-pass":
                set("user_data", "hash=?", "id=?", data[1], data[2]);
                break;
            case "get-user":
                return select("user_data", "username=?", GET_USER, (String) data[1]);
            case "get-user-from-id":
                return select("user_data", "id=?", GET_USER, (int) data[1]);
            case "get-user-from-sess":
                return select("sessions", "sess_id=?", GET_SESSION_USER, (String) data[1]);
            case "add-sess":
                username = (String) data[1];
                String sess_id = SessionIDGenerator.getInstance().getSessionId();
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, 30);
                Timestamp stamp = new Timestamp(c.getTime().getTime());
                insert("sessions", username, sess_id, stamp);
                return new Object[]{sess_id};
            case "remove-all-sess":
                username = (String) data[1];
                delete("sessions", "username=?", username);
                break;
        }
        return null;
    }

    private SQLQuery GET_USER = (set) -> {
        if (empty(set)) return null;
        int id = getInt(set, "id");
        String username = getString(set, "username");
        String firstName = getString(set, "first_name");
        String lastName = getString(set, "last_name");
        String hash = getString(set, "hash");
        String salt = getString(set, "salt");
        Timestamp created = getTimestamp(set, "created");
        return new Object[]{new User(id, username, firstName, lastName, hash, salt, created)};
    };

    private SQLQuery GET_SESSION_USER = (set) -> {
        if (empty(set)) return null;
        String username = getString(set, "username");
        return handleRequest("get-user", username);
    };
}
