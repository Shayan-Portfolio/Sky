package engine.graphics.pipelines;

import engine.Application;
import engine.Time;
import engine.asset.AssetRegistry;
import engine.ecs.RenderSystem;
import engine.gameui.TextureBindings;
import engine.gameui.Theme;
import engine.gameui.ThemeLoader;
import engine.gameui.Compositor;
import engine.graphics.*;
import engine.graphics.text.MsdfFont;
import engine.graphics.text.MsdfJsonLoader;
import engine.graphics.text.TextEffect;
import engine.input.Input;
import engine.util.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.stackPush;


public class ForwardPipeline extends RenderPipeline {


    private GraphicsPass uiPass;
    private RenderTarget scenePassRT;
    private GraphicsPass scenePass;
    private Texture[] nSceneColorTextures;
    private GraphicsPass depthPass;
    private RenderTarget depthPassRT;
    private GraphicsPass shadowMapPass;
    private ComputePass tilingPass;
    private ShaderProgram tiledLightingShaderProgram;
    private ShaderProgram uiShaderProgram;
    private ShaderProgram wireframeShaderProgram;
    private ShaderProgram skyboxShaderProgram;
    private Texture[] nDepthPrepassTextures;
    private Buffer[] tileDataBuffers;
    private Buffer[] sceneBuffers;
    private Buffer[] wireframeVertexBuffers;
    private SceneRenderer sceneRenderer;
    private Buffer[] uiVertexBuffers, uiIndexBuffers;



