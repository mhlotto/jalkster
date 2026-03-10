package com.example.jalkster.live;

public enum ActMode {
    JOG("jog"),
    WALK("walk"),
    REST("rest");

    private final String id;

    ActMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ActMode fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase();
        if ("jog".equals(normalized)) {
            return JOG;
        }
        if ("walk".equals(normalized)) {
            return WALK;
        }
        if ("rest".equals(normalized)) {
            return REST;
        }
        return null;
    }
}
