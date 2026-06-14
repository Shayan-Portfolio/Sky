package engine.graphics.pipelines;

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
    private Texture[] nDepthPrepassTextures;
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



        shadowMapPass = Pass.newGraphicsPass(graph, "Shadow", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.writes("NShadowTextures", null, DependencyTypes.RenderTargetWriteDepth);
        }


        depthPass = Pass.newGraphicsPass(graph, "Depth", renderer.getMaxFramesInFlight());
        {
            depthPass.writes("NDepthPrepassTextures", nDepthPrepassTextures, DependencyTypes.RenderTargetWriteDepth);
        }

        scenePass = Pass.newGraphicsPass(graph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.reads("IShadowTextures", null, DependencyTypes.FragmentShaderReadDepth);
            scenePass.reads("IDepthPrepassTextures", nDepthPrepassTextures, DependencyTypes.RenderTargetReadDepth);
            scenePass.writes("NRenderTextures", nSceneColorTextures, DependencyTypes.RenderTargetWrite);
        }


        uiPass = Pass.newGraphicsPass(graph, "UI", renderer.getMaxFramesInFlight());
        {
            uiPass.reads("IRenderTextures", nSceneColorTextures, DependencyTypes.FragmentShaderRead);
            uiPass.writes("NSwapchainTextures", null, DependencyTypes.RenderTargetWrite);
            uiPass.writes("NSwapchainTexturesPresent", null, DependencyTypes.Present);
        }

        graph.addPasses(
                shadowMapPass,
                depthPass,
                scenePass,
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

        });

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
