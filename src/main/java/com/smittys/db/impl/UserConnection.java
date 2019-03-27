package com.smittys.db.impl;

import com.smittys.Tracker;
import com.smittys.db.DBConnectionManager;
import com.smittys.db.DatabaseConnection;
import com.smittys.entities.SQLQuery;
import com.smittys.entities.User;
import com.smittys.utils.BCrypt;
import com.smittys.utils.SessionIDGenerator;

import java.sql.Timestamp;
import java.util.Calendar;

public class UserConnection extends DatabaseConnection {

    public UserConnection() {
        super("smittys_global");
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
                data = select("user_data", "username=?", GET_USER, username);
                if (data == null) return null;
                User user = (User) data[0];
                String hash = user.getHash();
                String salt = user.getSalt();
                return new Object[]{BCrypt.hashPassword(password, salt).equals(hash), salt};
            case "create-user":
                username = (String) data[1];
                hash = (String) data[2];
                salt = (String) data[3];
                try {
                    insert("user_data", username, hash, salt, "DEFAULT");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
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
