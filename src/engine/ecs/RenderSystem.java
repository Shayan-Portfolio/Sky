package engine.ecs;

import engine.asset.Asset;
import engine.gameui.*;
import engine.util.MathUtil;
import engine.wsi.Surface;
import engine.graphics.*;
import engine.graphics.Color;
import engine.graphics.text.MsdfFont;
import engine.graphics.text.MsdfJsonLoader;
import engine.graphics.text.TextEffect;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.List;

public class RenderSystem extends ActorSystem {
    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;
    private Camera activeCamera;
    private List<IndexedDrawCall> indexedDrawCalls = new java.util.LinkedList<>();
    private List<LightData> lights = new java.util.LinkedList<>();


    private Buffer[] uiVertexBuffers, uiIndexBuffers;
    private Buffer[] wireframeVertexBuffers;


    private int uiQuadCount = 0;
    private Camera uiCamera;
    private TextureBindings textureBindings;
    private Theme uiTheme;
    private Matrix4f uiTransform = new Matrix4f();
    private Vector4f origin = new Vector4f();
    private UIPainter uiPainterImpl;
    private int maxUIQuads = 6000;
    private int maxWireframeLines = 100000;
    private int wireframeVertexCount = 0;



    public static class IndexedDrawCall {
        public Buffer vertexBuffer;
        public Buffer indexBuffer;
        public Material material;
        public ShaderProgram shaderProgram;
        public boolean visible;
        public boolean instanced;
        public int instanceCount;
        public int indexCount;
        public Buffer[] transformsBuffers;

