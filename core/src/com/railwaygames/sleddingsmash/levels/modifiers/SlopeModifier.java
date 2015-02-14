package com.railwaygames.sleddingsmash.levels.modifiers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.railwaygames.sleddingsmash.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;

public class SlopeModifier implements TerrainModifier {
    /**
     * Axis to evaluation for changes: x, y, z
     */
    public static final String EVAL_AXIS = "evalAxis";

    /**
     * After evaluating the EVAL_AXIS, modify this axis: x, y, z
     */
    public static final String IMPACT_AXIS = "impactAxis";

    /**
     * Pick an interpolation
     *
     * @see com.badlogic.gdx.math.Interpolation
     */
    public static final String INTERPOLATION = "interpolation";

    /**
     * The number of units to move the impact axis. Negative values to move down.
     */
    public static final String IMPACT_AMOUNT = "impactAmount";

    /**
     * Value between 0.0 and 1.0, 0.0 being the beginning of the EVAL_AXIS, 1.0 being the end.
     */
    public static final String EVAL_AXIS_START_RATIO = "evalAxisStartRatio";

    /**
     * Value between 0.0 and 1.0. Determines the distance to interpolate over, with 0.0 being an instant change and 1.0 stretching over the whole EVAL_AXIS.
     */
    public static final String EVAL_AXIS_INTERPOLATION_DURATION = "evalAxisInterpolationDuration";

    private Map<String, Object> defaultParams = new HashMap<String, Object>() {{
        put(EVAL_AXIS, "z");
        put(IMPACT_AXIS, "y");
        put(INTERPOLATION, Interpolation.pow2);
        put(IMPACT_AMOUNT, -30.0f);
        put(EVAL_AXIS_START_RATIO, 0.1f);
        put(EVAL_AXIS_INTERPOLATION_DURATION, 0.25f);
    }};

    @Override
    public void modify(Model model, Map<String, Object> params) {
        MapUtils.addDefaults(params, defaultParams);

        checkValidAxis(params);

        Mesh mesh = model.meshes.get(0);

        float[] vertices = new float[mesh.getNumVertices() * 3];
        mesh.getVertices(vertices);

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        String evalAxis = (String) params.get(EVAL_AXIS);
        String impactAxis = (String) params.get(IMPACT_AXIS);
        Interpolation interpolation = (Interpolation) params.get(INTERPOLATION);
        float impactAmount = (Float) params.get(IMPACT_AMOUNT);

        AxisEvaluator pointEvaluator = createAxisEvaluator(evalAxis, impactAxis, interpolation, impactAmount);

        float evalAxisLength = calculateAxisLength(evalAxis, vertices, newVertexOffset);
        float offset = ((Float) params.get(EVAL_AXIS_START_RATIO)) * evalAxisLength;
        float duration = ((Float) params.get(EVAL_AXIS_INTERPOLATION_DURATION)) * evalAxisLength;

        for (int i = 0; i < vertices.length; i += newVertexOffset) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            Vector3 modifiedPoint = pointEvaluator.evaluate(x, y, z, offset, duration);
            if (modifiedPoint != null) {
                vertices[i] = modifiedPoint.x;
                vertices[i + 1] = modifiedPoint.y;
                vertices[i + 2] = modifiedPoint.z;
            }
        }

        mesh.setVertices(vertices);
    }

    private float calculateAxisLength(String evalAxis, float [] vertices, float newVertexOffset) {
        float min = Integer.MAX_VALUE;
        float max = Integer.MIN_VALUE;

        for (int i = 0; i < vertices.length; i += newVertexOffset) {
            float val;
            if(evalAxis.equals("x")) {
                val = vertices[i];
            } else if(evalAxis.equals("y")) {
                val = vertices[i+1];
            } else {
                val = vertices[i+2];
            }

            min = Math.min(val, min);
            max = Math.max(val, max);
        }

        return max - min;
    }

    private void checkValidAxis(Map<String, Object> params) {
        String val = (String)params.get(EVAL_AXIS);
        if(!(val.equals("x") || val.equals("y") || val.equals("z"))) {
            throw new IllegalArgumentException("EVAL_AXIS must be x, y, or z");
        }

        val = (String)params.get(IMPACT_AXIS);
        if(!(val.equals("x") || val.equals("y") || val.equals("z"))) {
            throw new IllegalArgumentException("IMPACT_AXIS must be x, y, or z");
        }
    }

    private AxisEvaluator createAxisEvaluator(String evalAxis, String impactAxis, Interpolation interpolation, float impactAmount) {
        if (evalAxis.equals("z") && impactAxis.equals("y")) {
            return new ZYAxisEvaluator(interpolation, impactAmount);
        }

        throw new RuntimeException("Class not defined for evalAxis '" + evalAxis + "' and impactAxis '" + impactAxis + "'");
    }

    public abstract class AxisEvaluator {
        // reuse for efficiency
        private Vector3 tmpVec = new Vector3();
        private Interpolation interpolation;
        private float impactAmount;

        public AxisEvaluator(Interpolation interpolation, float impactAmount) {
            this.interpolation = interpolation;
            this.impactAmount = impactAmount;
        }

        public Vector3 evaluate(float x, float y, float z, float start, float duration) {
            float val = getValueToEvaluate(x, y, z);
            if (val >= start) {
                tmpVec.set(x, y, z);

                float newVal = interpolation.apply((val - start) / duration);
                setEvaluatedValue(Math.max(0, Math.min(1.0f, newVal)), tmpVec);

                return tmpVec;
            }

            return null;
        }

        public abstract float getValueToEvaluate(float x, float y, float z);

        public abstract void setEvaluatedValue(float newVal, Vector3 vec);

        public float getImpactAmount() {
            return impactAmount;
        }
    }

    public class ZYAxisEvaluator extends AxisEvaluator {

        public ZYAxisEvaluator(Interpolation interpolation, float impactAmount) {
            super(interpolation, impactAmount);
        }

        @Override
        public float getValueToEvaluate(float x, float y, float z) {
            // z axis goes in negative direction
            return Math.abs(z);
        }

        @Override
        public void setEvaluatedValue(float newVal, Vector3 vec) {
            vec.y = vec.y + getImpactAmount() * newVal;
        }
    }
}
