package engine.graphics;

import engine.ecs.RenderSystem;

import java.util.List;

public record SceneRenderData(
        Camera sceneCamera,
        List<RenderSystem.RenderItem> drawCalls,
        List<LightData> lights) {
}
