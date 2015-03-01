package com.railwaygames.sleddingsmash.utils;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by conormullen on 2/18/15.
 */
public class ModelUtils {

    public static List<Vector3> findAreaInModel(Model model, RectangleArea area, Vector3 upVector, float allowedAngle) {
        Mesh mesh = model.meshes.get(0);

        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[mesh.getNumVertices() * newVertexOffset];
        mesh.getVertices(vertices);

        List<Vector3> locationsInBounds = new ArrayList<Vector3>();

        Map<String, MathUtils.MinMax> axisMinMax = MathUtils.calculateAxisMinMax(vertices, newVertexOffset);

        MathUtils.MinMax xAxis = axisMinMax.get("x");
        MathUtils.MinMax zAxis = axisMinMax.get("z");


        float maxX = xAxis.min + (xAxis.axisSize() * area.xEndPercent);
        float minX = xAxis.min + (xAxis.axisSize() * area.xStartPercent);
        float maxZ = zAxis.min + (zAxis.axisSize() * area.zEndPercent);
        float minZ = zAxis.min + (zAxis.axisSize() * area.zStartPercent);


        for (int i = 0; i < vertices.length; i += newVertexOffset) {
            if (inBounds(vertices[i], maxX, minX) && inBounds(vertices[i + 2], maxZ, minZ)) {
                float normalX = vertices[i + 3];
                float normalY = vertices[i + 4];
                float normalZ = vertices[i + 5];

                double angle = Math.toDegrees(Math.acos(new Vector3(normalX, normalY, normalZ).dot(upVector)));

                if (angle < allowedAngle) {
                    locationsInBounds.add(new Vector3(vertices[i], vertices[i + 1], vertices[i + 2]));
                }
            }
        }


        return locationsInBounds;
    }

    private static boolean inBounds(float vertex, float max, float min) {
        if (vertex <= max && vertex >= min) {
            return true;
        }
        return false;
    }

    public static class RectangleArea {
        public float xStartPercent;
        public float zStartPercent;
        public float xEndPercent;
        public float zEndPercent;

        public RectangleArea(float xStartPercent, float zStartPercent, float xEndPercent, float zEndPercent) {
            this.xStartPercent = xStartPercent;
            this.zStartPercent = zStartPercent;
            this.xEndPercent = xEndPercent;
            this.zEndPercent = zEndPercent;
        }
    }


}
