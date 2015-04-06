package com.railwaygames.sleddingsmash.levels.modifiers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Interpolation;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.railwaygames.sleddingsmash.utils.MathUtils.distance;
import static java.lang.Math.abs;

public class BumpyTerrainModifier implements TerrainModifier {

    /**
     * Number of bumps to create
     */
    public static final String COUNT = "count";

    /**
     * Precomputed hills list
     */
    public static final String HILLS = "hills";

    @Override
    public void modify(Model model, Map<String, Object> params) {
        Mesh mesh = model.meshes.get(0);

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[mesh.getNumVertices() * newVertexOffset];
        mesh.getVertices(vertices);

        List<Hill> hills = (List<Hill>) params.get(HILLS);
        for (Hill hill : hills) {
            for (int j = 0; j < vertices.length; j += newVertexOffset) {
                float x = vertices[j];
                float z = vertices[j + 2];

                float dist = abs(distance(hill.x, hill.z, x, z));
                if (dist < hill.radius) {
                    float newY = Interpolation.pow3.apply((hill.radius - dist) / hill.radius) * hill.height;
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

            hills.add(new Hill(x, z, radius, height));
        }

        return hills;
    }

    public static class Hill {
        public float x;
        public float z;
        public float radius;
        public float height;

        public Hill(float x, float z, float radius, float height) {
            this.x = x;
            this.z = z;
            this.radius = radius;
            this.height = height;
        }
    }
}
