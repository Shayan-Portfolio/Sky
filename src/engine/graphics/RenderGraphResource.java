package engine.graphics;

import org.lwjgl.vulkan.VK13;

public class RenderGraphResource<Type> {
    private Type type;
    private int stage = VK13.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
    private int access = 0;

    public RenderGraphResource(Type type) {
        this.type = type;
    }


    public void setPipelineStage(int stage) {
        this.stage = stage;
    }

    public Type get() {
        return type;
    }

    public int getPipelineStage() {
        return stage;
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }
}
