package engine.graphics.pipelines;

import engine.util.Pair;
import engine.graphics.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;


public class ForwardPipeline extends RenderPipeline {


    private GraphicsPass uiPass;
    private RenderTarget rt_graphics2DPass;
    private Resource<Texture[]> r_swapchainTextures;
    private Renderer renderer;


    @Override
    public void init(Renderer renderer) {
        this.renderer = renderer;
        this.graph = new RenderGraph(renderer);

        //Features
        {
            supportedFeatures = List.of(
                    new SceneFeatures(true),
                    new SkyboxFeatures(true)
            );
        }

        {
            rt_graphics2DPass = renderer.getSwapchainRenderTarget();
            r_swapchainTextures = new Resource<>(
                    rt_graphics2DPass.getAttachment(RenderTargetAttachmentTypes.Color0).getTextures()
            );
        }


        uiPass = Pass.newGraphicsPass(graph, "Display", renderer.getMaxFramesInFlight());
        {
            uiPass.addDependencies(
                    new Dependency(
                            "NSwapchainTextures",
                            r_swapchainTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "NSwapchainTexturesPresent",
                            r_swapchainTextures,
                            DependencyTypes.Present
                    )
            );
        }

        graph.addPasses(
                uiPass
        );
    }

    @Override
    public List<Pass> buildFrame(ScenePack scenePack) {
        //Update the dependency with the new swapchain
        if(rt_graphics2DPass != renderer.getSwapchainRenderTarget()) {
            rt_graphics2DPass = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = rt_graphics2DPass.getAttachment(RenderTargetAttachmentTypes.Color0);
            r_swapchainTextures = new Resource<>(colorAttachment.getTextures());

            uiPass.getDependency(
                    "NSwapchainTextures"
            ).setResource(r_swapchainTextures);

            uiPass.getDependency(
                    "NSwapchainTexturesPresent"
            ).setResource(r_swapchainTextures);
        }

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
