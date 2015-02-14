package com.railwaygames.sleddingsmash.utils;

public class MathUtils {

    /**
     * @return Generate a random number within [min, max]
     */
    public static float randomInRange(float min, float max) {
        return min + ((float) Math.random()) * (max - min);
    }
}
