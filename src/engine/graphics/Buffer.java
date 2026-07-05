package engine.graphics;

import engine.logging.SkyRuntimeException;
import engine.graphics.vulkan.VulkanBuffer;

import java.nio.ByteBuffer;

public abstract class Buffer extends Disposable {
    public enum Type {
        GPULocal,
        CPUGPUShared
    }

    public enum Usage {
        VertexBuffer,
        IndexBuffer,
        UniformBuffer,
        ShaderStorageBuffer,
        ImageBackingBuffer
    }

    protected Usage usage;
    protected Type type;

    protected int sizeBytes;
    protected boolean mapped;
    protected boolean staging;
    protected ByteBuffer data;
    protected Fence copyFence;

    public Buffer(Disposable parent, int sizeBytes, Usage usage, Type type, boolean staging){
        super(parent);
        this.sizeBytes = sizeBytes;
        this.usage = usage;
        this.type = type;
        this.staging = staging;
    }

    public Usage getUsage() {
        return usage;
    }

    public Type getType() {
        return type;
    }

    public ByteBuffer get(){
        if(!mapped) data = map();
        return data;
    }

    public ByteBuffer map(){

        if(mapped) {
            throw new SkyRuntimeException("This buffer is already mapped. Use get() instead");
        }
        mapped = true;

        return null;
    }

    public abstract void copyTo(Buffer target, int srcOffset, int dstOffset, int size);

    public boolean isMapped() {
        return mapped;
    }

    public void unmap(){
        mapped = false;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public static Buffer newBuffer(Disposable parent, int sizeBytes, Usage usage, Type type, boolean staging){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanBuffer(parent, sizeBytes, usage, type, staging);

        return null;
    }

}
