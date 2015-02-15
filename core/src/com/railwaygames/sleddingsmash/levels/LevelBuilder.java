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
import com.badlogic.gdx.math.Vector3;

public class LevelBuilder {

    public static Model generate(float w, float l) {
        Model model = new Model();

        // Assume we are going to split our plane into chunks this size
        float chunkSize = 10.0f;

        int lengthCount = (int) (l / chunkSize);
        l = (int) (chunkSize * lengthCount);

        int widthCount = (int) (w / chunkSize);
        w = (int) (chunkSize * widthCount);

        int vertexCount = (lengthCount + 1) * (widthCount + 1) * 3 * 2;
        float[] vertices = new float[vertexCount];

        int count = 0;
        for (float z = 0; z >= -l; z -= chunkSize) {
            for (float x = 0; x <= w; x += chunkSize) {
                vertices[count++] = x;
                vertices[count++] = 0.0f;
                vertices[count++] = z;

                // skip normals until all terrain modifiers have been run
                count++;
                count++;
                count++;
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

        Mesh mesh = new Mesh(false, vertexCount, indexCount, VertexAttribute.Position(), VertexAttribute.Normal());
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

    /**
     * Given the current model with vertices and indices, will calculate the normals for each
     *
     * @param model
     */
    public static void calculateNormals(Model model) {
        for (Mesh mesh : model.meshes) {
            int numIndices = mesh.getNumIndices();
            int numVertices = mesh.getNumVertices() * 3 * 2;

            int vertexSize = mesh.getVertexSize() / 4;

            float[] vertices = new float[numVertices];
            mesh.getVertices(vertices);

            short[] indices = new short[numIndices];
            mesh.getIndices(indices);

            Vector3 one = new Vector3();
            Vector3 two = new Vector3();
            Vector3 twoCopy = new Vector3();
            Vector3 three = new Vector3();
            for (int i = 0; i < numIndices; i += 3) {
                int vertexOffset = indices[i] * vertexSize;
                one.set(vertices[vertexOffset], vertices[vertexOffset + 1], vertices[vertexOffset + 2]);

                vertexOffset = indices[i + 1] * vertexSize;
                two.set(vertices[vertexOffset], vertices[vertexOffset + 1], vertices[vertexOffset + 2]);

                vertexOffset = indices[i + 2] * vertexSize;
                three.set(vertices[vertexOffset], vertices[vertexOffset + 1], vertices[vertexOffset + 2]);

                one.sub(two).crs(twoCopy.set(two).sub(three)).nor();

                vertexOffset = indices[i] * vertexSize + 3;
                vertices[vertexOffset] = one.x;
                vertices[vertexOffset + 1] = one.y;
                vertices[vertexOffset + 2] = one.z;

                vertexOffset = indices[i + 1] * vertexSize + 3;
                vertices[vertexOffset] = one.x;
                vertices[vertexOffset + 1] = one.y;
                vertices[vertexOffset + 2] = one.z;

                vertexOffset = indices[i + 2] * vertexSize + 3;
                vertices[vertexOffset] = one.x;
                vertices[vertexOffset + 1] = one.y;
                vertices[vertexOffset + 2] = one.z;
            }

            mesh.setVertices(vertices);
        }
    }
}
