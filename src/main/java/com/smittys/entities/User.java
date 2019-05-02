package com.smittys.entities;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@RequiredArgsConstructor
@Data
public class User {

    private final int id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String hash;
    private final String salt;
    private final Timestamp created;

    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    public Object[] data() {
        return new Object[]{"DEFAULT", username, firstName, lastName == null ? "" : lastName, hash, salt, "DEFAULT"};
    }
}
