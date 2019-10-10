package com.cryo.entities;

public class TipError extends Error {

    //allow errors to have 'parameters' (key, value)
    //for tips, parameters will have 'employees' list, with employees whose tips are missing
    //parameters are saved in JSON format in the database

    public TipError(int id, String key, String shortMessage, String longMessage, ErrorParent parent, boolean error) {
        super(id, key, shortMessage, longMessage, parent, error);
    }
}
