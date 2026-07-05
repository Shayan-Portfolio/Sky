package engine.graphics;
import java.util.*;


public class MeshData {
    private Map<String, List<Float>> data;
    private int vertexCount;
    private List<Integer> indices;


    public MeshData(Map<String, List<Float>> data, List<Integer> indices, int vertexCount) {
        this.data = data;
        this.indices = indices;
        this.vertexCount = vertexCount;
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
