package engine.graphics;
import org.joml.Vector4f;

import java.util.*;


public class MeshData {
    private Map<String, List<Float>> data;
    private int vertexCount;
    private List<Integer> indices;
    private Vector4f sphereMaxPoint = new Vector4f(0, 0, 0, 1);


    public MeshData(Map<String, List<Float>> data, List<Integer> indices, int vertexCount) {
        this.data = data;
        this.indices = indices;
        this.vertexCount = vertexCount;

        List<Float> positions = data.get("Positions");

        for (int i = 0; i < vertexCount; i++) {
            float x = positions.get(i * 3);
            float y = positions.get(i * 3 + 1);
            float z = positions.get(i * 3 + 2);

            if(sphereMaxPoint.length() < Vector4f.length(x, y, z, 1))
                sphereMaxPoint.set(x, y, z, 1);
        }

    }

    public Vector4f getSphereMaxPoint() {
        return sphereMaxPoint;
    }

    public Map<String, List<Float>> getData() {
        return data;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public int getVertexCount() {
        return vertexCount;
    }
    public int getIndexCount() {
        return indices.size();
    }
}
