package engine.graphics;

import java.util.List;

public abstract class RenderPipeline {
    protected RenderGraph graph;
    public abstract void init(Renderer renderer);
    public abstract List<Pass> buildFrame(ScenePack scenePack);

}