    private Sampler sampler;
    private static final int PbrMode = 0, ShadowMapMode = 1, DepthPrepassMode = 2;
    private final int TileSize = 64;
    @Override
    public void init(SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.graph = new RenderGraph(sceneRenderer);

        //Depth prepass
        {
            depthPassRT = new RenderTarget(sceneRenderer);
            nDepthPrepassTextures = new Texture[sceneRenderer.getMaxFramesInFlight()];
            for (int i = 0; i < nDepthPrepassTextures.length; i++) {
                nDepthPrepassTextures[i] = Texture.newDepthTexture(depthPassRT, sceneRenderer.getWidth(), sceneRenderer.getHeight(), TextureFormatType.Depth32);
            }

            depthPassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }

        //Tiled lighting data
        {

            tiledLightingShaderProgram = ShaderProgram.newShaderProgram(graph);
            tiledLightingShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/TileLightCull_compute.spv"), ShaderType.ComputeShader);
            tiledLightingShaderProgram.assemble();

            tileDataBuffers = new Buffer[sceneRenderer.getMaxFramesInFlight()];
            for (int i = 0; i < tileDataBuffers.length; i++) {
                tileDataBuffers[i] = Buffer.newBuffer(
                        sceneRenderer,
                        Float.BYTES * 4 * 1200,
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.GPULocal,
                        false
                );
                tiledLightingShaderProgram.setBuffers(i, new DescriptorUpdate<>("tiled_lighting_data", tileDataBuffers[i]));
            }
        }

        //UI shader
        {
            uiShaderProgram = ShaderProgram.newShaderProgram(graph);
            uiShaderProgram.setDepthTestType(DepthTestType.Always);
            uiShaderProgram.setEnableBlending(true);
            uiShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/UI_vertex.spv"), ShaderType.VertexShader);
            uiShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/UI_fragment.spv"), ShaderType.FragmentShader);
            uiShaderProgram.assemble();
        }


        //UI State
        {
            uiVertexBuffers = new Buffer[sceneRenderer.getMaxFramesInFlight()];
            uiIndexBuffers = new Buffer[sceneRenderer.getMaxFramesInFlight()];


            for (int i = 0; i < sceneRenderer.getMaxFramesInFlight(); i++) {
                uiVertexBuffers[i] = Buffer.newBuffer(
                        sceneRenderer,
                        uiShaderProgram.getVertexAttributesSize() * Float.BYTES * 4 * maxUIQuads,
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
                uiIndexBuffers[i] = Buffer.newBuffer(
                        sceneRenderer,
                        Integer.BYTES * 6 * maxUIQuads,
                        Buffer.Usage.IndexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }
        }

        //UI Rendering Impl
        {
            uiTheme = ThemeLoader.loadTheme(AssetRegistry.<String>getAsset("core:assets/themes/DarkMode.json").getObject());
            //uiThemeJSON.addListener(() -> uiTheme = ThemeLoader.loadTheme(uiThemeJSON.getObject()));


            uiCamera = Camera.newOrthoCamera(
                    sceneRenderer.getWidth(),
                    sceneRenderer.getHeight(),
                    0,
                    1,
                    true,
                    false
            );


            textureBindings = new TextureBindings(sceneRenderer.getMaxFramesInFlight());

            //Compositor implementation
            {
                compositor = new Compositor() {
                    @Override
                    public void drawRoundRect(float x, float y, float w, float h, float radius, Color c0, Color c1, Color c2, Color c3) {
                        drawQuad(
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
                        drawQuad(
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
                        drawQuad(
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

                        int index = textureBindings.getTextureBinding(texture, sceneRenderer.getFrameIndex());
                        if(index == -1) {
                            index = textureBindings.addBinding(texture, sceneRenderer.getFrameIndex());
                            uiShaderProgram.setTextures(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", texture).arrayIndex(index));
                            uiShaderProgram.setSamplers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(index));
                        }

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
                        ForwardPipeline.this.setOrigin(x, y);
                    }

                    @Override
                    public Vector4f getOrigin() {
                        return origin;
                    }

                    @Override
                    public void setTransform(Matrix4f transform) {
                        uiTransform.set(transform);
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

                        //drawRect(x, y, w, h, Color.GREEN); //TODO(Shayan!)
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

        //Wireframe Shader
        {
            wireframeShaderProgram = ShaderProgram.newShaderProgram(graph);
            wireframeShaderProgram.setTopologyType(TopologyType.LineList);
            wireframeShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Wireframe_vertex.spv"), ShaderType.VertexShader);
            wireframeShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Wireframe_fragment.spv"), ShaderType.FragmentShader);
            wireframeShaderProgram.assemble();
        }

        //Wireframe VB
        {
            wireframeVertexBuffers = new Buffer[sceneRenderer.getMaxFramesInFlight()];


            for (int i = 0; i < sceneRenderer.getMaxFramesInFlight(); i++) {
                wireframeVertexBuffers[i] = Buffer.newBuffer(
                        sceneRenderer,
                        wireframeShaderProgram.getVertexAttributesSize() * Float.BYTES * 2 * maxWireframeLines,
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }
        }


        //Skybox Shader
        {
            skyboxShaderProgram = ShaderProgram.newShaderProgram(graph);
            skyboxShaderProgram.setDepthTestType(DepthTestType.Equals);
            skyboxShaderProgram.setUsesVertexBuffer(false);
            skyboxShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Skybox_vertex.spv"), ShaderType.VertexShader);
            skyboxShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Skybox_fragment.spv"), ShaderType.FragmentShader);
            skyboxShaderProgram.assemble();
        }

        //Scene Data Buffers
        {
            sceneBuffers = new Buffer[sceneRenderer.getMaxFramesInFlight()];
            for (int i = 0; i < sceneBuffers.length; i++) {
                sceneBuffers[i] = Buffer.newBuffer(
                        sceneRenderer,
                        3136, //TODO Shayan this is bad!!!
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.GPULocal,
                        false
                );
            }
        }

        //Scene pass
        {
            scenePassRT = new RenderTarget(sceneRenderer);
            nSceneColorTextures = new Texture[sceneRenderer.getMaxFramesInFlight()];
            for (int i = 0; i < nSceneColorTextures.length; i++) {
                nSceneColorTextures[i] = Texture.newColorTexture(scenePassRT, sceneRenderer.getWidth(), sceneRenderer.getHeight(), TextureFormatType.ColorR16G16B16A16);
            }
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Color0, nSceneColorTextures, null));
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }




        sampler = Sampler.newSampler(scenePassRT, Linear, Linear, false);



        shadowMapPass = Pass.newGraphicsPass(graph, "Shadow", sceneRenderer.getMaxFramesInFlight());
        {
            shadowMapPass.writes("NShadowTextures", null, AccessTypes.DepthWrite);
        }


        depthPass = Pass.newGraphicsPass(graph, "Depth", sceneRenderer.getMaxFramesInFlight());
        {
            depthPass.writes("NDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthWrite);
        }

        tilingPass = Pass.newComputePass(graph, "Forward+ Tiled Light Culling", sceneRenderer.getMaxFramesInFlight());
        {
            tilingPass.writes("NTiledLightingData", tileDataBuffers, AccessTypes.ShaderWrite);
        }


        scenePass = Pass.newGraphicsPass(graph, "Scene", sceneRenderer.getMaxFramesInFlight());
        {
            scenePass.reads("ITiledLightingData", tileDataBuffers, AccessTypes.ShaderRead);
            scenePass.reads("IShadowTextures", null, AccessTypes.ShaderRead);
            scenePass.reads("IDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthReadWrite);
            scenePass.writes("NRenderTextures", nSceneColorTextures, AccessTypes.ColorWrite);
        }


        uiPass = Pass.newGraphicsPass(graph, "UI", sceneRenderer.getMaxFramesInFlight());
        {
            uiPass.reads("IRenderTextures", nSceneColorTextures, AccessTypes.ShaderRead);
            uiPass.writes("NSwapchainTextures", null, AccessTypes.ColorWrite);
            uiPass.writes("NSwapchainTexturesPresent", null, AccessTypes.Present);
        }

        graph.addPasses(
                shadowMapPass,
                depthPass,
                tilingPass,
                scenePass,
                uiPass
        );
    }

    private float radius = 0.37f;

    private void drawLine(float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        ByteBuffer vertexBufferData = wireframeVertexBuffers[sceneRenderer.getFrameIndex()].get();

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
        Vector3f[] frustum = MathUtil.getFrustum(invView, invProj, 1);
        {
            Vector3f tr = frustum[0];
            Vector3f tl = frustum[1];
            Vector3f br = frustum[2];
            Vector3f bl = frustum[3];

            drawLine(x, y, z, tr.x, tr.y, tr.z, color);
            drawLine(x, y, z, tl.x, tl.y, tl.z, color);
            drawLine(x, y, z, br.x, br.y, br.z, color);
            drawLine(x, y, z, bl.x, bl.y, bl.z, color);

            drawLine(tl.x, tl.y, tl.z, tr.x, tr.y, tr.z, color);
            drawLine(tr.x, tr.y, tr.z, br.x, br.y, br.z, color);
            drawLine(br.x, br.y, br.z, bl.x, bl.y, bl.z, color);
            drawLine(bl.x, bl.y, bl.z, tl.x, tl.y, tl.z, color);
        }






    }
    private void drawSphere(@NotNull Vector3f center, Vector3f sphereMaxPoint, Color color) {
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

                    drawLine(x, y, z, nextX, y, nextZ, color);
                }

                if (i < stacks) {
                    float nextPhi = (float) (Math.PI * (i + 1) / stacks);

                    float nextX = center.x + radius * (float) (Math.sin(nextPhi) * Math.cos(theta));
                    float nextY = center.y + radius * (float) Math.cos(nextPhi);
                    float nextZ = center.z + radius * (float) (Math.sin(nextPhi) * Math.sin(theta));

                    drawLine(x, y, z, nextX, nextY, nextZ, color);
                }
            }
        }
    }
    private void drawQuad(float x, float y, float w, float h, float uvtlx, float uvtly, float uvblx, float uvbly, float uvtrx, float uvtry, float uvbrx, float uvbry, int shapeMode, float op0, float op1, float op2, Color c0, Color c1, Color c2, Color c3) {



        ByteBuffer vertexBufferData = uiVertexBuffers[sceneRenderer.getFrameIndex()].get();
        ByteBuffer indexBufferData = uiIndexBuffers[sceneRenderer.getFrameIndex()].get();

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
    private void setOrigin(float x, float y) {
        origin.set(x, y, 0, 1);
    }








    @Override
    public List<Pass> buildFrame(SceneRenderData sceneRenderData) {
        wireframeVertexCount = 0;

        uiTransform.identity();
        setOrigin(0, 0);

        //Update UI shaders
        uiShaderProgram.setTextures(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", nSceneColorTextures[sceneRenderer.getFrameIndex()]).arrayIndex(0));
        uiShaderProgram.setSamplers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(0));
        Texture[] swapchainTextures = sceneRenderer.getSwapchainRenderTarget()
                .getAttachment(AttachmentTypes.Color0)
                .getTextures();

        //Collect scene lights
        Texture[] shadowMapTextures = new Texture[sceneRenderData.lights().size() * sceneRenderer.getMaxFramesInFlight()];

        List<LightData> lights = sceneRenderData.lights();
        for (int i = 0; i < lights.size(); i++) {
            LightData lightData = lights.get(i);
            drawFrustum(lightData.invView, lightData.invProj, Color.RED);

            for (int j = 0; j < sceneRenderer.getMaxFramesInFlight(); j++) {
                shadowMapTextures[(sceneRenderer.getMaxFramesInFlight() * i) + j] = lightData.renderTarget
                        .getAttachment(AttachmentTypes.Depth)
                        .getTextures()[sceneRenderer.getFrameIndex()];
            }
        }

        //Update entity shaders
        List<RenderSystem.RenderItem> drawCalls = sceneRenderData.drawCalls();
        for (RenderSystem.RenderItem drawCall : drawCalls) {
            for (int j = 0; j < sceneRenderData.lights().size(); j++) {
                drawCall.shaderProgram.setTextures(
                        sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps", shadowMapTextures[sceneRenderer.getMaxFramesInFlight() * j + sceneRenderer.getFrameIndex()]).arrayIndex(j)
                );
            }

            drawSphere(drawCall.sphereCenter, drawCall.sphereMaxPoint, Color.RED);
            drawCall.shaderProgram.setSamplers(
                    sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps_sampler", sampler)
            );
            drawCall.shaderProgram.setBuffers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("tile_colors", tileDataBuffers[sceneRenderer.getFrameIndex()]));
            drawCall.shaderProgram.setBuffers(
                    sceneRenderer.getFrameIndex(),
                    new DescriptorUpdate<>("scene_desc", sceneBuffers[sceneRenderer.getFrameIndex()]),
                    new DescriptorUpdate<>("transforms", drawCall.transformsBuffers[sceneRenderer.getFrameIndex()])
            );

            Sampler sampler = drawCall.material.getSampler();

            List<Texture> textures = drawCall.material.getTextures();
            for (int i = 0; i < textures.size(); i++) {
                Texture texture = textures.get(i);
                drawCall.shaderProgram.setTextures(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("material", texture).arrayIndex(i));
                drawCall.shaderProgram.setSamplers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("material_sampler", sampler));

            }
        }

        //Update internal shaders
        tiledLightingShaderProgram.setBuffers(
                sceneRenderer.getFrameIndex(),
                new DescriptorUpdate<>("scene_desc", sceneBuffers[sceneRenderer.getFrameIndex()])
        );
        tiledLightingShaderProgram.setBuffers(
                sceneRenderer.getFrameIndex(),
                new DescriptorUpdate<>("tiled_lighting_data", tileDataBuffers[sceneRenderer.getFrameIndex()])
        );

        skyboxShaderProgram.setBuffers(
                sceneRenderer.getFrameIndex(),
                new DescriptorUpdate<>("scene_desc", sceneBuffers[sceneRenderer.getFrameIndex()])
        );




        //Update scene description
        Camera camera = sceneRenderData.sceneCamera();
        Buffer sceneBuffer = sceneBuffers[sceneRenderer.getFrameIndex()];
        ByteBuffer sceneBufferData = sceneBuffer.get();
        {
            sceneBufferData.clear();

            camera.getView().get(sceneBufferData);
            sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);

            camera.getProj().get(sceneBufferData);
            sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);

            camera.getInvView().get(sceneBufferData);
            sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);

            camera.getInvProj().get(sceneBufferData);
            sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);

            sceneBufferData.putFloat(camera.getzNear());
            sceneBufferData.putFloat(camera.getzFar());

            sceneBufferData.putFloat(camera.getFovY());
            sceneBufferData.putFloat(-1);


            for(LightData data : sceneRenderData.lights()) {
                data.view.get(sceneBufferData);
                sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);
                data.proj.get(sceneBufferData);
                sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);
                data.invView.get(sceneBufferData);
                sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);
                data.invProj.get(sceneBufferData);
                sceneBufferData.position(sceneBufferData.position() + MathUtil.MATRIX_SIZE_BYTES);

                sceneBufferData.putFloat(data.attenuationConstant);
                sceneBufferData.putFloat(data.attenuationLinear);
                sceneBufferData.putFloat(data.attenuationQuadratic);
                sceneBufferData.putFloat(data.shadowTestOffsetBias);
                sceneBufferData.putFloat(data.color.r);
                sceneBufferData.putFloat(data.color.g);
                sceneBufferData.putFloat(data.color.b);
                sceneBufferData.putFloat(data.color.a);
                sceneBufferData.position(sceneBufferData.position() + MathUtil.VEC3_SIZE_BYTES);
                sceneBufferData.putFloat(data.shadowNormalOffsetBias);
            }


        }

