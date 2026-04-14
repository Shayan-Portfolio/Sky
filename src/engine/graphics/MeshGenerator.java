package engine.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshGenerator {
    public static MeshData newBox(float width, float height, float depth) {


        float hw = width / 2.0f;
        float hh = height / 2.0f;
        float hd = depth / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();


        Vector3f[][] vertices = {
                // Front
                {
                        new Vector3f(-hw, -hh,  hd),
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f( hw,  hh,  hd),
                        new Vector3f(-hw,  hh,  hd)
                },
                // Back
                {
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f(-hw,  hh, -hd),
                        new Vector3f( hw,  hh, -hd)
                },
                // Left
                {
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f(-hw, -hh,  hd),
                        new Vector3f(-hw,  hh,  hd),
                        new Vector3f(-hw,  hh, -hd)
                },
                // Right
                {
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f( hw,  hh, -hd),
                        new Vector3f( hw,  hh,  hd)
                },
                // Top
                {
                        new Vector3f(-hw,  hh,  hd),
                        new Vector3f( hw,  hh,  hd),
                        new Vector3f( hw,  hh, -hd),
                        new Vector3f(-hw,  hh, -hd)
                },
                // Bottom
                {
                        new Vector3f(-hw, -hh, -hd),
                        new Vector3f( hw, -hh, -hd),
                        new Vector3f( hw, -hh,  hd),
                        new Vector3f(-hw, -hh,  hd)
                }
        };

        float[][] colors = {
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 1, 1}
        };

        Vector2f[] textureUVs = {
                new Vector2f(0.0f, 1.0f),
                new Vector2f(1.0f, 1.0f),
                new Vector2f(1.0f, 0.0f),
                new Vector2f(0.0f, 0.0f),

        };

        int index = 0;
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            Vector3f[] faceVertices = vertices[faceIndex];
            float[] faceColors = colors[faceIndex];

            Vector3f e0 = new Vector3f(faceVertices[1]).sub(faceVertices[0]);
            Vector3f e1 = new Vector3f(faceVertices[2]).sub(faceVertices[0]);

            Vector3f faceNormal = new Vector3f(e0).cross(e1).normalize();
            Vector3f faceTangent = new Vector3f(e0).normalize();


            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                Vector3f vertex = faceVertices[vertexIndex];
                Vector2f uv =  textureUVs[vertexIndex];

                // Position
                verticesList.add(vertex.x);
                verticesList.add(vertex.y);
                verticesList.add(vertex.z);

                // Color
                colorsList.add(faceColors[0]);
                colorsList.add(faceColors[1]);
                colorsList.add(faceColors[2]);
                colorsList.add(faceColors[3]);

                //Normals
                normalsList.add(faceNormal.x);
                normalsList.add(faceNormal.y);
                normalsList.add(faceNormal.z);

                //Tangents
                tangentsList.add(faceTangent.x);
                tangentsList.add(faceTangent.y);
                tangentsList.add(faceTangent.z);

                textureUVsList.add(uv.x);
                textureUVsList.add(uv.y);

            }

            // Indices for the face (2 triangles)
            indicesList.add(index);
            indicesList.add(index + 1);
            indicesList.add(index + 2);
            indicesList.add(index);
            indicesList.add(index + 2);
            indicesList.add(index + 3);

            index += 4;
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", verticesList);
        vertexData.put("Colors", colorsList);
        vertexData.put("Normals", normalsList);
        vertexData.put("TextureUVs", textureUVsList);
        vertexData.put("Tangents", tangentsList);

        return new MeshData(vertexData, indicesList, 24);
    }
    public static MeshData newCylinder(float radius, float height, int segments) {

        float hh = height / 2.0f;

        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        float[] faceColor = {1, 1, 1, 1};

        int index = 0;

        // -------------------------
        // SIDE SURFACE (seam fixed)
        // -------------------------
        for (int i = 0; i <= segments; i++) {

            float theta = (float) (2.0 * Math.PI * i / segments);

            float x = (float) Math.cos(theta) * radius;
            float z = (float) Math.sin(theta) * radius;

            Vector3f normal = new Vector3f(x, 0, z).normalize();
            Vector3f tangent = new Vector3f(-z, 0, x).normalize();

            float u = (float) i / segments;

            // bottom
            verticesList.add(x);
            verticesList.add(-hh);
            verticesList.add(z);

            colorsList.add(faceColor[0]);
            colorsList.add(faceColor[1]);
            colorsList.add(faceColor[2]);
            colorsList.add(faceColor[3]);

            normalsList.add(normal.x);
            normalsList.add(normal.y);
            normalsList.add(normal.z);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(u);
            textureUVsList.add(0f);

            // top
            verticesList.add(x);
            verticesList.add(hh);
            verticesList.add(z);

            colorsList.add(faceColor[0]);
            colorsList.add(faceColor[1]);
            colorsList.add(faceColor[2]);
            colorsList.add(faceColor[3]);

            normalsList.add(normal.x);
            normalsList.add(normal.y);
            normalsList.add(normal.z);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(u);
            textureUVsList.add(1f);

            if (i < segments) {

                int base = index;

                indicesList.add(base);
                indicesList.add(base + 1);
                indicesList.add(base + 3);

                indicesList.add(base);
                indicesList.add(base + 3);
                indicesList.add(base + 2);
            }

            index += 2;
        }

        // -------------------------
        // CAPS
        // -------------------------

        int bottomCenterIndex = index;

        // bottom center
        verticesList.add(0f);
        verticesList.add(-hh);
        verticesList.add(0f);

        colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);
        normalsList.add(0f); normalsList.add(-1f); normalsList.add(0f);
        tangentsList.add(1f); tangentsList.add(0f); tangentsList.add(0f);
        textureUVsList.add(0.5f); textureUVsList.add(0.5f);

        index++;

        int topCenterIndex = index;

        // top center
        verticesList.add(0f);
        verticesList.add(hh);
        verticesList.add(0f);

        colorsList.add(1f); colorsList.add(1f); colorsList.add(1f); colorsList.add(1f);
        normalsList.add(0f); normalsList.add(1f); normalsList.add(0f);
        tangentsList.add(1f); tangentsList.add(0f); tangentsList.add(0f);
        textureUVsList.add(0.5f); textureUVsList.add(0.5f);

        index++;

        int bottomStart = index;

        // ring vertices (duplicate for caps → correct normals)
        for (int i = 0; i < segments; i++) {

            float theta = (float) (2.0 * Math.PI * i / segments);

            float x = (float) Math.cos(theta) * radius;
            float z = (float) Math.sin(theta) * radius;

            Vector3f tangent = new Vector3f(-z, 0, x).normalize();

            // bottom ring
            verticesList.add(x);
            verticesList.add(-hh);
            verticesList.add(z);

            colorsList.add(faceColor[0]);
            colorsList.add(faceColor[1]);
            colorsList.add(faceColor[2]);
            colorsList.add(faceColor[3]);

            normalsList.add(0f);
            normalsList.add(-1f);
            normalsList.add(0f);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(0.5f + x / (2 * radius));
            textureUVsList.add(0.5f + z / (2 * radius));

            index++;

            // top ring
            verticesList.add(x);
            verticesList.add(hh);
            verticesList.add(z);

            colorsList.add(faceColor[0]);
            colorsList.add(faceColor[1]);
            colorsList.add(faceColor[2]);
            colorsList.add(faceColor[3]);

            normalsList.add(0f);
            normalsList.add(1f);
            normalsList.add(0f);

            tangentsList.add(tangent.x);
            tangentsList.add(tangent.y);
            tangentsList.add(tangent.z);

            textureUVsList.add(0.5f + x / (2 * radius));
            textureUVsList.add(0.5f + z / (2 * radius));

            index++;
        }

        // -------------------------
        // CAP INDICES (CW winding)
        // -------------------------
        for (int i = 0; i < segments; i++) {

            int next = (i + 1) % segments;

            int bottom0 = bottomStart + i * 2;
            int bottom1 = bottomStart + next * 2;

            int top0 = bottomStart + i * 2 + 1;
            int top1 = bottomStart + next * 2 + 1;

            // bottom (faces down)
            indicesList.add(bottomCenterIndex);
            indicesList.add(bottom0);
            indicesList.add(bottom1);

            // top (faces up)
            indicesList.add(topCenterIndex);
            indicesList.add(top1);
            indicesList.add(top0);
        }

        Map<String, List<Float>> vertexData = new HashMap<>();
        vertexData.put("Positions", verticesList);
        vertexData.put("Colors", colorsList);
        vertexData.put("Normals", normalsList);
        vertexData.put("TextureUVs", textureUVsList);
        vertexData.put("Tangents", tangentsList);

        return new MeshData(vertexData, indicesList, verticesList.size() / 3);
    }
}
