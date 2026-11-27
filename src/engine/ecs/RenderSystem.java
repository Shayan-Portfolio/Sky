package engine.ecs;

import engine.gameui.*;
import engine.util.MathUtil;
import engine.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.List;

public class RenderSystem extends ActorSystem {
    private SceneRenderer sceneRenderer;
    private RenderPipeline renderPipeline;
    private Camera activeCamera;
    private List<RenderItem> items = new java.util.LinkedList<>();
    private List<LightData> lights = new java.util.LinkedList<>();





    public static class RenderItem {
        public Buffer vertexBuffer;
        public Buffer indexBuffer;
        public Material material;
        public ShaderProgram shaderProgram;
        public boolean visible;
        public int instanceCount;
        public int indexCount;
        public Buffer[] transformsBuffers;
        public Actor actor;
        public Vector3f sphereCenter, sphereMaxPoint;



        public RenderItem(Buffer vertexBuffer,
                          Buffer indexBuffer,
                          Material material,
                          ShaderProgram shaderProgram,
                          boolean visible,
                          int instanceCount,
                          int indexCount,
                          Buffer[] transformsBuffers,
                          Actor actor,
                          Vector3f sphereCenter,
                          Vector3f sphereMaxPoint) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.material = material;
            this.shaderProgram = shaderProgram;
            this.visible = visible;
            this.instanceCount = instanceCount;
            this.indexCount = indexCount;
            this.transformsBuffers = transformsBuffers;
            this.actor = actor;
            this.sphereCenter = sphereCenter;
            this.sphereMaxPoint = sphereMaxPoint;
        }
    }

    public RenderSystem(SceneRenderer sceneRenderer, RenderPipeline renderPipeline) {
        this.sceneRenderer = sceneRenderer;
        this.renderPipeline = renderPipeline;
    }



    @Override
    public void run(Actor root) {
        items.clear();
        lights.clear();


        root.previsitAllActors(actor -> {
            if(actor.has(CameraComponent.class)) {
                CameraComponent cameraComponent = actor.getComponent(CameraComponent.class);
                Camera newCamera = actor.getComponent(CameraComponent.class).camera;
                if(cameraComponent.camera.isActive())
                    activeCamera = newCamera;
                //else
                    //drawFrustum(newCamera.getInvView(), newCamera.getInvProj(), newCamera.getDebugColor());
            }
        });

        Vector3f cameraPosition = activeCamera.getPosition(new Vector3f());
        Vector3f[] frustum = MathUtil.getFrustum(activeCamera.getInvView(), activeCamera.getInvProj(), 1);


        root.previsitAllActors(actor -> {
            if(actor.has(MeshComponent.class) && actor.has(MaterialComponent.class) && actor.has(ShaderComponent.class) && actor.has(TransformComponent.class)) {
                MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                MaterialComponent materialComponent = actor.getComponent(MaterialComponent.class);
                ShaderComponent shaderComponent = actor.getComponent(ShaderComponent.class);
                TransformComponent transformComponent = actor.getComponent(TransformComponent.class);

                Vector3f sphereCenter, sphereMaxPoint;

                if(meshComponent.instances > 1) {
                    sphereCenter = new Vector3f();
                    sphereMaxPoint = new Vector3f();
                    for (int i = 0; i < meshComponent.instances; i++) {
                        Matrix4f t = transformComponent.transforms()[i];
                        sphereCenter.add(
                                t.m30(),
                                t.m31(),
                                t.m32()
                        );
                    }
                    sphereCenter.div(meshComponent.instances);


                    for (int i = 0; i < meshComponent.instances; i++) {
                        Matrix4f transform = transformComponent.transforms()[i];
                        Vector3f pos = transformComponent.getPosition(i, new Vector3f());


                        Vector3f max = transform.transform(meshComponent.getMeshData().getSphereMaxPoint(), new Vector4f()).xyz(new Vector3f());

                        float r = max.distance(pos);

                        if(sphereCenter.distance(pos) + r > sphereCenter.distance(sphereMaxPoint)) {
                            sphereMaxPoint = pos.normalize(pos.length() + r);
                        }

                    }



                }
                else {
                    sphereCenter = transformComponent.getPosition(0, new Vector3f());
                    sphereMaxPoint = transformComponent.transforms()[0].transform(
                            meshComponent.getMeshData().getSphereMaxPoint(),
                            new Vector4f()
                    ).xyz(new Vector3f());
                }


                float radius = sphereCenter.distance(sphereMaxPoint);


                if(meshComponent.isVisible()) {

                    if(MathUtil.isSphereInFrustum(sphereCenter, radius, cameraPosition, frustum)) {
                        items.add(new RenderItem(
                                meshComponent.vertexBuffer,
                                meshComponent.indexBuffer,
                                materialComponent.material,
                                shaderComponent.shaderProgram(),
                                meshComponent.visible,
                                meshComponent.instances,
                                meshComponent.indexCount,
                                meshComponent.transformsBuffers,
                                actor,
                                sphereCenter,
                                sphereMaxPoint
                        ));
                    }
                }

                ByteBuffer transformsData = meshComponent.transformsBuffers[sceneRenderer.getFrameIndex()].get();
                for(int i = 0; i < meshComponent.instances; i++)
                    transformComponent.transforms()[i].get(i * MathUtil.MATRIX_SIZE_BYTES, transformsData);

            }
            if(actor.has(LightComponent.class)) {
                LightData lightData = actor.getComponent(LightComponent.class).data;

                lights.add(lightData);


            }
            if(actor.has(UIComponent.class)) {
                UIComponent uiComponent = actor.getComponent(UIComponent.class);
                if (!uiComponent.active) {

                    uiComponent.loop = new Loop();
                    uiComponent.loop.setWidget(uiComponent.widget);
                    uiComponent.loop.setCompositor(renderPipeline.getCompositor());

                    uiComponent.active = true;
                }

                if (uiComponent.rect2D.isPresent()) {
                    Rect2D rect2D = uiComponent.rect2D.get();
                    uiComponent.loop.update(
                            (int) rect2D.x,
                            (int) rect2D.y,
                            (int) rect2D.w,
                            (int) rect2D.h
                    );
                } else {
                    uiComponent.loop.update(
                            0, 0
                    );
                }
            }
        });

        List<Pass> passes = renderPipeline.buildFrame(new SceneRenderData(
                activeCamera,
                items,
                lights
        ));

        sceneRenderer.submit(passes);
    }





    @Override
    public void dispose() {

    }
}
