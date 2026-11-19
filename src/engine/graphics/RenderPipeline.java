package engine.graphics;

import java.util.List;

public abstract class RenderPipeline {
    protected RenderGraph graph;
    public abstract void init(SceneRenderer sceneRenderer);
    public abstract List<Pass> buildFrame(SceneRenderData sceneRenderData);
    public abstract ShaderProgram getUIShaderProgram();
    public abstract ShaderProgram getWireframeShaderProgram();
}
