package engine.graphics.pipelines;

import engine.Application;
import engine.Time;
import engine.asset.AssetRegistry;
import engine.ecs.RenderSystem;
import engine.graphics.*;
import engine.input.Input;
import engine.util.MathUtil;
import org.joml.Vector3f;
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
    private SceneRenderer sceneRenderer;


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

        //Wireframe Shader
        {
            wireframeShaderProgram = ShaderProgram.newShaderProgram(graph);
            wireframeShaderProgram.setTopologyType(TopologyType.LineList);
            wireframeShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Wireframe_vertex.spv"), ShaderType.VertexShader);
            wireframeShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/Wireframe_fragment.spv"), ShaderType.FragmentShader);
            wireframeShaderProgram.assemble();
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
    @Override
    public List<Pass> buildFrame(SceneRenderData sceneRenderData) {


        uiShaderProgram.setTextures(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", nSceneColorTextures[sceneRenderer.getFrameIndex()]).arrayIndex(0));
        uiShaderProgram.setSamplers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(0));
        Texture[] swapchainTextures = sceneRenderer.getSwapchainRenderTarget()
                .getAttachment(AttachmentTypes.Color0)
                .getTextures();

        Texture[] shadowMapTextures = new Texture[sceneRenderData.lights().size() * sceneRenderer.getMaxFramesInFlight()];

        List<LightData> lights = sceneRenderData.lights();
        for (int i = 0; i < lights.size(); i++) {
            LightData lightData = lights.get(i);

            for (int j = 0; j < sceneRenderer.getMaxFramesInFlight(); j++) {
                shadowMapTextures[(sceneRenderer.getMaxFramesInFlight() * i) + j] = lightData.renderTarget
                        .getAttachment(AttachmentTypes.Depth)
                        .getTextures()[sceneRenderer.getFrameIndex()];
            }
        }

        List<RenderSystem.RenderItem> drawCalls = sceneRenderData.drawCalls();
        for (RenderSystem.RenderItem drawCall : drawCalls) {
            for (int j = 0; j < sceneRenderData.lights().size(); j++) {
                drawCall.shaderProgram.setTextures(
                        sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps", shadowMapTextures[sceneRenderer.getMaxFramesInFlight() * j + sceneRenderer.getFrameIndex()]).arrayIndex(j)
                );
            }
            drawCall.shaderProgram.setSamplers(
                    sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps_sampler", sampler)
            );
            //drawCall.shaderProgram.setBuffers(sceneRenderer.getFrameIndex(), new DescriptorUpdate<>("tile_colors", tileDataBuffers[sceneRenderer.getFrameIndex()]));
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

                Vector3f cameraPosition = camera.getPosition(new Vector3f());
                Vector3f[] frustum = MathUtil.getFrustum(camera.getInvView(), camera.getInvProj(), 1);
                for(RenderSystem.RenderItem item : sceneRenderData.drawCalls()) {

                    if(MathUtil.isSphereInFrustum(item.sphereCenter, item.radius, cameraPosition, frustum)) {

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


                }


                scenePass.setCullMode(CullMode.None);
                scenePass.setShaderProgram(wireframeShaderProgram);
                scenePass.setVertexBuffer(sceneRenderData.wireframeVertexBuffer());

                //TODO(Shayan) This is incredibly slow on Intel Iris Xe (Windows)
                scenePass.draw(sceneRenderData.wireframeVertexCount());


                scenePass.setCullMode(CullMode.None);
                scenePass.setShaderProgram(skyboxShaderProgram);
                scenePass.draw(36);
            }
            scenePass.endRendering();

        });

        uiPass.bind("NSwapchainTextures", swapchainTextures);
        uiPass.bind("NSwapchainTexturesPresent", swapchainTextures);
        uiPass.submit(() -> {
            uiPass.startRendering(sceneRenderer.getSwapchainRenderTarget(), 0, sceneRenderer.getWidth(), sceneRenderer.getHeight(), true, Color.BLACK);
            {
                uiPass.setDrawBuffers(sceneRenderData.uiVertexBuffer(), sceneRenderData.uiIndexBuffer());
                uiPass.setCullMode(CullMode.Back);
                uiPass.setShaderProgram(uiShaderProgram);
                try (MemoryStack stack = stackPush()) {
                    ByteBuffer pPushConstants = stack.calloc(MathUtil.MATRIX_SIZE_BYTES * 2);
                    sceneRenderData.uiCamera().getView().get(pPushConstants);
                    sceneRenderData.uiCamera().getProj().get(MathUtil.MATRIX_SIZE_BYTES, pPushConstants);
                    uiPass.setPushConstants(pPushConstants);
                }
                uiPass.drawIndexed(sceneRenderData.uiQuadCount() * 6);
            }
            uiPass.endRendering();
        });

        return graph.compile(uiPass);
    }

    @Override
    public ShaderProgram getUIShaderProgram() {
        return uiShaderProgram;
    }

    @Override
    public ShaderProgram getWireframeShaderProgram() {
        return wireframeShaderProgram;
    }

}
