package com.railwaygames.sleddingsmash.utils;

import java.util.HashMap;
import java.util.Map;

public class MathUtils {

    /**
     * @return Generate a random number within [min, max]
     */
    public static float randomInRange(float min, float max) {
        return min + ((float) Math.random()) * (max - min);
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float one = x2 - x1;
        float two = y2 - y1;
        return (float) Math.sqrt(one * one + two * two);
    }

    public static Map<String, MinMax> calculateAxisMinMax(float[] vertices, float newVertexOffset) {
        Map<String, MinMax> minMaxMap = new HashMap<String, MinMax>();

        float xMin = Integer.MAX_VALUE;
        float xMax = Integer.MIN_VALUE;
        float yMin = Integer.MAX_VALUE;
        float yMax = Integer.MIN_VALUE;
        float zMin = Integer.MAX_VALUE;
        float zMax = Integer.MIN_VALUE;

        for (int i = 0; i < vertices.length; i += newVertexOffset) {
            float val;
            val = vertices[i];
            xMin = Math.min(val, xMin);
            xMax = Math.max(val, xMax);

            val = vertices[i + 1];
            yMin = Math.min(val, yMin);
            yMax = Math.max(val, yMax);

            val = vertices[i + 2];
            zMin = Math.min(val, zMin);
            zMax = Math.max(val, zMax);
        }

        minMaxMap.put("x", new MinMax(xMin, xMax));
        minMaxMap.put("y", new MinMax(yMin, yMax));
        minMaxMap.put("z", new MinMax(zMin, zMax));

        return minMaxMap;
    }

    public static class MinMax {

        public float min;
        public float max;
        public float mid;

        public MinMax(float min, float max) {
            this.min = min;
            this.max = max;
            this.mid = (max - min) * 0.5f;
        }

        public float axisSize() {
            return this.max - this.min;
        }
    }
}
