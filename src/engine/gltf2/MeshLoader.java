package engine.gltf2;

import engine.asset.Asset;
import engine.gltf2.schemas.*;
import engine.graphics.MeshData;
import engine.logging.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private static void loadVectorAccessor(int allowedSize, Gltf g, int accessorIndex, List<Float> list, Map<String, ByteBuffer> readers) {
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
            for(int j = 0; j < size; j++) {
                list.add(data.getFloat());
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
                                 List<Integer> indicesList) {

        Mesh mesh = meshes[node.mesh];

        for(Primitives primitives : mesh.primitives) {

            //Indices
            loadScalarAccessor(g, primitives.indices, indicesList, readers, verticesList.size() / 3);

            //Everything else
            {
                int positionAccessorIndex = primitives.attributes.get("POSITION");
                loadVectorAccessor(3, g, positionAccessorIndex, verticesList, readers);

                int normalAccessorIndex = primitives.attributes.get("NORMAL");
                loadVectorAccessor(3, g, normalAccessorIndex, normalsList, readers);

                if(primitives.attributes.get("COLOR") == null) {
                    for (int i = 0; i < verticesList.size() / 3; i++) {
                        colorsList.add(1f);
                        colorsList.add(1f);
                        colorsList.add(1f);
                        colorsList.add(1f);

                        textureUVsList.add(0f);
                        textureUVsList.add(0f);
                    }
                }

                //These calculations are wrong
                if(primitives.attributes.get("TANGENT") == null) {
                    for (int i = 0; i < verticesList.size() / 3; i++) {
                        Vector3f normal = new Vector3f(normalsList.get(i * 3), normalsList.get(i * 3 + 1), normalsList.get(i * 3 + 2));
                        Vector3f position = new Vector3f(verticesList.get(i * 3), verticesList.get(i * 3 + 1), verticesList.get(i * 3 + 2));

                        Vector3f tangent = new Vector3f(normal).cross(position).normalize().mul(-1);

                        tangentsList.add(tangent.x);
                        tangentsList.add(tangent.y);
                        tangentsList.add(tangent.z);
                    }
                }
                else {
                    int tangentAccessorIndex = primitives.attributes.get("TANGENT");
                    loadVectorAccessor(3, g, tangentAccessorIndex, tangentsList, readers);
                }



            }







        }

        if(node.children == null) return;
        for(int i : node.children) {
            Node child = g.nodes[i];
            openNode(readers, g, child, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList);
        }

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

        for(int i : root.nodes) {
            Node node = g.nodes[i];
            openNode(readers, g, node, g.meshes, verticesList, colorsList, normalsList, tangentsList, textureUVsList, indicesList);
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