        //Update Wireframe descriptors
        {
            wireframeShaderProgram.setBuffers(
                    sceneRenderer.getFrameIndex(),
                    new DescriptorUpdate<>("scene_desc", sceneBuffers[sceneRenderer.getFrameIndex()])
            );
        }

        //Reset internal state
        wireframeVertexBuffers[sceneRenderer.getFrameIndex()].get().clear();
        uiVertexBuffers[sceneRenderer.getFrameIndex()].get().clear();
        uiIndexBuffers[sceneRenderer.getFrameIndex()].get().clear();

        //Compositing
        Color color = new Color(3, 3, 3, 1);

        drawQuad(
                0,
                0,
                sceneRenderer.getWidth(),
                sceneRenderer.getHeight(),
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





        int tilesW = (int) Math.ceil((double) sceneRenderer.getWidth() / TileSize);
        int tilesH = (int) Math.ceil((double) sceneRenderer.getHeight() / TileSize);

        shadowMapPass.bind("NShadowTextures", shadowMapTextures);
        shadowMapPass.submit(() -> {
            List<LightData> lightDataList = sceneRenderData.lights();
            for (int i = 0; i < lightDataList.size(); i++) {
                LightData lightData = lightDataList.get(i);
                shadowMapPass.startRendering(lightData.renderTarget, 0, 1024, 1024, true, Color.BLACK);
                shadowMapPass.setCullMode(CullMode.None);

                for (RenderSystem.RenderItem drawCall : sceneRenderData.drawCalls()) {
                    shadowMapPass.setShaderProgram(drawCall.shaderProgram);
                    shadowMapPass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                        pPushConstants.putInt(ShadowMapMode);
                        pPushConstants.putInt(i);
                        pPushConstants.putInt(sceneRenderData.lights().size());
                        shadowMapPass.setPushConstants(pPushConstants);
                    }
                    shadowMapPass.drawIndexedInstanced(drawCall.indexCount, drawCall.instanceCount);
                }

                shadowMapPass.endRendering();
            }
        });

