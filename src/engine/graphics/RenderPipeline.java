package engine.graphics;

import engine.ecs.RenderSystem;
import engine.logging.SkyRuntimeException;
import engine.graphics.pipelines.Features;

import java.util.List;

public abstract class RenderPipeline {
    protected RenderGraph graph;
    protected List<Features> supportedFeatures;
    public abstract void init(Renderer renderer);
    public abstract List<Pass> buildFrame(ScenePack scenePack);

    public <T> T getFeatures(Class<T> c) {

        for(Features features : supportedFeatures){
            if(features.getClass() == c || c.isAssignableFrom(features.getClass())) {
                return c.cast(features);
            }
        }

        throw new SkyRuntimeException("This pipeline does not support the feature set " + c.getName());
    }

}
