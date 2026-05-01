package engine.ecs;

import engine.graphics.*;
import engine.graphics.pipelines.SceneFeatures;

import java.util.List;

public class RenderSystem extends ActorSystem {
    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;

    public RenderSystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;
    }

    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {
            if(actor.has(ShaderComponent.class)) {
                ShaderComponent shaderComponent = actor.getComponent(ShaderComponent.class);
                if (actor.has(MeshComponent.class)) {
                    setMaterialData(renderer.getFrameIndex(), shaderComponent.shaderProgram(), actor.getComponent(MaterialComponent.class));
                    setSceneDescAndTransformData(renderer.getFrameIndex(), actor.getComponent(MeshComponent.class));
                }
            }
        });






        renderPipeline.getFeatures(SceneFeatures.class).setScene(scene);
        renderPipeline.render(renderer);
    }

    private void setSceneDescAndTransformData(int frameIndex, MeshComponent meshComponent) {
        meshComponent.shaderProgram.setBuffers(
                frameIndex,
                new DescriptorUpdate<>("scene_desc", meshComponent.sceneDescBuffers[frameIndex]),
                new DescriptorUpdate<>("transforms", meshComponent.transformsBuffers[frameIndex])
        );
    }


    private void setMaterialData(int frameIndex, ShaderProgram shaderProgram, MaterialComponent materialComponent) {
        Sampler sampler = materialComponent.material.getSampler();

        List<Texture> textures = materialComponent.material.getTextures();
        for (int i = 0; i < textures.size(); i++) {
            Texture texture = textures.get(i);

            shaderProgram.setTextures(frameIndex, new DescriptorUpdate<>("material", texture).arrayIndex(i));
            shaderProgram.setSamplers(frameIndex, new DescriptorUpdate<>("material_sampler", sampler));

        }


    }

    @Override
    public void dispose() {

    }
}
