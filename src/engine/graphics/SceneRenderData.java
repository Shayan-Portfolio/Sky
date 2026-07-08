package engine.graphics;

import engine.ecs.RenderSystem;

import java.util.List;

public record SceneRenderData(Camera sceneCamera, List<RenderSystem.IndexedDrawCall> drawCalls, List<LightData> lights, Buffer uiVertexBuffer, Buffer uiIndexBuffer, ShaderProgram uiShaderProgram, int uiQuadCount, Camera uiCamera) {
}
