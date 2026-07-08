package engine.graphics;

import engine.wsi.Surface;

public abstract class RenderContext {
    public abstract void readyDisplay(Surface surface, RendererSettings settings);
}