        public IndexedDrawCall(Buffer vertexBuffer, Buffer indexBuffer, Material material, ShaderProgram shaderProgram, boolean visible, boolean instanced, int instanceCount, int indexCount, Buffer[] transformsBuffers, Vector4f sphereRadius) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.material = material;
            this.shaderProgram = shaderProgram;
            this.visible = visible;
            this.instanced = instanced;
            this.instanceCount = instanceCount;
            this.indexCount = indexCount;
            this.transformsBuffers = transformsBuffers;
        }
    }

    public RenderSystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene, Surface surface, Asset<String> uiThemeJSON) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;
        uiTheme = ThemeLoader.loadTheme(uiThemeJSON.getObject());
        uiThemeJSON.addListener(() -> uiTheme = ThemeLoader.loadTheme(uiThemeJSON.getObject()));


        uiCamera = Camera.newOrthoCamera(
                renderer.getWidth(),
                renderer.getHeight(),
                0,
                1,
                true,
                false
        );




        //UI Compositing setup
        {
            uiVertexBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            uiIndexBuffers = new Buffer[renderer.getMaxFramesInFlight()];


            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                uiVertexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        renderPipeline.getUIShaderProgram().getVertexAttributesSize() * Float.BYTES * 4 * maxUIQuads,
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
                uiIndexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Integer.BYTES * 6 * maxUIQuads,
                        Buffer.Usage.IndexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

            textureBindings = new TextureBindings();




            //UI Painter implementation
            {
                uiPainterImpl = new UIPainter() {
                    @Override
                    public void drawRoundRect(float x, float y, float w, float h, float radius, Color c0, Color c1, Color c2, Color c3) {
                        RenderSystem.this.drawQuad(
                                x,
                                y,
                                w,
                                h,
                                0, 0,
                                0, 1,
                                1, 0,
                                1, 1,
                                -4,
                                w,
                                h,
                                radius,
                                c0,
                                c1,
                                c2,
                                c3
                        );
                    }

                    @Override
                    public void drawRect(float x, float y, float w, float h, Color color) {
                        RenderSystem.this.drawQuad(
                                x,
                                y,
                                w,
                                h,
                                -1, -1,
                                -1, -1,
                                -1, -1,
                                -1, -1,
                                -1,
                                -1,
                                -1,
                                -1,
                                color,
                                color,
                                color,
                                color
                        );
                    }

                    @Override
                    public void drawArc(float x, float y, float w, float h, float range, float thickness, Color color) {
                        RenderSystem.this.drawQuad(
                                x,
                                y,
                                w,
                                h,
                                -1, 1,
                                -1, -1,
                                1, 1,
                                1, -1,
                                -2,
                                thickness,
                                range,
                                -1,
                                color,
                                color,
                                color,
                                color
                        );
                    }

                    @Override
                    public void drawRectLines(float x, float y, float w, float h, int thickness, Color color) {
                        drawRect(x - ((float) thickness / 2), y, thickness, h, color);
                        //Top
                        drawRect(x, y - ((float) thickness / 2), w, thickness, color);
                        //Bottom
                        drawRect(x, y - ((float) thickness / 2) + h, w, thickness, color);
                        //Right
                        drawRect(x - ((float) thickness / 2) + w, y, thickness, h, color);
                    }

                    @Override
                    public void drawLine(float x1, float y1, float x2, float y2, Color color) {
                        float dx = x2 - x1;
                        float dy = y2 - y1;

                        float angle = (float) Math.atan2(dy, dx);

                        setOrigin(x1, y1);
                        setTransform(uiTransform.rotateZ(angle));

                        float hypotenuse = (float) Math.sqrt((dx * dx) + (dy * dy));

                        drawRect(x1, y1 - 2, hypotenuse, 4, color);

                        setTransform(uiTransform.rotateZ(-angle));

                        setOrigin(0, 0);
                    }


                    public void drawTexture(float x,
                                            float y,
                                            float w,
                                            float h,

                                            float uvtlx,
                                            float uvtly,

                                            float uvblx,
                                            float uvbly,

                                            float uvtrx,
                                            float uvtry,

                                            float uvbrx,
                                            float uvbry,
                                            float op1,
                                            Color color,
                                            Texture texture,
                                            Sampler sampler, boolean msdf) {

                        int index = textureBindings.getTextureBinding(texture);

                        renderPipeline.getUIShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", texture).arrayIndex(index));
                        renderPipeline.getUIShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(index));


                        drawQuad(
                                x,
                                y,
                                w,
                                h,
                                uvtlx,
                                uvtly,
                                uvblx,
                                uvbly,
                                uvtrx,
                                uvtry,
                                uvbrx,
                                uvbry,
                                msdf ? -3 : 0,
                                index,
                                op1,
                                -1,
                                color,
                                color,
                                color,
                                color
                        );
                    }

                    @Override
                    public void setOrigin(float x, float y) {
                        RenderSystem.this.setOrigin(x, y);
                    }

                    @Override
                    public Vector4f getOrigin() {
                        return origin;
                    }

                    @Override
                    public void setTransform(Matrix4f transform) {
                        RenderSystem.this.uiTransform.set(transform);
                    }

                    @Override
                    public void drawTexture(float x, float y, float w, float h, Color color, Texture texture, Sampler sampler) {
                        drawTexture(x, y, w, h, 0, 0,
                                0, 1,
                                1, 0,
                                1, 1,
                                -1,
                                color,
                                texture,
                                sampler,
                                false
                        );
                    }

                    private void drawGlyph(float x, float y, float w, float h, MsdfFont msdfFont, MsdfJsonLoader.Character character, Color color) {

                        MsdfJsonLoader.MsdfData msdfData = msdfFont.getMSDFData();
                        int msdfScreenPxRange = (int) ((32 / msdfData.width) * msdfData.size);

                        drawTexture(
                                x,
                                y,
                                w,
                                h,
                                character.atlasBounds.left / msdfData.width,
                                1 - character.atlasBounds.top / msdfData.height,
                                character.atlasBounds.left / msdfData.width,
                                1 - character.atlasBounds.bottom / msdfData.height,
                                character.atlasBounds.right / msdfData.width,
                                1 - character.atlasBounds.top / msdfData.height,
                                character.atlasBounds.right / msdfData.width,
                                1 - character.atlasBounds.bottom / msdfData.height,
                                msdfScreenPxRange,
                                color,
                                msdfFont.getTexture(),
                                msdfFont.getSampler(),
                                true
                        );
                    }

                    @Override
                    public void drawString(float x, float y, String text, MsdfFont msdfFont, TextEffect textEffect, Color color) {
                        if(textEffect != null)
                            textEffect.update();

                        float xl = 0;
                        float yl = y + (msdfFont.getMSDFData().lineHeight + msdfFont.getMSDFData().descender) * msdfFont.getMSDFData().size;
                        float spaceXAdvance = msdfFont.getMSDFData().characters[' '].advance;

                        for (int i = 0; i < text.length(); i++) {
                            char c = text.charAt(i);

                            if(c == '\n') {
                                yl += msdfFont.getMSDFData().lineHeight * msdfFont.getMSDFData().size;
                                xl = 0;
                                continue;
                            }
                            if(c == '\t') {
                                xl = msdfFont.getTabWidth() * spaceXAdvance;
                                continue;
                            }

                            MsdfJsonLoader.Character character = msdfFont.getMSDFData().characters[c];
                            if(character == null) character = msdfFont.getMSDFData().characters['?'];

                            MsdfJsonLoader.Rect planeBounds = character.planeBounds;
                            if(planeBounds != null) {

                                float sw = (planeBounds.right - planeBounds.left) * msdfFont.getMSDFData().size;
                                float sh = (planeBounds.top - planeBounds.bottom) * msdfFont.getMSDFData().size;
                                float yo = planeBounds.bottom * msdfFont.getMSDFData().size;

                                float effectOffsetX = 0, effectOffsetY = 0;

                                if(textEffect != null) {
                                    Vector2f effectOffset = textEffect.offset(i, text.length());
                                    effectOffsetX = effectOffset.x;
                                    effectOffsetY = effectOffset.y;
                                }

                                drawGlyph(
                                        x + xl + effectOffsetX,
                                        yl - sh - yo + effectOffsetY,
                                        sw,
                                        sh,
                                        msdfFont,
                                        character,
                                        color
                                );
                            }

                            xl += character.advance * msdfFont.getMSDFData().size;
                        }


                    }

                    @Override
                    public Theme getTheme() {
                        return uiTheme;
                    }
                };
            }

        }

        //Wireframes
        {
            wireframeVertexBuffers = new Buffer[renderer.getMaxFramesInFlight()];


            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                wireframeVertexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        renderPipeline.getWireframeShaderProgram().getVertexAttributesSize() * Float.BYTES * 2 * maxWireframeLines,
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }
        }



    }



    @Override
    public void run(Actor root) {
        indexedDrawCalls.clear();
        lights.clear();
        uiTransform.identity();
        uiVertexBuffers[renderer.getFrameIndex()].get().clear();
        uiIndexBuffers[renderer.getFrameIndex()].get().clear();
        wireframeVertexBuffers[renderer.getFrameIndex()].get().clear();
        setOrigin(0, 0);

        Color color = new Color(3, 3, 3, 1);

        drawQuad(
                0,
                0,
                renderer.getWidth(),
                renderer.getHeight(),
                0, 0,
                0, 1,
                1, 0,
                1, 1,
                0,
                0,
                -1,
                -1,
                color, color, color, color
        );



        root.previsitAllActors(actor -> {
            if(actor.has(CameraComponent.class)) {
                CameraComponent cameraComponent = actor.getComponent(CameraComponent.class);
                Camera newCamera = actor.getComponent(CameraComponent.class).camera;
                if(cameraComponent.camera.isActive())
                    activeCamera = newCamera;
                else
                    drawFrustum(newCamera.getInvView(), newCamera.getInvProj(), newCamera.getDebugColor());

            }
            if(actor.has(MeshComponent.class) && actor.has(MaterialComponent.class) && actor.has(ShaderComponent.class) && actor.has(TransformComponent.class)) {
                MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                MaterialComponent materialComponent = actor.getComponent(MaterialComponent.class);
                ShaderComponent shaderComponent = actor.getComponent(ShaderComponent.class);
                TransformComponent transformComponent = actor.getComponent(TransformComponent.class);

                Vector4f sphereCenter = transformComponent.getPosition(new Vector4f());
                Vector4f sphereMaxPoint = transformComponent.transform().transform(meshComponent.getMeshData().getSphereMaxPoint(), new Vector4f());

                if(meshComponent.isVisible()) {
                    drawSphere(sphereCenter, sphereMaxPoint);
                    indexedDrawCalls.add(new IndexedDrawCall(
                            meshComponent.vertexBuffer,
                            meshComponent.indexBuffer,
                            materialComponent.material,
                            shaderComponent.shaderProgram(),
                            meshComponent.visible,
                            meshComponent.instanced,
                            meshComponent.instanceCount,
                            meshComponent.indexCount,
                            meshComponent.transformsBuffers,
                            sphereMaxPoint
                    ));
                }

                ByteBuffer transformsData = meshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                if(!meshComponent.instanced) transformComponent.transform().get(0, transformsData);
            }
            if(actor.has(LightComponent.class)) {
                LightData lightData = actor.getComponent(LightComponent.class).data;

                lights.add(lightData);
                drawFrustum(lightData.invView, lightData.invProj, Color.RED);

            }
            if(actor.has(UIComponent.class)) {
                UIComponent uiComponent = actor.getComponent(UIComponent.class);
                if (!uiComponent.active) {

                    uiComponent.loop = new Loop();
                    uiComponent.loop.setWidget(uiComponent.widget);
                    uiComponent.loop.setGfxPlatform(uiPainterImpl);

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
                indexedDrawCalls,
                lights,
                uiVertexBuffers[renderer.getFrameIndex()],
                uiIndexBuffers[renderer.getFrameIndex()],
                uiQuadCount,
                uiCamera,
                wireframeVertexBuffers[renderer.getFrameIndex()],
                wireframeVertexCount
        ));

        renderer.submit(passes);
        uiQuadCount = 0;
    }

    private void drawSphere(Vector4f center, Vector4f sphereMaxPoint) {
        float dx = sphereMaxPoint.x - center.x;
        float dy = sphereMaxPoint.y - center.y;
        float dz = sphereMaxPoint.z - center.z;

        float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        int stacks = 16;
        int slices = 16;

        for (int i = 0; i <= stacks; i++) {
            float phi = (float) (Math.PI * i / stacks);

            for (int j = 0; j <= slices; j++) {
                float theta = (float) (2.0 * Math.PI * j / slices);

                float x = center.x + radius * (float) (Math.sin(phi) * Math.cos(theta));
                float y = center.y + radius * (float) Math.cos(phi);
                float z = center.z + radius * (float) (Math.sin(phi) * Math.sin(theta));

                if (j < slices) {
                    float nextTheta = (float) (2.0 * Math.PI * (j + 1) / slices);

                    float nextX = center.x + radius * (float) (Math.sin(phi) * Math.cos(nextTheta));
                    float nextZ = center.z + radius * (float) (Math.sin(phi) * Math.sin(nextTheta));

                    drawLine(x, y, z, nextX, y, nextZ, Color.BLUE);
                }

                if (i < stacks) {
                    float nextPhi = (float) (Math.PI * (i + 1) / stacks);

                    float nextX = center.x + radius * (float) (Math.sin(nextPhi) * Math.cos(theta));
                    float nextY = center.y + radius * (float) Math.cos(nextPhi);
                    float nextZ = center.z + radius * (float) (Math.sin(nextPhi) * Math.sin(theta));

                    drawLine(x, y, z, nextX, nextY, nextZ, Color.BLUE);
                }
            }
        }
    }

    private void drawLine(float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        ByteBuffer vertexBufferData = wireframeVertexBuffers[renderer.getFrameIndex()].get();

        vertexBufferData.putFloat(x1);
        vertexBufferData.putFloat(y1);
        vertexBufferData.putFloat(z1);

        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        vertexBufferData.putFloat(x2);
        vertexBufferData.putFloat(y2);
        vertexBufferData.putFloat(z2);

        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        wireframeVertexCount += 2;
    }
    private void drawFrustum(Matrix4f invView, Matrix4f invProj, Color color) {

        float x = invView.m30(), y = invView.m31(), z = invView.m32();


        float depth = 1;
        Vector4f tr = MathUtil.unproject(new Vector2f(1f, -1f), depth, invView, invProj);
        Vector4f tl = MathUtil.unproject(new Vector2f(-1f, -1f), depth, invView, invProj);
        Vector4f br = MathUtil.unproject(new Vector2f(1f, 1f), depth, invView, invProj);
        Vector4f bl = MathUtil.unproject(new Vector2f(-1f, 1f), depth, invView, invProj);

        drawLine(x, y, z, tr.x, tr.y, tr.z, color);
        drawLine(x, y, z, tl.x, tl.y, tl.z, color);
        drawLine(x, y, z, br.x, br.y, br.z, color);
        drawLine(x, y, z, bl.x, bl.y, bl.z, color);

        drawLine(tl.x, tl.y, tl.z, tr.x, tr.y, tr.z, color);
        drawLine(tr.x, tr.y, tr.z, br.x, br.y, br.z, color);
        drawLine(br.x, br.y, br.z, bl.x, bl.y, bl.z, color);
        drawLine(bl.x, bl.y, bl.z, tl.x, tl.y, tl.z, color);


    }

    private void setOrigin(float x, float y) {
        origin.set(x, y, 0, 1);
    }


    private void drawQuad(
                          float x,
                          float y,
                          float w,
                          float h,

                          float uvtlx,
                          float uvtly,

                          float uvblx,
                          float uvbly,

                          float uvtrx,
                          float uvtry,

                          float uvbrx,
                          float uvbry,

                          int shapeMode,
                          float op0,
                          float op1,
                          float op2,
                          Color c0, Color c1, Color c2, Color c3) {


        ByteBuffer vertexBufferData = uiVertexBuffers[renderer.getFrameIndex()].get();
        ByteBuffer indexBufferData = uiIndexBuffers[renderer.getFrameIndex()].get();

                Vector4f topLeft = new Vector4f(x, y, 0, 1).sub(origin).mul(uiTransform).add(origin),
                bottomLeft = new Vector4f(x, y + h, 0, 1).sub(origin).mul(uiTransform).add(origin),
                bottomRight = new Vector4f(x + w, y + h, 0, 1).sub(origin).mul(uiTransform).add(origin),
                topRight = new Vector4f(x + w, y, 0, 1).sub(origin).mul(uiTransform).add(origin);

        vertexBufferData.putFloat(topLeft.x);
        vertexBufferData.putFloat(topLeft.y);
        vertexBufferData.putFloat(c0.r);
        vertexBufferData.putFloat(c0.g);
        vertexBufferData.putFloat(c0.b);
        vertexBufferData.putFloat(c0.a);
        vertexBufferData.putFloat(uvtlx);
        vertexBufferData.putFloat(uvtly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);
        vertexBufferData.putFloat(op2);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(c1.r);
        vertexBufferData.putFloat(c1.g);
        vertexBufferData.putFloat(c1.b);
        vertexBufferData.putFloat(c1.a);
        vertexBufferData.putFloat(uvblx);
        vertexBufferData.putFloat(uvbly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);
        vertexBufferData.putFloat(op2);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(c2.r);
        vertexBufferData.putFloat(c2.g);
        vertexBufferData.putFloat(c2.b);
        vertexBufferData.putFloat(c2.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);
        vertexBufferData.putFloat(op2);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(c3.r);
        vertexBufferData.putFloat(c3.g);
        vertexBufferData.putFloat(c3.b);
        vertexBufferData.putFloat(c3.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);
        vertexBufferData.putFloat(op2);

        indexBufferData.putInt(0 + (4 * uiQuadCount));
        indexBufferData.putInt(1 + (4 * uiQuadCount));
        indexBufferData.putInt(2 + (4 * uiQuadCount));
        indexBufferData.putInt(2 + (4 * uiQuadCount));
        indexBufferData.putInt(3 + (4 * uiQuadCount));
        indexBufferData.putInt(0 + (4 * uiQuadCount));
        uiQuadCount++;
    }


    @Override
    public void dispose() {

    }
}
