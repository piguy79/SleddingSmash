package com.railwaygames.sleddingsmash.utils;

import java.util.Map;

public class MapUtils {

    /**
     * Copy values from defaultMap to map only if !map.contains(key)
     *
     * @param map        Map to copy values into
     * @param defaultMap Map to copy values from
     */
    public static void addDefaults(Map<String, Object> map, Map<String, Object> defaultMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
