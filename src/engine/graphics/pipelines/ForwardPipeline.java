package engine.graphics.pipelines;

import engine.asset.AssetRegistry;
import engine.ecs.LightComponent;
import engine.ecs.RenderSystem;
import engine.graphics.*;
import engine.util.MathUtil;
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
    private Texture[] nDepthPrepassTextures;
    private Buffer[] tileDataBuffers;
    private Buffer[] sceneBuffers;
    private Renderer renderer;

    private Sampler sampler;
    private static final int PbrMode = 0, ShadowMapMode = 1, DepthPrepassMode = 2;
    private int TileSize = 64;
    @Override
    public void init(Renderer renderer) {
        this.renderer = renderer;
        this.graph = new RenderGraph(renderer);

        //Depth prepass
        {
            depthPassRT = new RenderTarget(renderer);
            nDepthPrepassTextures = new Texture[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < nDepthPrepassTextures.length; i++) {
                nDepthPrepassTextures[i] = Texture.newDepthTexture(depthPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32);
            }

            depthPassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }

        //Tiled lighting data
        {

            tiledLightingShaderProgram = ShaderProgram.newShaderProgram(graph);
            tiledLightingShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/TileLightCull_compute.spv"), ShaderType.ComputeShader);
            tiledLightingShaderProgram.assemble();

            tileDataBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < tileDataBuffers.length; i++) {
                tileDataBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Float.BYTES * 4 * 1200,
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.GPULocal,
                        false
                );
                tiledLightingShaderProgram.setBuffers(i, new DescriptorUpdate<>("tiled_lighting_data", tileDataBuffers[i]));
            }




        }

        //Scene Data Buffers
        {
            sceneBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < sceneBuffers.length; i++) {
                sceneBuffers[i] = Buffer.newBuffer(
                        renderer,
                        3136,
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.GPULocal,
                        false
                );
            }
        }

        //Scene pass
        {
            scenePassRT = new RenderTarget(renderer);
            nSceneColorTextures = new Texture[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < nSceneColorTextures.length; i++) {
                nSceneColorTextures[i] = Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR16G16B16A16);
            }
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Color0, nSceneColorTextures, null));
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }

        sampler = Sampler.newSampler(scenePassRT, Linear, Linear, false);



        shadowMapPass = Pass.newGraphicsPass(graph, "Shadow", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.writes("NShadowTextures", null, AccessTypes.DepthWrite);
        }


        depthPass = Pass.newGraphicsPass(graph, "Depth", renderer.getMaxFramesInFlight());
        {
            depthPass.writes("NDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthWrite);
        }

        tilingPass = Pass.newComputePass(graph, "Forward+ Tiled Light Culling", renderer.getMaxFramesInFlight());
        {
            tilingPass.writes("NTiledLightingData", tileDataBuffers, AccessTypes.ShaderWrite);
        }


        scenePass = Pass.newGraphicsPass(graph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.reads("ITiledLightingData", tileDataBuffers, AccessTypes.ShaderRead);
            scenePass.reads("IShadowTextures", null, AccessTypes.ShaderRead);
            scenePass.reads("IDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthReadWrite);
            scenePass.writes("NRenderTextures", nSceneColorTextures, AccessTypes.ColorWrite);
        }


        uiPass = Pass.newGraphicsPass(graph, "UI", renderer.getMaxFramesInFlight());
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

    @Override
    public List<Pass> buildFrame(SceneRenderData sceneRenderData) {


        sceneRenderData.uiShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", nSceneColorTextures[renderer.getFrameIndex()]).arrayIndex(0));
        sceneRenderData.uiShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(0));
        Texture[] swapchainTextures = renderer.getSwapchainRenderTarget()
                .getAttachment(AttachmentTypes.Color0)
                .getTextures();

        Texture[] shadowMapTextures = new Texture[sceneRenderData.lights().size()];

        List<LightData> lights = sceneRenderData.lights();
        for (int i = 0; i < lights.size(); i++) {
            LightData lightData = lights.get(i);
            shadowMapTextures[i] = lightData.renderTarget
                    .getAttachment(AttachmentTypes.Depth)
                    .getTextures()[renderer.getFrameIndex()];
        }

        List<RenderSystem.IndexedDrawCall> drawCalls = sceneRenderData.drawCalls();
        for (RenderSystem.IndexedDrawCall drawCall : drawCalls) {
            for (int j = 0; j < sceneRenderData.lights().size(); j++) {
                drawCall.shaderProgram.setTextures(
                        renderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps", shadowMapTextures[j]).arrayIndex(j)
                );
            }
            drawCall.shaderProgram.setSamplers(
                    renderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps_sampler", sampler)
            );
            drawCall.shaderProgram.setBuffers(renderer.getFrameIndex(), new DescriptorUpdate<>("tile_colors", tileDataBuffers[renderer.getFrameIndex()]));
            drawCall.shaderProgram.setBuffers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>("scene_desc", sceneBuffers[renderer.getFrameIndex()]),
                    new DescriptorUpdate<>("transforms", drawCall.transformsBuffers[renderer.getFrameIndex()])
            );

            Sampler sampler = drawCall.material.getSampler();

            List<Texture> textures = drawCall.material.getTextures();
            for (int i = 0; i < textures.size(); i++) {
                Texture texture = textures.get(i);
                drawCall.shaderProgram.setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("material", texture).arrayIndex(i));
                drawCall.shaderProgram.setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("material_sampler", sampler));

            }
        }

        tiledLightingShaderProgram.setBuffers(
                renderer.getFrameIndex(),
                new DescriptorUpdate<>("scene_desc", sceneBuffers[renderer.getFrameIndex()])
        );

        tiledLightingShaderProgram.setBuffers(renderer.getFrameIndex(), new DescriptorUpdate<>("tiled_lighting_data", tileDataBuffers[renderer.getFrameIndex()]));

        Camera camera = sceneRenderData.sceneCamera();
        Buffer sceneBuffer = sceneBuffers[renderer.getFrameIndex()];
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






        int tilesW = (int) Math.ceil((double) renderer.getWidth() / TileSize);
        int tilesH = (int) Math.ceil((double) renderer.getHeight() / TileSize);

        shadowMapPass.bind("NShadowTextures", shadowMapTextures);
        shadowMapPass.submit(() -> {
            List<LightData> lightDataList = sceneRenderData.lights();
            for (int i = 0; i < lightDataList.size(); i++) {
                LightData lightData = lightDataList.get(i);
                shadowMapPass.startRendering(lightData.renderTarget, 0, 1024, 1024, true, Color.BLACK);
                shadowMapPass.setCullMode(CullMode.None);

                for (RenderSystem.IndexedDrawCall drawCall : sceneRenderData.drawCalls()) {
                    shadowMapPass.setShaderProgram(drawCall.shaderProgram);
                    shadowMapPass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                        pPushConstants.putInt(ShadowMapMode);
                        pPushConstants.putInt(i);
                        pPushConstants.putInt(sceneRenderData.lights().size());
                        shadowMapPass.setPushConstants(pPushConstants);
                    }
                    if(drawCall.instanced) shadowMapPass.drawInstanced(drawCall.indexCount, drawCall.instanceCount);
                    else shadowMapPass.drawIndexed(drawCall.indexCount);
                }

                shadowMapPass.endRendering();
            }
        });

        depthPass.submit(() -> {
            depthPass.startRendering(depthPassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
            {
                for(RenderSystem.IndexedDrawCall drawCall : sceneRenderData.drawCalls()) {
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
                    if(drawCall.instanced) depthPass.drawInstanced(drawCall.indexCount, drawCall.instanceCount);
                    else depthPass.drawIndexed(drawCall.indexCount);
                }
            }
            depthPass.endRendering();

        });

        tilingPass.submit(() -> {
            tilingPass.setShaderProgram(tiledLightingShaderProgram);

            try (MemoryStack stack = stackPush()) {
                ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 5);
                pPushConstants.putInt(tilesW);
                pPushConstants.putInt(tilesH);
                pPushConstants.putInt(TileSize);
                pPushConstants.putInt(renderer.getWidth());
                pPushConstants.putInt(renderer.getHeight());
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
            scenePass.startRendering(scenePassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.WHITE);
            {
                for(RenderSystem.IndexedDrawCall drawCall : sceneRenderData.drawCalls()) {
                    scenePass.setCullMode(CullMode.Back);
                    scenePass.setShaderProgram(drawCall.shaderProgram);
                    scenePass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 6);
                        pPushConstants.putInt(PbrMode);
                        pPushConstants.putInt(-1);
                        pPushConstants.putInt(sceneRenderData.lights().size());
                        pPushConstants.putInt(tilesW);
                        pPushConstants.putInt(tilesH);
                        pPushConstants.putInt(TileSize);
                        scenePass.setPushConstants(pPushConstants);
                    }
                    if(drawCall.instanced) scenePass.drawInstanced(drawCall.indexCount, drawCall.instanceCount);
                    else scenePass.drawIndexed(drawCall.indexCount);
                }
            }
            scenePass.endRendering();

        });

        uiPass.bind("NSwapchainTextures", swapchainTextures);
        uiPass.bind("NSwapchainTexturesPresent", swapchainTextures);
        uiPass.submit(() -> {
            uiPass.startRendering(renderer.getSwapchainRenderTarget(), 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
            {
                uiPass.setDrawBuffers(sceneRenderData.uiVertexBuffer(), sceneRenderData.uiIndexBuffer());
                uiPass.setCullMode(CullMode.Back);
                uiPass.setShaderProgram(sceneRenderData.uiShaderProgram());
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
}
