package engine.gltf2;

import engine.asset.Asset;
import engine.gltf2.schemas.*;
import engine.graphics.MeshData;
import engine.logging.Logger;
import org.joml.*;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Importer {
    private Importer(){}
    private static int getSize(String s) {
        return switch (s) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> 0;
        };
    }
    private static void loadVectorAccessor(String name, int allowedSize, Gltf g, int accessorIndex, List<Float> list, Map<String, ByteBuffer> readers, Matrix4f transform, float scale, boolean translate) {
        Accessor accessor = g.accessors[accessorIndex];
        BufferView bufferView = g.bufferViews[accessor.bufferView];
        Buffer buffer = g.buffers[bufferView.buffer];
        int offset = accessor.byteOffset + bufferView.byteOffset;
        int count = accessor.count;

        ByteBuffer data = readers.get(buffer.uri);
        data.position(offset);

        int size = getSize(accessor.type);
        if(size != allowedSize) {
            Logger.info(
                    Importer.class,
                    name + " accessor type " + accessor.type + " does not match allowed size of " + allowedSize
            );
        }

        for(int i = 0; i < count; i++) {
            if(allowedSize == 3) {
                Vector4f vector = new Vector4f(data.getFloat(), data.getFloat(), data.getFloat(), translate ? 1 : 0);

                transform.transform(vector);

                list.add(vector.x * scale);
                list.add(vector.y * scale);
                list.add(vector.z * scale);
            }
            else if(allowedSize == 4) {
                Vector4f vector = new Vector4f(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
                transform.transform(vector);

                list.add(vector.x * scale);
                list.add(vector.y * scale);
                list.add(vector.z * scale);
                list.add(vector.w);
            }
            else {
                for (int j = 0; j < allowedSize; j++) {
                    list.add(data.getFloat());
                }
            }

            for (int j = 0; j < size - allowedSize; j++) data.getFloat();

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

    private static void openNode(float scale,
                                 Map<String, ByteBuffer> readers,
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

        Matrix4f matrix = new Matrix4f().identity();

        if(node.matrix != null) {
            matrix = new Matrix4f(
                    node.matrix[0], node.matrix[1], node.matrix[2], node.matrix[3],
                    node.matrix[4], node.matrix[5], node.matrix[6], node.matrix[7],
                    node.matrix[8], node.matrix[9], node.matrix[10], node.matrix[11],
                    node.matrix[12], node.matrix[13], node.matrix[14], node.matrix[15]
            );
        }


        if(node.translation != null) {
            Vector3f translation = new Vector3f(node.translation[0], node.translation[1], node.translation[2]);
            matrix.translate(translation);
        }

        if(node.rotation != null) {
            Quaternionf rotation = new Quaternionf(node.rotation[0], node.rotation[1], node.rotation[2], node.rotation[3]);
            matrix.rotate(rotation);
        }

        if(node.scale != null) {
            Vector3f s = new Vector3f(node.scale[0], node.scale[1], node.scale[2]);
            matrix.scale(s);
        }







        matrices.push(matrix);
        matrix = getCombinedMatrix(matrices);

        if(node.mesh != -1) {
            Mesh mesh = meshes[node.mesh];


            int vertexCount = verticesList.size() / 3;
            int oldTriangleCount = indicesList.size() / 3;

            for (Primitives primitives : mesh.primitives) {

                //Indices
                loadScalarAccessor(g, primitives.indices, indicesList, readers, vertexCount);

                //Everything else
                {
                    int positionAccessorIndex = primitives.attributes.get("POSITION");
                    loadVectorAccessor("POSITION", 3, g, positionAccessorIndex, verticesList, readers, matrix, scale, true);

                    int normalAccessorIndex = primitives.attributes.get("NORMAL");
                    loadVectorAccessor("NORMAL",3, g, normalAccessorIndex, normalsList, readers, matrix, 1, false);

                    if (primitives.attributes.get("TEXCOORD_0") == null) {
                        for (int i = 0; i < verticesList.size() / 3; i++) {
                            float nx = normalsList.get(i * 3);
                            float ny = normalsList.get(i * 3 + 1);
                            float nz = normalsList.get(i * 3 + 2);

                            float px = verticesList.get(i * 3);
                            float py = verticesList.get(i * 3 + 1);
                            float pz = verticesList.get(i * 3 + 2);

                            float ax = Math.abs(nx);
                            float ay = Math.abs(ny);
                            float az = Math.abs(nz);

                            float u, v;

                            // dominant axis projection
                            if (ax > ay && ax > az) {
                                // X dominant → project YZ
                                u = py;
                                v = pz;
                            }
                            else if (ay > az) {
                                // Y dominant → project XZ
                                u = px;
                                v = pz;
                            }
                            else {
                                // Z dominant → project XY
                                u = px;
                                v = py;
                            }

                            textureUVsList.add(u);
                            textureUVsList.add(v);
                        }
                    } else {
                        int textureUVAccessorIndex = primitives.attributes.get("TEXCOORD_0");
                        loadVectorAccessor("TEXCOORD_0", 2, g, textureUVAccessorIndex, textureUVsList, readers, matrix, 1, false);
                    }

                    if (primitives.attributes.get("TANGENT") == null) {
                        int triangleCount = indicesList.size() / 3;

                        Matrix4f inverseMatrix = new Matrix4f(matrix).invert();

                        for (int triangleIndex = oldTriangleCount; triangleIndex < triangleCount; triangleIndex++) {
                            int index1 = indicesList.get(triangleIndex * 3);
                            int index2 = indicesList.get(triangleIndex * 3 + 1);
                            int index3 = indicesList.get(triangleIndex * 3 + 2);


                            Vector4f pos1 = new Vector4f(
                                    verticesList.get(index1 * 3),
                                    verticesList.get(index1 * 3 + 1),
                                    verticesList.get(index1 * 3 + 2),
                                    1);
                            Vector4f pos2 = new Vector4f(
                                    verticesList.get(index2 * 3),
                                    verticesList.get(index2 * 3 + 1),
                                    verticesList.get(index2 * 3 + 2),
                                    1);
                            Vector4f pos3 = new Vector4f(
                                    verticesList.get(index3 * 3),
                                    verticesList.get(index3 * 3 + 1),
                                    verticesList.get(index3 * 3 + 2),
                                    1);


                            pos1.mul(inverseMatrix);
                            pos2.mul(inverseMatrix);
                            pos3.mul(inverseMatrix);


                            Vector2f uv1 = new Vector2f(
                                    textureUVsList.get(index1 * 2),
                                    textureUVsList.get(index1 * 2 + 1)
                            );
                            Vector2f uv2 = new Vector2f(
                                    textureUVsList.get(index2 * 2),
                                    textureUVsList.get(index2 * 2 + 1)
                            );
                            Vector2f uv3 = new Vector2f(
                                    textureUVsList.get(index3 * 2),
                                    textureUVsList.get(index3 * 2 + 1)
                            );



                            Vector4f edge1 = pos2.sub(pos1);
                            Vector4f edge2 = pos3.sub(pos1);

                            Vector2f dUV1 = uv2.sub(uv1);
                            Vector2f dUV2 = uv3.sub(uv1);

                            float f = 1.0f / (dUV1.x * dUV2.y - dUV2.x * dUV1.y);
                            if(Float.isNaN(f)) {
                                System.out.println("Uh oh!");
                            }

                            Vector3f tangent = new Vector3f();

                            tangent.x = (dUV2.y * edge1.x - dUV1.y * edge2.x);
                            tangent.y = (dUV2.y * edge1.y - dUV1.y * edge2.y);
                            tangent.z = (dUV2.y * edge1.z - dUV1.y * edge2.z);
                            tangent.mul(f);

                            tangentsList.add(tangent.x);
                            tangentsList.add(tangent.y);
                            tangentsList.add(tangent.z);

                            tangentsList.add(tangent.x);
                            tangentsList.add(tangent.y);
                            tangentsList.add(tangent.z);






                        }
                    } else {
                        int tangentAccessorIndex = primitives.attributes.get("TANGENT");
                        loadVectorAccessor("TANGENT", 3, g, tangentAccessorIndex, tangentsList, readers, matrix, 1, false);
                    }


                    if (primitives.attributes.get("COLOR") == null) {
                        for (int i = 0; i < verticesList.size() / 3; i++) {
                            colorsList.add(1f);
                            colorsList.add(1f);
                            colorsList.add(1f);
                            colorsList.add(1f);
                        }
                    }


                }
            }
        }

        if(node.children != null) {
            for (int i : node.children) {
                Node child = g.nodes[i];
                openNode(scale, readers, g, child, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList, matrices);
            }
        }

        matrices.pop();
    }

    private static Matrix4f getCombinedMatrix(Stack<Matrix4f> matrices) {
        Matrix4f matrix = new Matrix4f();

        for(int i = 0; i < matrices.size(); i++) {
            matrix.mul(matrices.get(i));
        }

        return matrix;
    }


    public static MeshData loadGLTF2(float scale, Asset<String> gltf, Asset<byte[]>... bin){
        Source gltf2 = new Source(gltf, bin);
        Loader parser = new Loader();
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
            openNode(scale, readers, g, node, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList, matrices);
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
