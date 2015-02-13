package com.railwaygames.sleddingsmash.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;

/**
 * Created by mpietal on 2/3/15.
 */
public class LevelBuilder {

    public static Model generate(float l, float w) {
        Model model = new Model();

        // Assume we are going to split our plane into chunks this size
        float chunkSize = 10.0f;

        int lengthCount = (int) (l / chunkSize);
        l = (int) (chunkSize * lengthCount);

        int widthCount = (int) (w / chunkSize);
        w = (int) (chunkSize * widthCount);

        int vertexCount = (lengthCount + 1) * (widthCount + 1) * 3;
        float[] vertices = new float[vertexCount];

        int count = 0;
        for (float z = 0; z >= -l; z -= chunkSize) {
            for (float x = 0; x <= w; x += chunkSize) {
                vertices[count++] = x;
                vertices[count++] = 0.0f;
                vertices[count++] = z;
            }
        }

        // subdivide by square, then divide each square into 2 triangles
        int indexCount = widthCount * lengthCount * 6;
        short[] indices = new short[indexCount];
        count = 0;
        for (int r = 0; r < lengthCount; r++) {
            for (int c = 0; c < widthCount; c++) {
                short offset = (short) (r * (widthCount + 1) + c);
                short nextOffset = (short) ((r + 1) * (widthCount + 1) + c);

                // triangle 1
                indices[count++] = offset;
                indices[count++] = (short) (offset + 1);
                indices[count++] = nextOffset;

                // triangle 2
                indices[count++] = (short) (offset + 1);
                indices[count++] = (short) (nextOffset + 1);
                indices[count++] = nextOffset;
            }
        }

        Mesh mesh = new Mesh(false, vertexCount, indexCount, VertexAttribute.Position());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        MeshPart meshPart = new MeshPart("plane", mesh, 0, indexCount, GL20.GL_TRIANGLES);
        meshPart.mesh = mesh;

        model.meshes.add(mesh);
        model.meshParts.add(meshPart);

        NodePart nodePart = new NodePart(meshPart, new Material(ColorAttribute.createDiffuse(Color.GREEN)));

        Node node = new Node();
        node.id = "plane_node";
        node.parts.add(nodePart);
        model.nodes.add(node);

        return model;
    }
}
