package engine.graphics;

import java.nio.ByteBuffer;

public abstract class GraphicsPass extends Pass {
    protected RenderTarget renderTarget;
    protected ShaderProgram shaderProgram;

    public GraphicsPass(Disposable parent, String name, int framesInFlight) {
        super(parent, name, framesInFlight);
    }

    public RenderTarget getRenderTarget() {
        return renderTarget;
    }

    public abstract void startRendering(RenderTarget renderTarget, int unusedAttachmentCount, int width, int height, boolean clear, Color clearColor);
    public abstract void endRendering();

    public abstract void setCullMode(CullMode cullMode);
    public abstract void setDrawBuffers(Buffer vertexBuffer, Buffer indexBuffer);
    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void setPushConstants(ByteBuffer pPushConstants);
    public abstract void drawInstanced(int indexCount, int instanceCount);
    public abstract void drawIndexed(int indexCount);
}