        depthPass.submit(() -> {
            depthPass.startRendering(depthPassRT, 0, sceneRenderer.getWidth(), sceneRenderer.getHeight(), true, Color.BLACK);
            {
                for(RenderSystem.RenderItem drawCall : sceneRenderData.drawCalls()) {
                    depthPass.setCullMode(CullMode.Back);
                    depthPass.setShaderProgram(drawCall.shaderProgram);
                    depthPass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 6);
                        pPushConstants.putInt(DepthPrepassMode);
                        pPushConstants.putInt(-1);
                        pPushConstants.putInt(sceneRenderData.lights().size());
                        pPushConstants.putInt(tilesW);
                        pPushConstants.putInt(tilesH);
                        pPushConstants.putInt(TileSize);
                        depthPass.setPushConstants(pPushConstants);
                    }
                    depthPass.drawIndexedInstanced(drawCall.indexCount, drawCall.instanceCount);
                }
            }
            depthPass.endRendering();

        });

        tilingPass.submit(() -> {
            tilingPass.setShaderProgram(tiledLightingShaderProgram);

            if(Application.get().getSurface().getKeyPressed(Input.KEY_LEFT)) {
                radius -= 10 * Time.deltaTime;
            }
            if(Application.get().getSurface().getKeyPressed(Input.KEY_RIGHT)) {
                radius += 10 * Time.deltaTime;
            }





            try (MemoryStack stack = stackPush()) {
                ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 7);
                pPushConstants.putInt(tilesW);
                pPushConstants.putInt(tilesH);
                pPushConstants.putInt(TileSize);
                pPushConstants.putInt(sceneRenderer.getWidth());
                pPushConstants.putInt(sceneRenderer.getHeight());
                pPushConstants.putInt(lights.size());
                pPushConstants.putInt((int) radius);
                tilingPass.setPushConstants(pPushConstants);
            }
            tilingPass.dispatch(
                    tilesW,
                    tilesH,
                    1
            );
        });




        scenePass.bind("IShadowTextures", shadowMapTextures);
        scenePass.submit(() -> {
            scenePass.startRendering(scenePassRT, 0, sceneRenderer.getWidth(), sceneRenderer.getHeight(), true, Color.BLACK);
            {

                for(RenderSystem.RenderItem item : sceneRenderData.drawCalls()) {

                    scenePass.setCullMode(CullMode.Back);
                    scenePass.setShaderProgram(item.shaderProgram);
                    scenePass.setDrawBuffers(item.vertexBuffer, item.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 6);
                        pPushConstants.putInt(PbrMode);
                        pPushConstants.putInt(-1);
                        pPushConstants.putInt(lights.size());
                        pPushConstants.putInt(tilesW);
                        pPushConstants.putInt(tilesH);
                        pPushConstants.putInt(TileSize);
                        scenePass.setPushConstants(pPushConstants);
                    }
                    scenePass.drawIndexedInstanced(item.indexCount, item.instanceCount);


                }


                scenePass.setCullMode(CullMode.None);
                scenePass.setShaderProgram(wireframeShaderProgram);
                scenePass.setVertexBuffer(wireframeVertexBuffers[sceneRenderer.getFrameIndex()]);

                //TODO(Shayan) This is incredibly slow on Intel Iris Xe (Windows)
                //scenePass.draw(wireframeVertexCount);


                scenePass.setCullMode(CullMode.None);
                scenePass.setShaderProgram(skyboxShaderProgram);
                scenePass.draw(36);
            }
            scenePass.endRendering();

        });

        uiPass.bind("NSwapchainTextures", swapchainTextures);
        uiPass.bind("NSwapchainTexturesPresent", swapchainTextures);

        //Capture for uiPass lambda
        int uiQuadCountCpy = uiQuadCount;
        uiPass.submit(() -> {
            uiPass.startRendering(sceneRenderer.getSwapchainRenderTarget(), 0, sceneRenderer.getWidth(), sceneRenderer.getHeight(), true, Color.BLACK);
            {
                uiPass.setDrawBuffers(uiVertexBuffers[sceneRenderer.getFrameIndex()], uiIndexBuffers[sceneRenderer.getFrameIndex()]);
                uiPass.setCullMode(CullMode.Back);
                uiPass.setShaderProgram(uiShaderProgram);
                try (MemoryStack stack = stackPush()) {
                    ByteBuffer pPushConstants = stack.calloc(MathUtil.MATRIX_SIZE_BYTES * 2);
                    uiCamera.getView().get(pPushConstants);
                    uiCamera.getProj().get(MathUtil.MATRIX_SIZE_BYTES, pPushConstants);
                    uiPass.setPushConstants(pPushConstants);
                }
                uiPass.drawIndexed(uiQuadCountCpy * 6);
            }
            uiPass.endRendering();
        });

        uiQuadCount = 0;

        return graph.compile(uiPass);
    }


}
