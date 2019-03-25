package com.smittys.entities;

import com.smittys.Tracker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import spark.Request;
import spark.Response;

import java.sql.Timestamp;
import java.util.Properties;

@RequiredArgsConstructor
@Data
public abstract class Error {

    protected final int id;
    protected final String key;
    protected final String shortMessage;
    protected final String longMessage;
    protected final ErrorParent parent;

    protected final boolean error;

    protected String link;
    protected Properties parameters;

    protected Timestamp added;
    protected Timestamp solved;

    public Error(String key, String shortMessage, String longMessage, ErrorParent parent) {
        this(-1, key, shortMessage, longMessage, parent, true);
    }

    public boolean hasLeftClick() {
        return false;
    }

    public boolean refreshAfterClick() {
        return false;
    }

    public boolean recheck() {
        return true;
    }

    public boolean opensModal() {
        return false;
    }

    public Properties getModalData(Request request, Response response) {
        return null;
    }

    public String click() {
        return null;
    }

    public String buttonClick(String button, Properties prop) {
        return null;
    }

    public int getType() {
        return parent.ordinal();
    }

    public enum ErrorParent {
        LABOUR,
        INVENTORY,
        SYSCO,
        TRACKER;

        public static ErrorParent getError(int index) {
            if (index < 0 || index >= ErrorParent.values().length) return null;
            return ErrorParent.values()[index];
        }
    }

    public Object[] data() {
        return new Object[]{"DEFAULT", parent.ordinal(), key, shortMessage, longMessage, link == null ? "NULL" : link, parameters == null ? "NULL" : Tracker.getGson().toJson(parameters), 1, "DEFAULT", "NULL"};
    }
}
