package com.railwaygames.sleddingsmash.levels.modifiers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Interpolation;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BumpyTerrainModifier implements TerrainModifier {

    /**
     * Number of bumps to create
     */
    public static final String COUNT = "count";

    /**
     * Precomputed hills list
     */
    public static final String HILLS = "hills";

    /**
     * (Optional) Specify the height of the hill
     */
    public static final String HEIGHT = "height";

    @Override
    public void modify(Model model, Map<String, Object> params) {
        Mesh mesh = model.meshes.get(0);

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[mesh.getNumVertices() * newVertexOffset];
        mesh.getVertices(vertices);

        List<Hill> hills = (List<Hill>) params.get(HILLS);
        for (Hill hill : hills) {
            if (hill.radius > -1.0f) {
                hill.xRadius = hill.radius;
                hill.zRadius = hill.radius;
            }
            float xRadSq = hill.xRadius * hill.xRadius;
            float zRadSq = hill.zRadius * hill.zRadius;

            for (int j = 0; j < vertices.length; j += newVertexOffset) {
                float x = vertices[j];
                float z = vertices[j + 2];

                float xDiff = hill.x - x;
                float zDiff = hill.z - z;
                double dist = xDiff * xDiff / xRadSq + zDiff * zDiff / zRadSq;
                if (dist <= 1.0) {
                    float newY = Interpolation.pow3.apply((float) (1.0 - dist)) * hill.height;
                    vertices[j + 1] += newY;
                }
            }
        }

        mesh.setVertices(vertices);
    }

    public List<Hill> generate(float width, float length, Map<String, Object> params) {
        int count = (Integer) params.get(COUNT);
        List<Hill> hills = new ArrayList<Hill>(count);
        for (int i = 0; i < count; ++i) {
            float x = (float) Math.random() * width;
            float z = (float) Math.random() * -length;
            float radius = MathUtils.randomInRange(0.0f, 50.0f);
            float height = MathUtils.randomInRange(-radius * 0.08f, radius * 0.15f);

            hills.add(new Hill(x, z, radius, radius, height));
        }

        return hills;
    }

    public static class Hill {
        public float x;
        public float z;
        public float xRadius;
        public float zRadius;
        public float radius = -1.0f;
        public float height;

        public Hill(float x, float z, float xRadius, float zRadius, float height) {
            this.x = x;
            this.z = z;
            this.xRadius = xRadius;
            this.zRadius = zRadius;
            this.height = height;
        }

        public Hill() {
        }
    }
}
