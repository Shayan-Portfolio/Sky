package engine.graphics;

import engine.gameui.TextureBindings;
import engine.gameui.Theme;
import engine.gameui.Compositor;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

public abstract class RenderPipeline {
    protected int wireframeVertexCount = 0;
    protected int maxWireframeLines = 100000;
    protected int maxUIQuads = 6000;
    protected RenderGraph graph;

    protected int uiQuadCount = 0;
    protected Camera uiCamera;
    protected TextureBindings textureBindings;
    protected Theme uiTheme;
    protected Matrix4f uiTransform = new Matrix4f();
    protected Vector4f origin = new Vector4f();
    protected Compositor compositor;
    public abstract void init(SceneRenderer sceneRenderer);
    public abstract List<Pass> buildFrame(SceneRenderData sceneRenderData);
    public Compositor getCompositor() {
        return compositor;
    }
}
