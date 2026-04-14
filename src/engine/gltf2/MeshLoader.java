package engine.gltf2;

import engine.asset.Asset;
import engine.gltf2.schemas.*;
import engine.graphics.MeshData;
import engine.logging.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class MeshLoader {
    private MeshLoader(){}
    private static int getSize(String s) {
        return switch (s) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> 0;
        };
    }
    private static void loadVectorAccessor(int allowedSize, Gltf g, int accessorIndex, List<Float> list, Map<String, ByteBuffer> readers, Matrix4f transform) {
        Accessor accessor = g.accessors[accessorIndex];
        BufferView bufferView = g.bufferViews[accessor.bufferView];
        Buffer buffer = g.buffers[bufferView.buffer];
        int offset = accessor.byteOffset + bufferView.byteOffset;
        int count = accessor.count;

        ByteBuffer data = readers.get(buffer.uri);
        data.position(offset);

        int size = getSize(accessor.type);
        if(size != allowedSize) {
            Logger.error(MeshLoader.class, "Accessor type " + accessor.type + " is not allowed for this type of data");
            size = allowedSize;
        }

        for(int i = 0; i < count; i++) {
            if(size == 3) {
                Vector3f vector = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                transform.transformDirection(vector);

                list.add(vector.x * 0.5f);
                list.add(vector.y * 0.5f);
                list.add(vector.z * 0.5f);
            }
            else if(size == 4) {
                Vector4f vector = new Vector4f(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
                transform.transform(vector);

                list.add(vector.x * 0.5f);
                list.add(vector.y * 0.5f);
                list.add(vector.z * 0.5f);
                list.add(vector.w * 0.5f);
            }
            else {
                for (int j = 0; j < size; j++) {
                    list.add(data.getFloat());
                }
            }

        }
    }
    private static void loadScalarAccessor(Gltf g, int accessorIndex, List<Integer> list, Map<String, ByteBuffer> readers, int vertexCount) {
        Accessor accessor = g.accessors[accessorIndex];
        BufferView bufferView = g.bufferViews[accessor.bufferView];
        Buffer buffer = g.buffers[bufferView.buffer];

        int offset = accessor.byteOffset + bufferView.byteOffset;
        int count = accessor.count;

        ByteBuffer data = readers.get(buffer.uri);
        data.position(offset);
        for(int i = 0; i < count; i++) {
            list.add(vertexCount + (data.getShort() & 0xFFFF));
        }
    }

    private static void openNode(Map<String, ByteBuffer> readers,
                                 Gltf g,
                                 Node node,
                                 Mesh[] meshes,
                                 List<Float> verticesList,
                                 List<Float> colorsList,
                                 List<Float> normalsList,
                                 List<Float> tangentsList,
                                 List<Float> textureUVsList,
                                 List<Integer> indicesList,
                                 Stack<Matrix4f> matrices) {

        Mesh mesh = meshes[node.mesh];

        Matrix4f matrix = new Matrix4f().identity();

        if(node.matrix != null) {
            matrix = new Matrix4f(
                    node.matrix[0], node.matrix[1], node.matrix[2], node.matrix[3],
                    node.matrix[4], node.matrix[5], node.matrix[6], node.matrix[7],
                    node.matrix[8], node.matrix[9], node.matrix[10], node.matrix[11],
                    node.matrix[12], node.matrix[13], node.matrix[14], node.matrix[15]
            );

            matrices.push(matrix);
            matrix = getCombinedMatrix(matrices);
        }



        for(Primitives primitives : mesh.primitives) {

            //Indices
            loadScalarAccessor(g, primitives.indices, indicesList, readers, verticesList.size() / 3);

            //Everything else
            {
                int positionAccessorIndex = primitives.attributes.get("POSITION");
                loadVectorAccessor(3, g, positionAccessorIndex, verticesList, readers, matrix);

                int normalAccessorIndex = primitives.attributes.get("NORMAL");
                loadVectorAccessor(3, g, normalAccessorIndex, normalsList, readers, matrix);

                if(primitives.attributes.get("TEXCOORD_0") == null) {
                    for (int i = 0; i < verticesList.size() / 3; i++) {
                        textureUVsList.add(0f);
                        textureUVsList.add(0f);
                    }
                }
                else {
                    int textureUVAccessorIndex = primitives.attributes.get("TEXCOORD_0");
                    loadVectorAccessor(2, g, textureUVAccessorIndex, textureUVsList, readers, matrix);
                }

                if(primitives.attributes.get("TANGENT") == null) {
                    for (int i = 0; i < verticesList.size() / 3 - 1; i++) {
                        Vector3f normal = new Vector3f(normalsList.get(i * 3), normalsList.get(i * 3 + 1), normalsList.get(i * 3 + 2));
                        Vector3f position1 = new Vector3f(verticesList.get(i * 3), verticesList.get(i * 3 + 1), verticesList.get(i * 3 + 2));
                        Vector3f position2 = new Vector3f(verticesList.get((i + 1) * 3), verticesList.get((i + 1) * 3 + 1), verticesList.get((i + 1) * 3 + 2));

                        Vector3f tangent = new Vector3f(normal).cross(position2.sub(position1)).normalize().mul(-1);

                        matrix.transformDirection(tangent);
                        tangentsList.add(tangent.x);
                        tangentsList.add(tangent.y);
                        tangentsList.add(tangent.z);
                    }
                }
                else {
                    int tangentAccessorIndex = primitives.attributes.get("TANGENT");
                    loadVectorAccessor(3, g, tangentAccessorIndex, tangentsList, readers, matrix);
                }

                if(primitives.attributes.get("COLOR") == null) {
                    for (int i = 0; i < verticesList.size() / 3; i++) {
                        colorsList.add(1f);
                        colorsList.add(1f);
                        colorsList.add(1f);
                        colorsList.add(1f);
                    }
                }



            }







        }

        if(node.children == null) return;
        for(int i : node.children) {
            Node child = g.nodes[i];
            openNode(readers, g, child, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList, matrices);
        }

        if(node.matrix != null) matrices.pop();
    }

    private static Matrix4f getCombinedMatrix(Stack<Matrix4f> matrices) {
        Matrix4f matrix = new Matrix4f();

        for(int i = matrices.size() - 1; i >= 0; i--) {
            matrix.mul(matrices.get(i));
        }

        return matrix;
    }


    public static MeshData loadGLTF2(Asset<String> gltf, Asset<byte[]>... bin){
        Source gltf2 = new Source(gltf, bin);
        GltfParser parser = new GltfParser();
        Gltf g = parser.parse(gltf2);

        Map<String, ByteBuffer> readers = new HashMap<>();
        {
            for (int i = 0; i < bin.length; i++) {
                Asset<byte[]> asset = bin[i];
                ByteBuffer data = ByteBuffer.wrap(asset.getObject());
                data.order(ByteOrder.LITTLE_ENDIAN);
                String[] tokens = asset.getFQN().split("/");
                String key = tokens[tokens.length - 1];
                readers.put(key, data);
            }
        }


        List<Float> verticesList = new ArrayList<>();
        List<Float> colorsList = new ArrayList<>();
        List<Float> normalsList = new ArrayList<>();
        List<Float> tangentsList = new ArrayList<>();
        List<Float> textureUVsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        Scene root = g.scenes[g.scene];
        Stack<Matrix4f> matrices = new Stack<>();

        for(int i : root.nodes) {
            Node node = g.nodes[i];
            openNode(readers, g, node, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList, matrices);
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
