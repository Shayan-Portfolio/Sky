package engine.gltf2.schemas;

public class Gltf {
    public Asset asset;
    public int scene = -1;
    public Scene[] scenes;
    public Node[] nodes;
    public Buffer[] buffers;
    public BufferView[] bufferViews;
    public Accessor[] accessors;
    public Mesh[] meshes;
}
