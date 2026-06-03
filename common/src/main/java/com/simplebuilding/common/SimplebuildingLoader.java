package com.simplebuilding.common;

public enum SimplebuildingLoader {
    FABRIC("fabric"),
    FORGE("forge"),
    NEOFORGE("neoforge");

    private final String id;

    SimplebuildingLoader(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}