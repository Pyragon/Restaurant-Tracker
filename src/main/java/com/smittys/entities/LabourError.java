package com.smittys.entities;

public class LabourError extends Error {

    public LabourError(int id, String key, String shortMessage, String longMessage) {
        super(key, shortMessage, longMessage, ErrorParent.LABOUR);
    }

    public LabourError(int id, String key, String shortMessage, String longMessage, boolean active) {
        super(id, key, shortMessage, longMessage, ErrorParent.LABOUR, active);
    }

    @Override
    public boolean recheck() {
        return true;
    }
}
