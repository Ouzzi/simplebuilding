package com.simplebuilding.common;

public enum SimplebuildingLoader {
    FABRIC("fabric"),
    NEOFORGE("neoforge"),
    FORGE("forge");

    private final String id;

    SimplebuildingLoader(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}