package com.simplebuilding.modules.simpleearth.view;

import java.util.HashMap;
import java.util.Map;

/** Numbers one earth version reads each frame (edge length plus its own style values by key). */
public final class ViewParams {
    public double edge = 65536;
    private final Map<String, Double> values = new HashMap<>();

    public ViewParams set(String key, double value) {
        values.put(key, value);
        return this;
    }

    public double get(String key, double fallback) {
        Double v = values.get(key);
        return v == null || !Double.isFinite(v) ? fallback : v;
    }
}
