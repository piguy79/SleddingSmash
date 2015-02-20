package com.railwaygames.sleddingsmash.levels.modifiers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.railwaygames.sleddingsmash.utils.MapUtils;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.MathUtils.MinMax;

import java.util.HashMap;
import java.util.Map;

public class SlopeModifier implements TerrainModifier {
    /**
     * Modify type: s (scale), t (transform)
     */
    public static final String MODIFICATION_TYPE = "modificationType";

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
        put(INTERPOLATION, Interpolation.pow2);
        put(IMPACT_AMOUNT, -30.0f);
        put(EVAL_AXIS_START_RATIO, 0.1f);
        put(EVAL_AXIS_INTERPOLATION_DURATION, 0.25f);
    }};

    @Override
    public void modify(Model model, Map<String, Object> params) {
        MapUtils.addDefaults(params, defaultParams);

        String modificationType = (String) params.get(MODIFICATION_TYPE);
        if (modificationType == null || !(modificationType.equals("s") || modificationType.equals("t"))) {
            throw new IllegalArgumentException("Modification type must be s or t");
        }

        checkValidAxis(params);

        Mesh mesh = model.meshes.get(0);

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[mesh.getNumVertices() * newVertexOffset];
        mesh.getVertices(vertices);

        String evalAxis = (String) params.get(EVAL_AXIS);
        String impactAxis = (String) params.get(IMPACT_AXIS);

        // invert for Z-axis, since it's backwards
        if (evalAxis.equals("z")) {
            params.put(EVAL_AXIS_START_RATIO, 1.0f - ((Float) params.get(EVAL_AXIS_START_RATIO)));
        }

        Map<String, MinMax> minMaxMap = MathUtils.calculateAxisMinMax(vertices, newVertexOffset);
        AxisEvaluator pointEvaluator = createAxisEvaluator(modificationType, evalAxis, impactAxis, params, minMaxMap);

        for (int i = 0; i < vertices.length; i += newVertexOffset) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            Vector3 modifiedPoint = pointEvaluator.evaluate(x, y, z);
            if (modifiedPoint != null) {
                vertices[i] = modifiedPoint.x;
                vertices[i + 1] = modifiedPoint.y;
                vertices[i + 2] = modifiedPoint.z;
            }
        }

        mesh.setVertices(vertices);
    }

    private void checkValidAxis(Map<String, Object> params) {
        String val = (String) params.get(EVAL_AXIS);
        if (!(val.equals("x") || val.equals("y") || val.equals("z"))) {
            throw new IllegalArgumentException("EVAL_AXIS must be x, y, or z");
        }

        val = (String) params.get(IMPACT_AXIS);
        if (!(val.equals("x") || val.equals("y") || val.equals("z"))) {
            throw new IllegalArgumentException("IMPACT_AXIS must be x, y, or z");
        }
    }

    private AxisEvaluator createAxisEvaluator(String modificationType, String evalAxis, String impactAxis, Map<String, Object> params, Map<String, MinMax> minMaxMap) {
        if (modificationType.equals("t")) {
            if (evalAxis.equals("z") && impactAxis.equals("y")) {
                return new ZYTranslatingAxisEvaluator(params, minMaxMap);
            } else if (evalAxis.equals("z") && impactAxis.equals("x")) {
                return new ZXTranslatingAxisEvaluator(params, minMaxMap);
            } else if (evalAxis.equals("x") && impactAxis.equals("y")) {
                return new XYTranslatingAxisEvaluator(params, minMaxMap);
            }

            throw new RuntimeException("Translating class not defined for evalAxis '" + evalAxis + "' and impactAxis '" + impactAxis + "'");
        } else {
            if (evalAxis.equals("z") && impactAxis.equals("x")) {
                return new ZXScalingAxisEvaluator(params, minMaxMap);
            }

            throw new RuntimeException("Scaling class not defined for evalAxis '" + evalAxis + "' and impactAxis '" + impactAxis + "'");
        }
    }

    public abstract class AxisEvaluator {
        // reuse for efficiency
        private Vector3 tmpVec = new Vector3();
        private Interpolation interpolation;
        private float impactAmount;
        private Map<String, MinMax> minMaxMap;
        private float evalAxisStartRatio;
        private float evalAxisInterpolationDuration;

        public AxisEvaluator(Map<String, Object> params, Map<String, MinMax> minMaxMap) {
            this.interpolation = (Interpolation) params.get(INTERPOLATION);
            this.impactAmount = (Float) params.get(IMPACT_AMOUNT);
            this.evalAxisStartRatio = ((Float) params.get(EVAL_AXIS_START_RATIO));
            this.evalAxisInterpolationDuration = ((Float) params.get(EVAL_AXIS_INTERPOLATION_DURATION));
            this.minMaxMap = minMaxMap;
        }

        public Vector3 evaluate(float x, float y, float z) {
            float val = getValueToEvaluate(x, y, z);
            float length = getAxisMinMax().max - getAxisMinMax().min;
            float start = getAxisMinMax().min + evalAxisStartRatio * length;
            if ((invert() && val <= start) || ((!invert() && val >= start))) {
                tmpVec.set(x, y, z);

                float duration = length * evalAxisInterpolationDuration;
                float ratio = interpolation.apply((val - start) / duration);
                setEvaluatedValue(Math.max(-1.0f, Math.min(1.0f, ratio)), tmpVec);

                return tmpVec;
            }

            return null;
        }

        protected boolean invert() {
            return false;
        }

        public abstract float getValueToEvaluate(float x, float y, float z);

        public abstract void setEvaluatedValue(float newVal, Vector3 vec);

        public abstract MinMax getAxisMinMax();

        public float getImpactAmount() {
            return impactAmount;
        }

        public Map<String, MinMax> getMinMaxMap() {
            return minMaxMap;
        }
    }

    public class ZXScalingAxisEvaluator extends AxisEvaluator {

        public ZXScalingAxisEvaluator(Map<String, Object> params, Map<String, MinMax> minMaxMap) {
            super(params, minMaxMap);
        }

        @Override
        public float getValueToEvaluate(float x, float y, float z) {
            return z;
        }

        @Override
        public void setEvaluatedValue(float ratio, Vector3 vec) {
            vec.x = getMinMaxMap().get("x").mid + (vec.x - getMinMaxMap().get("x").mid) * getImpactAmount();
        }

        @Override
        public boolean invert() {
            return true;
        }

        @Override
        public MinMax getAxisMinMax() {
            return getMinMaxMap().get("z");
        }
    }

    public class ZYTranslatingAxisEvaluator extends AxisEvaluator {

        public ZYTranslatingAxisEvaluator(Map<String, Object> params, Map<String, MinMax> minMaxMap) {
            super(params, minMaxMap);
        }

        @Override
        public float getValueToEvaluate(float x, float y, float z) {
            return z;
        }

        @Override
        public void setEvaluatedValue(float ratio, Vector3 vec) {
            vec.y += getImpactAmount() * ratio;
        }

        @Override
        public boolean invert() {
            return true;
        }

        @Override
        public MinMax getAxisMinMax() {
            return getMinMaxMap().get("z");
        }
    }

    public class XYTranslatingAxisEvaluator extends AxisEvaluator {

        public XYTranslatingAxisEvaluator(Map<String, Object> params, Map<String, MinMax> minMaxMap) {
            super(params, minMaxMap);
        }

        @Override
        public float getValueToEvaluate(float x, float y, float z) {
            return x;
        }

        @Override
        public void setEvaluatedValue(float ratio, Vector3 vec) {
            vec.y += getImpactAmount() * ratio;
        }

        @Override
        public MinMax getAxisMinMax() {
            return getMinMaxMap().get("x");
        }
    }

    public class ZXTranslatingAxisEvaluator extends AxisEvaluator {

        public ZXTranslatingAxisEvaluator(Map<String, Object> params, Map<String, MinMax> minMaxMap) {
            super(params, minMaxMap);
        }

        @Override
        public float getValueToEvaluate(float x, float y, float z) {
            return z;
        }

        @Override
        public void setEvaluatedValue(float ratio, Vector3 vec) {
            vec.x += getImpactAmount() * ratio;
        }

        @Override
        public boolean invert() {
            return true;
        }

        @Override
        public MinMax getAxisMinMax() {
            return getMinMaxMap().get("z");
        }
    }
}
