package engine.graphics;

public class RenderGraphResource<Type> {
    private Type type;
    private Pass outboundFrom;

    public RenderGraphResource(Type type) {
        this.type = type;
    }


    public void setOutboundFrom(Pass outboundFrom) {
        this.outboundFrom = outboundFrom;
    }

    public Type get() {
        return type;
    }

    public Pass getOutboundFrom() {
        return outboundFrom;
    }
}
