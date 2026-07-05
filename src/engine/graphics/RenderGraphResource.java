package engine.graphics;

public class RenderGraphResource<Type> {
    private Type type;
    public RenderGraphResource(Type type) {
        this.type = type;
    }
    public Type get() {
        return type;
    }
}
