package engine.graphics.pipelines;

import engine.util.Pair;
import engine.asset.AssetRegistry;
import engine.ecs.*;
import engine.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.*;

import java.nio.ByteBuffer;
import java.util.List;


public class ForwardPipeline extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Pair<Texture[], Sampler[]>> r_sceneColorTextures;
    private Resource<Pair<Texture[], Sampler[]>> r_sceneHDRTextures;
    private Resource<Pair<Texture[], Sampler[]>> r_sceneDepthStencilTextures;

    private GraphicsPass displayPass;
    private RenderTarget displayPassRT;
    private Resource<Texture[]> r_swapchainTextures;

    private ShaderProgram displayPassShaderProgram;
    private Camera displayPassCamera;
    private Buffer[] displayPassVertexBuffers, displayPassIndexBuffers;
    private Buffer[] displayPassCameraBuffers;
    private int lightCount = 0;


    @Override
    public void init(Renderer renderer) {

        //Features
        {
            supportedFeatures = List.of(
                    new SceneFeatures(true),
                    new ScreenSpaceFeatures(false),
                    new SkyboxFeatures(true)
            );
        }

        TextureFormatType textureFormatType = TextureFormatType.ColorR16G16B16A16;



        //Scene Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            r_sceneColorTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), textureFormatType),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), textureFormatType)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false),
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false)
                            }
                    )
            );

            r_sceneHDRTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), textureFormatType),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), textureFormatType)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false),
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false)
                            }
                    )
            );



            r_sceneDepthStencilTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32),
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false),
                                    Sampler.newSampler(scenePassRT, Linear, Linear, false)
                            }
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color0,
                            r_sceneColorTextures.get().key,
                            r_sceneColorTextures.get().value
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color1,
                            r_sceneHDRTextures.get().key,
                            r_sceneHDRTextures.get().value

                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Depth,
                            r_sceneDepthStencilTextures.get().key,
                            r_sceneDepthStencilTextures.get().value

                    )
            );
        }

        //2D Pass Resources
        {
            displayPassRT = renderer.getSwapchainRenderTarget();
            r_swapchainTextures = new Resource<>(
                    displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color0).getTextures()
            );

            displayPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );


            ScreenSpaceFeatures screenSpaceFeatures = getFeatures(ScreenSpaceFeatures.class);

            displayPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            displayPassShaderProgram.setDepthTestType(DepthTestType.Always);
            displayPassShaderProgram.setEnableBlending(true);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred/Display_vertex.spv"), ShaderType.VertexShader);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred/Display_fragment.spv"), ShaderType.FragmentShader);
            displayPassShaderProgram.assemble();

            displayPassVertexBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            displayPassIndexBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            displayPassCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];

            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                displayPassVertexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        displayPassShaderProgram.getVertexAttributesSize() * Float.BYTES * 4 * screenSpaceFeatures.getMaxQuads(),
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
                displayPassIndexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Integer.BYTES * 6 * screenSpaceFeatures.getMaxQuads(),
                        Buffer.Usage.IndexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );

                displayPassCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        displayPassShaderProgram.getDescriptorByName("camera").getSizeBytes(),
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );

                ByteBuffer swapchainPassCameraBufferData = displayPassCameraBuffers[i].get();

                displayPassCamera.getProj().get(0, swapchainPassCameraBufferData);
                displayPassShaderProgram.setBuffers(
                        i,
                        new DescriptorUpdate<>("camera", displayPassCameraBuffers[i])
                );


            }


            screenSpaceFeatures.setVertexBuffers(displayPassVertexBuffers);
            screenSpaceFeatures.setIndexBuffers(displayPassIndexBuffers);
            screenSpaceFeatures.setShaderProgram(displayPassShaderProgram);




        }

        renderGraph = new RenderGraph(renderer);

        shadowMapGenPass = Pass.newGraphicsPass(renderGraph, "ShadowMapGen", renderer.getMaxFramesInFlight());
        {
            shadowMapGenPass.addDependencies(
                    new Dependency(
                            "NShadowMaps",
                            null,
                            DependencyTypes.RenderTargetWriteDepth
                    )
            );
        }
        scenePass = Pass.newGraphicsPass(renderGraph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.addDependencies(
                    new Dependency(
                            "IShadowMaps",
                            null,
                            DependencyTypes.FragmentShaderReadDepth
                    ),
                    new Dependency("NSceneHDRTextures", r_sceneHDRTextures, DependencyTypes.RenderTargetWrite),
                    new Dependency(
                            "NColorTextures",
                            r_sceneColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "NDepthStencilTextures",
                            r_sceneDepthStencilTextures,
                            DependencyTypes.RenderTargetWriteDepth
                    )
            );

        }



        displayPass = Pass.newGraphicsPass(renderGraph, "Display", renderer.getMaxFramesInFlight());
        {
            displayPass.addDependencies(
                    new Dependency("ISceneColorTextures", r_sceneColorTextures, DependencyTypes.FragmentShaderRead),
                    new Dependency("ISceneHDRTextures", r_sceneHDRTextures, DependencyTypes.FragmentShaderRead),
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

        renderGraph.addPasses(
                shadowMapGenPass,
                scenePass,
                displayPass
        );
    }

    private void updateSceneDesc(ByteBuffer sceneDescData, Camera camera, Scene scene) {
        sceneDescData.clear();

        camera.getView().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getProj().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getInvView().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getInvProj().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(LightComponent.class)) {
                LightComponent lightComponent = actor.getComponent(LightComponent.class);

                lightComponent.view.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                lightComponent.proj.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                lightComponent.invView.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                lightComponent.invProj.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                sceneDescData.putFloat(lightComponent.attenuationConstant);
                sceneDescData.putFloat(lightComponent.attenuationLinear);
                sceneDescData.putFloat(lightComponent.attenuationQuadratic);
                sceneDescData.putFloat(lightComponent.shadowTestOffsetBias);
                lightComponent.color.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.VEC3_SIZE_BYTES);
                sceneDescData.putFloat(lightComponent.shadowNormalOffsetBias);
            }
        });




    }


    @Override
    public void render(Renderer renderer) {
        Scene scene = getFeatures(SceneFeatures.class).getScene();




        //Update the dependency with the new swapchain
        if(displayPassRT != renderer.getSwapchainRenderTarget()) {
            displayPassRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color0);
            r_swapchainTextures = new Resource<>(colorAttachment.getTextures());

            displayPass.getDependency(
                    "NSwapchainTextures"
            ).setDependency(r_swapchainTextures);

            displayPass.getDependency(
                    "NSwapchainTexturesPresent"
            ).setDependency(r_swapchainTextures);

        }

        //Acquire Camera
        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(CameraComponent.class)) {
                CameraComponent cameraComponent = actor.getComponent(CameraComponent.class);
                sceneCamera = cameraComponent.camera;
            }
        });

        //Update all shadow map descriptors
        {

            lightCount = 0;

            scene.getRootActor().previsitAllActors(actor -> {
                if (actor.has(LightComponent.class))
                    lightCount++;
            });



            Texture[] shadowMapTextures = new Texture[lightCount * renderer.getMaxFramesInFlight()];
            Sampler[] shadowMapSamplers = new Sampler[shadowMapTextures.length];

            final int[] lightIndex = {0};

            scene.getRootActor().previsitAllActors(actor -> {
                if (actor.has(LightComponent.class)) {
                    LightComponent lightComponent = actor.getComponent(LightComponent.class);
                    int i1 = renderer.getMaxFramesInFlight() * lightIndex[0];
                    int i2 = renderer.getMaxFramesInFlight() * lightIndex[0] + 1;

                    shadowMapTextures[i1] = lightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getTextures()[0];

                    shadowMapSamplers[i1] = lightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getSamplers()[0];

                    shadowMapTextures[i2] = lightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getTextures()[1];

                    shadowMapSamplers[i2] = lightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getSamplers()[1];

                    lightIndex[0]++;
                }
            });


            Resource<Pair<Texture[], Sampler[]>> r_shadowMapTextures = new Resource<>(new Pair<>(shadowMapTextures, shadowMapSamplers));


            shadowMapGenPass.getDependency(
                    "NShadowMaps"
            ).setDependency(r_shadowMapTextures);

            scenePass.getDependency(
                    "IShadowMaps"
            ).setDependency(r_shadowMapTextures);
        }

        //Ensure all descriptors are up to date
        {
            //Update entity shaders
            {
                scene.getRootActor().previsitAllActors(actor -> {
                    if(actor.has(TransformComponent.class)) {
                        TransformComponent transformComponent = actor.getComponent(TransformComponent.class);


                        if(actor.has(MeshComponent.class)) {
                            MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                            if(meshComponent.isVisible()) {
                                ByteBuffer transformsData = meshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                                transformComponent.transform().get(0, transformsData);
                                ByteBuffer sceneDescData = meshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                                updateSceneDesc(sceneDescData, sceneCamera, scene);
                            }
                        }


                    }
                });
            }
        }

        shadowMapGenPassLightIndex = 0;
        shadowMapGenPass.setPassExecuteCallback(() -> {
            shadowMapGenPass.startRecording(renderer.getFrameIndex());
            {
                shadowMapGenPass.resolveBarriers();

                //Shadow Map Gen (mode 1)
                {
                    int mode = 1;

                    scene.getRootActor().previsitAllActors(actor -> {
                        if(actor.has(LightComponent.class)) {
                            LightComponent lightComponent = actor.getComponent(LightComponent.class);



                            int width, height;
                            {
                                Texture texture = lightComponent.renderTarget.getAttachmentByIndex(0).getTextures()[0];
                                width = texture.getWidth();
                                height = texture.getHeight();
                            }


                            shadowMapGenPass.startRendering(lightComponent.renderTarget, 3, width, height, true, Color.BLACK);
                            {
                                shadowMapGenPass.setCullMode(CullMode.Front);


                                scene.getRootActor().previsitAllActors(e -> {

                                    if (e.has(MeshComponent.class)) {
                                        MeshComponent meshComponent = e.getComponent(MeshComponent.class);
                                        if(meshComponent.isVisible()) {
                                            shadowMapGenPass.setDrawBuffers(
                                                    meshComponent.vertexBuffer,
                                                    meshComponent.indexBuffer
                                            );

                                            shadowMapGenPass.setShaderProgram(
                                                    meshComponent.shaderProgram
                                            );
                                            try (MemoryStack stack = stackPush()) {
                                                ByteBuffer pPushConstants = stack.calloc(3 * Integer.BYTES);
                                                pPushConstants.putInt(mode);
                                                pPushConstants.putInt(shadowMapGenPassLightIndex);
                                                pPushConstants.putInt(lightCount);
                                                shadowMapGenPass.setPushConstants(pPushConstants);
                                            }
                                            if(meshComponent.instanced) shadowMapGenPass.drawInstanced(meshComponent.indexCount, meshComponent.instanceCount);
                                            else shadowMapGenPass.drawIndexed(meshComponent.indexCount);
                                        }
                                    }
                                });

                            }
                            shadowMapGenPass.endRendering();
                            shadowMapGenPassLightIndex++;
                        }


                    });



                }

            }
            shadowMapGenPass.endRecording();

        });
        scenePass.setPassExecuteCallback(() -> {
            scene.getRootActor().previsitAllActors(actor -> {
                if (actor.has(MeshComponent.class)) {
                    MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                    if (meshComponent.isVisible()) {
                        Resource<Pair<Texture[], Sampler[]>> r_shadowMapTextures = scenePass.getDependency("IShadowMaps").getResource();
                        for (int i = 0; i < lightCount; i++) {
                            meshComponent.shaderProgram.setTextures(
                                    renderer.getFrameIndex(),
                                    new DescriptorUpdate<>(
                                            "input_shadow_maps",
                                            r_shadowMapTextures.get().key[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                                    ).arrayIndex(i)
                            );
                            meshComponent.shaderProgram.setSamplers(
                                    renderer.getFrameIndex(),
                                    new DescriptorUpdate<>(
                                            "input_shadow_maps_samplers",
                                            r_shadowMapTextures.get().value[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                                    ).arrayIndex(i)
                            );
                        }
                    }
                }
            });

            scenePass.startRecording(renderer.getFrameIndex());
            {

                scenePass.resolveBarriers();

                //Default Rendering (mode 0)
                {
                    int mode = 0;
                    scenePass.startRendering(scenePassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                    {
                        scenePass.setCullMode(CullMode.Back);
                        scene.getRootActor().previsitAllActors(actor -> {
                            if(actor.has(MeshComponent.class)) {
                                MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                                if(meshComponent.isVisible()) {

                                    scenePass.setDrawBuffers(
                                            meshComponent.vertexBuffer,
                                            meshComponent.indexBuffer
                                    );
                                    scenePass.setShaderProgram(
                                            meshComponent.shaderProgram
                                    );
                                    try (MemoryStack stack = stackPush()) {
                                        ByteBuffer pPushConstants = stack.calloc(3 * Integer.BYTES);
                                        pPushConstants.putInt(mode);
                                        pPushConstants.putInt(-1);
                                        pPushConstants.putInt(lightCount);
                                        scenePass.setPushConstants(pPushConstants);
                                    }
                                    if(meshComponent.instanced) scenePass.drawInstanced(meshComponent.indexCount, meshComponent.instanceCount);
                                    else scenePass.drawIndexed(meshComponent.indexCount);
                                }
                            }

                        });



                    }
                    scenePass.endRendering();
                }


            }
            scenePass.endRecording();

        });

        displayPass.setPassExecuteCallback(() -> {

            //Provide display with the latest scene textures
            {
                Resource<Pair<Texture[], Sampler[]>> r_sceneColorTextures = displayPass.getDependency("ISceneColorTextures").getResource();

                displayPassShaderProgram.setTextures(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_textures",
                                r_sceneColorTextures.get().key[renderer.getFrameIndex()]
                        ).arrayIndex(0)
                );

                displayPassShaderProgram.setSamplers(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_samplers",
                                r_sceneColorTextures.get().value[renderer.getFrameIndex()]
                        ).arrayIndex(0)
                );

                /*

                Resource<Pair<Texture[], Sampler[]>> r_sceneHDRTextures = displayPass.getDependency("ISceneHDRTextures").getResource();

                displayPassShaderProgram.setTextures(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_textures",
                                r_sceneHDRTextures.get().key[renderer.getFrameIndex()]
                        ).arrayIndex(1)
                );

                displayPassShaderProgram.setSamplers(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_samplers",
                                r_sceneHDRTextures.get().value[renderer.getFrameIndex()]
                        ).arrayIndex(1)
                );*/
            }

            displayPass.startRecording(renderer.getFrameIndex());
            {

                displayPass.resolveBarriers();

                displayPass.startRendering(renderer.getSwapchainRenderTarget(), 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                {
                    ScreenSpaceFeatures screenSpaceFeatures = getFeatures(ScreenSpaceFeatures.class);
                    screenSpaceFeatures.setShaderProgram(displayPassShaderProgram);


                    displayPass.setCullMode(CullMode.Back);
                    displayPass.setDrawBuffers(displayPassVertexBuffers[renderer.getFrameIndex()], displayPassIndexBuffers[renderer.getFrameIndex()]);
                    displayPass.setShaderProgram(displayPassShaderProgram);
                    displayPass.drawIndexed(screenSpaceFeatures.getIndexCount());
                }
                displayPass.endRendering();



            }
            displayPass.endRecording();
        });



        renderGraph.setTargetPass(displayPass);
        renderer.render(renderGraph);
    }
}
