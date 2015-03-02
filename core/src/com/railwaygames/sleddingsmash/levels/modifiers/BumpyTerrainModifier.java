package com.railwaygames.sleddingsmash.levels.modifiers;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Interpolation;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.util.Map;

import static com.railwaygames.sleddingsmash.utils.MathUtils.MinMax;
import static com.railwaygames.sleddingsmash.utils.MathUtils.distance;
import static java.lang.Math.abs;

public class BumpyTerrainModifier implements TerrainModifier {

    /**
     * Number of bumps to create
     */
    public static final String COUNT = "count";

    @Override
    public void modify(Model model, Map<String, Object> params) {
        Mesh mesh = model.meshes.get(0);

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[mesh.getNumVertices() * newVertexOffset];
        mesh.getVertices(vertices);

        Map<String, MinMax> minMaxMap = MathUtils.calculateAxisMinMax(vertices, newVertexOffset);
        float xLen = minMaxMap.get("x").max - minMaxMap.get("x").min;
        float zLen = minMaxMap.get("z").max - minMaxMap.get("z").min;

        int count = (Integer) params.get(COUNT);
        for (int i = 0; i < count; ++i) {
            float centerX = (float) Math.random() * xLen;
            float centerZ = (float) Math.random() * -zLen;
            float radius = MathUtils.randomInRange(0.0f, 50.0f);
            float height = MathUtils.randomInRange(-radius * 0.08f, radius * 0.15f);

            for (int j = 0; j < vertices.length; j += newVertexOffset) {
                float x = vertices[j];
                float z = vertices[j + 2];

                float dist = abs(distance(centerX, centerZ, x, z));
                if (dist < radius) {
                    float newY = Interpolation.pow3.apply((radius - dist) / radius) * height;
                    vertices[j + 1] = newY;
                }
            }
        }

        mesh.setVertices(vertices);
    }
}
