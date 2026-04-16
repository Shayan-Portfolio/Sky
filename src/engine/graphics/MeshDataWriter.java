package engine.graphics;

import engine.logging.SkyRuntimeException;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

public class MeshDataWriter {

    public static void upload(MeshData meshData, ShaderProgram shaderProgram, ByteBuffer vertexBufferData, ByteBuffer indexBufferData, int vertexOffset) {

        List<Float> positions = meshData.getData().get("Positions");
        List<Float> textureUVs = meshData.getData().get("TextureUVs");
        List<Float> colors = meshData.getData().get("Colors");
        List<Float> normals = meshData.getData().get("Normals");
        List<Float> tangents = meshData.getData().get("Tangents");

        for (int vertexIndex = 0; vertexIndex < meshData.getVertexCount(); vertexIndex++) {


            for(VertexAttribute vertexAttribute : shaderProgram.getVertexAttributes()) {
                switch (vertexAttribute.getName()) {
                    case "vertex.pos_ms" -> {
                        float x = positions.get(3 * vertexIndex + 0);
                        float y = positions.get(3 * vertexIndex + 1);
                        float z = positions.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(x);
                        vertexBufferData.putFloat(y);
                        vertexBufferData.putFloat(z);
                    }
                    case "vertex.uv_ts" -> {
                        float u = textureUVs.get(2 * vertexIndex + 0);
                        float v = textureUVs.get(2 * vertexIndex + 1);

                        vertexBufferData.putFloat(u);
                        vertexBufferData.putFloat(v);
                    }
                    case "vertex.color" -> {
                        float r = colors.get(4 * vertexIndex + 0);
                        float g = colors.get(4 * vertexIndex + 1);
                        float b = colors.get(4 * vertexIndex + 2);
                        float a = colors.get(4 * vertexIndex + 3);

                        vertexBufferData.putFloat(r);
                        vertexBufferData.putFloat(g);
                        vertexBufferData.putFloat(b);
                        vertexBufferData.putFloat(a);
                    }
                    case "vertex.normal_ms" -> {
                        float nx = normals.get(3 * vertexIndex + 0);
                        float ny = normals.get(3 * vertexIndex + 1);
                        float nz = normals.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(nx);
                        vertexBufferData.putFloat(ny);
                        vertexBufferData.putFloat(nz);
                    }
                    case "vertex.tangent_ms" -> {
                        float tx = tangents.get(3 * vertexIndex + 0);
                        float ty = tangents.get(3 * vertexIndex + 1);
                        float tz = tangents.get(3 * vertexIndex + 2);

                        vertexBufferData.putFloat(tx);
                        vertexBufferData.putFloat(ty);
                        vertexBufferData.putFloat(tz);
                    }
                    default -> {
                        for(int i = 0; i < vertexAttribute.getSize(); i++)
                            vertexBufferData.putFloat(0);
                    }
                }
            }

        }

        try {
            for (int index : meshData.getIndices()) {
                indexBufferData.putInt(vertexOffset + index);
            }
        }
        catch (BufferOverflowException e) {
            throw new SkyRuntimeException("Index buffer overflow at index " + indexBufferData.position());
        }
    }


}
