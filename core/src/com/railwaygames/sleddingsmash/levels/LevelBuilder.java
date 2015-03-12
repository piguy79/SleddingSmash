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
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                count += 3;
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

        NodePart nodePart = new NodePart(meshPart, new Material(ColorAttribute.createDiffuse(new Color(0.95f, 0.92f, 0.93f, 1.0f))));

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

            int vertexSize = mesh.getVertexSize() / 4;
            int numVertices = mesh.getNumVertices() * vertexSize;

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

    public static List<Model> createSides(Model model) {
        Mesh mesh = model.meshes.get(0);

        int numIndices = mesh.getNumIndices();
        int numVertices = mesh.getNumVertices() * 3 * 2;

        // divide by 4 b/c size is in bytes
        int newVertexOffset = mesh.getVertexSize() / 4;

        float[] vertices = new float[numVertices];
        mesh.getVertices(vertices);

        short[] indices = new short[numIndices];
        mesh.getIndices(indices);

        /*
         * Locate edges by counting the number of triangles that connect to each index
         */
        Map<Short, Integer> indexCount = new HashMap<Short, Integer>();
        List<Triangle> triangles = new ArrayList<Triangle>();
        for (int i = 0; i < numIndices; i += 3) {
            short one = indices[i];
            short two = indices[i + 1];
            short three = indices[i + 2];

            newCount(indexCount, one);
            newCount(indexCount, two);
            newCount(indexCount, three);

            triangles.add(new Triangle(one, two, three));
        }

        List<Segment> leftEdges = new ArrayList<Segment>();
        List<Segment> rightEdges = new ArrayList<Segment>();
        for (Triangle triangle : triangles) {
            findEdges(vertices, indexCount, triangle.one, triangle.two, triangle.three, leftEdges, rightEdges, newVertexOffset);
            findEdges(vertices, indexCount, triangle.two, triangle.three, triangle.one, leftEdges, rightEdges, newVertexOffset);
            findEdges(vertices, indexCount, triangle.three, triangle.one, triangle.two, leftEdges, rightEdges, newVertexOffset);
        }

        Model leftModel = createSideModel(true, leftEdges, newVertexOffset, vertices);
        Model rightModel = createSideModel(false, rightEdges, newVertexOffset, vertices);

        List<Model> models = new ArrayList<Model>(2);
        models.add(leftModel);
        models.add(rightModel);
        return models;
    }

    public static Model createBackground() {
        Model model = new Model();

        float[] vertices = new float[32];
        short[] indices = new short[6];

        vertices[0] = -5.0f;
        vertices[1] = -5.0f;
        vertices[2] = 0;
        vertices[6] = 0;
        vertices[7] = 1;

        vertices[8] = 5.0f;
        vertices[9] = -5.0f;
        vertices[10] = 0;
        vertices[14] = 1;
        vertices[15] = 1;

        vertices[16] = -5.0f;
        vertices[17] = 5.0f;
        vertices[18] = 0;
        vertices[22] = 0;
        vertices[23] = 0;

        vertices[24] = 5.0f;
        vertices[25] = 5.0f;
        vertices[26] = 0;
        vertices[30] = 1;
        vertices[31] = 0;

        indices[0] = 0;
        indices[1] = 1;
        indices[2] = 2;
        indices[3] = 2;
        indices[4] = 1;
        indices[5] = 3;

        Mesh mesh = new Mesh(false, vertices.length, indices.length, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        MeshPart meshPart = new MeshPart("bg", mesh, 0, indices.length, GL20.GL_TRIANGLES);
        meshPart.mesh = mesh;

        ModelMaterial modelMaterial = new ModelMaterial();
        modelMaterial.id = "bg_one_material";
        modelMaterial.diffuse = new Color(1.0f, 1.0f, 1.0f, 1.0f);

        ModelTexture modelTexture = new ModelTexture();
        modelTexture.id = "bg_one_texture";
        modelTexture.fileName = "data/images/bg/one.png";
        modelTexture.usage = ModelTexture.USAGE_DIFFUSE;
        Array<ModelTexture> modelTextures = new Array<ModelTexture>();
        modelTextures.add(modelTexture);

        modelMaterial.textures = modelTextures;
        List<ModelMaterial> materials = new ArrayList<ModelMaterial>();
        materials.add(modelMaterial);

        try {
            Method method = model.getClass().getDeclaredMethod("loadMaterials", Iterable.class, TextureProvider.class);
            method.setAccessible(true);
            Object r = method.invoke(model, materials, new TextureProvider.FileTextureProvider());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        model.meshes.add(mesh);
        model.meshParts.add(meshPart);

        NodePart nodePart = new NodePart(meshPart, model.getMaterial("bg_one_material"));

        Node node = new Node();
        node.id = "bg_node";
        node.parts.add(nodePart);
        model.nodes.add(node);

        LevelBuilder.calculateNormals(model);
        return model;
    }

    private static Model createSideModel(boolean left, List<Segment> edges, int newVertexOffset, float[] vertices) {
        //reverse sort b/c Z gets smaller
        Collections.sort(edges, new Comparator<Segment>() {
            @Override
            public int compare(Segment o1, Segment o2) {
                return Float.valueOf(o2.oneZ).compareTo(Float.valueOf(o1.oneZ));
            }
        });
        int lengthCount = edges.size();

        int rectWidth = 100;
        int widthCount = 8;
        int width = rectWidth * widthCount;

        int vertexCount = (lengthCount + 1) * (widthCount + 1) * 3 * 2;
        float[] newVertices = new float[vertexCount];

        int mod = 1;
        if (left) {
            mod = -1;
        }

        boolean first = true;
        int v = 0;

        // use interpolators to create some variation in the sides
        Interpolation xInterpolator = Interpolation.fade;
        float xImpact = MathUtils.randomInRange(50.0f, 200.0f);

        for (Segment segment : edges) {
            if (first) {
                first = false;
                float x1 = vertices[segment.one * newVertexOffset];
                float y1 = vertices[segment.one * newVertexOffset + 1];
                float z1 = vertices[segment.one * newVertexOffset + 2];
                for (int x = 0; x <= width; x += rectWidth, v += newVertexOffset) {
                    newVertices[v] = x1 + x * mod;
                    newVertices[v + 1] = y1 + xImpact * xInterpolator.apply((float) x / (float) width);
                    newVertices[v + 2] = z1;
                }
            }
            float x1 = vertices[segment.two * newVertexOffset];
            float y1 = vertices[segment.two * newVertexOffset + 1];
            float z1 = vertices[segment.two * newVertexOffset + 2];
            for (int x = 0; x <= width; x += rectWidth, v += newVertexOffset) {
                newVertices[v] = x1 + x * mod;
                newVertices[v + 1] = y1 + xImpact * xInterpolator.apply((float) x / (float) width);
                newVertices[v + 2] = z1;
            }
        }

        // subdivide by square, then divide each square into 2 triangles
        int indexCount = widthCount * lengthCount * 6;
        short[] indices = new short[indexCount];
        int count = 0;
        for (int r = 0; r < lengthCount - 1; r++) {
            for (int c = 0; c < widthCount; c++) {
                short offset = (short) (r * (widthCount + 1) + c);
                short nextOffset = (short) ((r + 1) * (widthCount + 1) + c);

                // triangle 1
                if (left) {
                    indices[count++] = (short) (offset + 1);
                    indices[count++] = offset;
                } else {
                    indices[count++] = offset;
                    indices[count++] = (short) (offset + 1);
                }
                indices[count++] = nextOffset;

                // triangle 2
                if (left) {
                    indices[count++] = (short) (nextOffset + 1);
                    indices[count++] = (short) (offset + 1);
                } else {
                    indices[count++] = (short) (offset + 1);
                    indices[count++] = (short) (nextOffset + 1);
                }
                indices[count++] = nextOffset;
            }
        }

        Mesh mesh = new Mesh(true, vertexCount, indexCount, VertexAttribute.Position(), VertexAttribute.Normal());
        mesh.setVertices(newVertices);
        mesh.setIndices(indices);

        MeshPart meshPart = new MeshPart("side" + (left ? "left" : "right"), mesh, 0, indexCount, GL20.GL_TRIANGLES);
        meshPart.mesh = mesh;

        Model model = new Model();

        model.meshes.add(mesh);
        model.meshParts.add(meshPart);

        NodePart nodePart = new NodePart(meshPart, new Material(ColorAttribute.createDiffuse(new Color(0.75f, 0.72f, 0.73f, 1.0f))));

        Node node = new Node();
        node.id = "side_node";
        node.parts.add(nodePart);
        model.nodes.add(node);

        return model;
    }

    private static void findEdges(float[] vertices, Map<Short, Integer> indexCount, short one, short two, short three, List<Segment> leftEdges, List<Segment> rightEdges, int newVertexOffset) {
        if (indexCount.get(one) < 6 && indexCount.get(two) < 6) {
            float oneX = vertices[one * newVertexOffset];
            float twoX = vertices[two * newVertexOffset];
            float threeX = vertices[three * newVertexOffset];

            if (threeX < twoX && threeX < oneX) {
                rightEdges.add(new Segment(one, two, vertices[one * newVertexOffset + 2], vertices[two * newVertexOffset + 2]));
            } else if (threeX > twoX && threeX > oneX) {
                leftEdges.add(new Segment(one, two, vertices[one * newVertexOffset + 2], vertices[two * newVertexOffset + 2]));
            }
        }
    }

    private static void newCount(Map<Short, Integer> indexCount, short i) {
        Integer c = indexCount.get(i);
        if (c == null) {
            c = 0;
        }
        indexCount.put(i, c + 1);
    }

    private static class Triangle {
        public short one;
        public short two;
        public short three;

        public Triangle(short one, short two, short three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }
    }

    private static class Segment {
        public short one;
        public short two;
        public float oneZ;
        public float twoZ;

        public Segment(short one, short two, float oneZ, float twoZ) {
            if (oneZ > twoZ) {
                this.one = one;
                this.two = two;
                this.oneZ = oneZ;
                this.twoZ = twoZ;
            } else {
                this.one = two;
                this.two = one;
                this.oneZ = twoZ;
                this.twoZ = oneZ;
            }
        }
    }
}
