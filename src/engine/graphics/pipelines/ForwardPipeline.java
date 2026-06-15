package engine.graphics.pipelines;

import engine.asset.AssetRegistry;
import engine.ecs.RenderSystem;
import engine.graphics.*;
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
    private ComputePass tiledLightCullingPass;
    private ShaderProgram tiledLightingShaderProgram;
    private Texture[] nDepthPrepassTextures;
    private Buffer[] tiledLightingDataBuffers;
    private Renderer renderer;

    private Sampler sampler;
    private static final int PbrMode = 0, ShadowMapMode = 1, DepthPrepassMode = 2;
    @Override
    public void init(Renderer renderer) {
        this.renderer = renderer;
        this.graph = new RenderGraph(renderer);

        //Depth prepass
        {
            nDepthPrepassTextures = new Texture[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < nDepthPrepassTextures.length; i++) {
                nDepthPrepassTextures[i] = Texture.newDepthTexture(depthPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32);
            }
            depthPassRT = new RenderTarget(renderer);
            depthPassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }

        //Tiled lighting data
        {

            tiledLightingShaderProgram = ShaderProgram.newShaderProgram(graph);
            tiledLightingShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forwardplus/TiledLightCulling_compute.spv"), ShaderType.ComputeShader);
            tiledLightingShaderProgram.assemble();

            tiledLightingDataBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < tiledLightingDataBuffers.length; i++) {
                tiledLightingDataBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Float.BYTES * 4 * 1000,
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.GPULocal,
                        false
                );
            }




        }

        //Scene pass
        {
            nSceneColorTextures = new Texture[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < nSceneColorTextures.length; i++) {
                nSceneColorTextures[i] = Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR16G16B16A16);
            }
            scenePassRT = new RenderTarget(renderer);
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Color0, nSceneColorTextures, null));
            scenePassRT.addAttachment(new Attachment(AttachmentTypes.Depth, nDepthPrepassTextures, null));
        }

        sampler = Sampler.newSampler(scenePassRT, Linear, Linear, false);


        /*
        shadowMapPass = Pass.newGraphicsPass(graph, "Shadow", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.writes("NShadowTextures", null, AccessTypes.DepthWrite);
        }


        depthPass = Pass.newGraphicsPass(graph, "Depth", renderer.getMaxFramesInFlight());
        {
            depthPass.writes("NDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthWrite);
        }

        tiledLightCullingPass = Pass.newComputePass(graph, "Forward+ Tiled Light Culling", renderer.getMaxFramesInFlight());
        {
            tiledLightCullingPass.writes("NTiledLightingData", tiledLightingDataBuffers, AccessTypes.DepthRead);
        }


        scenePass = Pass.newGraphicsPass(graph, "Scene", renderer.getMaxFramesInFlight());
        {
            //scenePass.reads("ITiledLightingData", tiledLightingDataBuffers, DependencyTypes.FragmentShaderRead);
            scenePass.reads("IShadowTextures", null, AccessTypes.ShaderRead);
            scenePass.reads("IDepthPrepassTextures", nDepthPrepassTextures, AccessTypes.DepthReadWrite);
            scenePass.writes("NRenderTextures", nSceneColorTextures, AccessTypes.ColorWrite);
        }*/


        uiPass = Pass.newGraphicsPass(graph, "UI", renderer.getMaxFramesInFlight());
        {
            //uiPass.reads("IRenderTextures", nSceneColorTextures, AccessTypes.ShaderRead);
            uiPass.writes("NSwapchainTextures", null, AccessTypes.ColorWrite);
            uiPass.writes("NSwapchainTexturesPresent", null, AccessTypes.Present);
        }

        graph.addPasses(
                //shadowMapPass,
                //depthPass,
                //tiledLightCullingPass,
                //scenePass,
                uiPass
        );
    }

    @Override
    public List<Pass> buildFrame(ScenePack scenePack) {

        scenePack.uiShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", nSceneColorTextures[renderer.getFrameIndex()]).arrayIndex(0));
        scenePack.uiShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(0));
        Texture[] swapchainTextures = renderer.getSwapchainRenderTarget()
                .getAttachment(AttachmentTypes.Color0)
                .getTextures();
        /*
        Texture[] shadowMapTextures = new Texture[scenePack.lights().size()];

        List<LightData> lights = scenePack.lights();
        for (int i = 0; i < lights.size(); i++) {
            LightData lightData = lights.get(i);
            shadowMapTextures[i] = lightData.renderTarget
                    .getAttachment(AttachmentTypes.Depth)
                    .getTextures()[renderer.getFrameIndex()];
        }

        List<RenderSystem.IndexedDrawCall> drawCalls = scenePack.drawCalls();
        for (RenderSystem.IndexedDrawCall drawCall : drawCalls) {
            for (int j = 0; j < scenePack.lights().size(); j++) {
                drawCall.shaderProgram.setTextures(
                        renderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps", shadowMapTextures[j]).arrayIndex(j)
                );
            }
            drawCall.shaderProgram.setSamplers(
                    renderer.getFrameIndex(), new DescriptorUpdate<>("input_shadow_maps_sampler", sampler)
            );
            drawCall.shaderProgram.setBuffers(renderer.getFrameIndex(), new DescriptorUpdate<>("tiled_lighting_data", tiledLightingDataBuffers[renderer.getFrameIndex()]));
        }



        shadowMapPass.bind("NShadowTextures", shadowMapTextures);
        shadowMapPass.submit(() -> {
            List<LightData> lightDataList = scenePack.lights();
            for (int i = 0; i < lightDataList.size(); i++) {
                LightData lightData = lightDataList.get(i);
                shadowMapPass.startRendering(lightData.renderTarget, 0, 1024, 1024, true, Color.BLACK);
                shadowMapPass.setCullMode(CullMode.Front);

                for (RenderSystem.IndexedDrawCall drawCall : scenePack.drawCalls()) {
                    shadowMapPass.setShaderProgram(drawCall.shaderProgram);
                    shadowMapPass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                        pPushConstants.putInt(ShadowMapMode);
                        pPushConstants.putInt(i);
                        pPushConstants.putInt(scenePack.lights().size());
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
                for(RenderSystem.IndexedDrawCall drawCall : scenePack.drawCalls()) {
                    depthPass.setCullMode(CullMode.Back);
                    depthPass.setShaderProgram(drawCall.shaderProgram);
                    depthPass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                        pPushConstants.putInt(DepthPrepassMode);
                        pPushConstants.putInt(-1);
                        pPushConstants.putInt(scenePack.lights().size());
                        depthPass.setPushConstants(pPushConstants);
                    }
                    if(drawCall.instanced) depthPass.drawInstanced(drawCall.indexCount, drawCall.instanceCount);
                    else depthPass.drawIndexed(drawCall.indexCount);
                }
            }
            depthPass.endRendering();

        });
        tiledLightCullingPass.submit(() -> {
            tiledLightCullingPass.setShaderProgram(tiledLightingShaderProgram);
            try (MemoryStack stack = stackPush()) {
                ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                pPushConstants.putInt(renderer.getWidth());
                pPushConstants.putInt(renderer.getHeight());
                pPushConstants.putInt(64);
                tiledLightCullingPass.setPushConstants(pPushConstants);
            }
            tiledLightCullingPass.dispatch(lights.size(), 1, 1);
        });




        scenePass.bind("IShadowTextures", shadowMapTextures);
        scenePass.submit(() -> {
            scenePass.startRendering(scenePassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
            {
                for(RenderSystem.IndexedDrawCall drawCall : scenePack.drawCalls()) {
                    scenePass.setCullMode(CullMode.Back);
                    scenePass.setShaderProgram(drawCall.shaderProgram);
                    scenePass.setDrawBuffers(drawCall.vertexBuffer, drawCall.indexBuffer);
                    try (MemoryStack stack = stackPush()) {
                        ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3);
                        pPushConstants.putInt(PbrMode);
                        pPushConstants.putInt(-1);
                        pPushConstants.putInt(scenePack.lights().size());
                        scenePass.setPushConstants(pPushConstants);
                    }
                    if(drawCall.instanced) scenePass.drawInstanced(drawCall.indexCount, drawCall.instanceCount);
                    else scenePass.drawIndexed(drawCall.indexCount);
                }
            }
            scenePass.endRendering();

        });*/

        uiPass.bind("NSwapchainTextures", swapchainTextures);
        uiPass.bind("NSwapchainTexturesPresent", swapchainTextures);
        uiPass.submit(() -> {
            uiPass.startRendering(renderer.getSwapchainRenderTarget(), 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
            {
                uiPass.setDrawBuffers(scenePack.uiVertexBuffer(), scenePack.uiIndexBuffer());
                uiPass.setCullMode(CullMode.Back);
                uiPass.setShaderProgram(scenePack.uiShaderProgram());
                try (MemoryStack stack = stackPush()) {
                    ByteBuffer pPushConstants = stack.calloc(SizeUtil.MATRIX_SIZE_BYTES * 2);
                    scenePack.uiCamera().getView().get(pPushConstants);
                    scenePack.uiCamera().getProj().get(SizeUtil.MATRIX_SIZE_BYTES, pPushConstants);
                    uiPass.setPushConstants(pPushConstants);
                }
                uiPass.drawIndexed(scenePack.uiQuadCount() * 6);
            }
            uiPass.endRendering();
        });

        return graph.compile(uiPass);
    }
}
