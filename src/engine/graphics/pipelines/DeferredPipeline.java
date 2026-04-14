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


public class DeferredPipeline extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Pair<Texture[], Sampler[]>> sceneColor0Textures;
    private Resource<Pair<Texture[], Sampler[]>> sceneColor1Textures;
    private Resource<Pair<Texture[], Sampler[]>> sceneDepthStencilTextures;

    private ComputePass lightingPass;
    private RenderTarget lightingPassRT;
    private Resource<Pair<Texture[], Sampler[]>> lightingPassColorTextures;
    private ShaderProgram lightingPassShaderProgram;
    private Buffer[] lightingPassSceneDescBuffers;
    private Resource<Pair<Texture[], Sampler[]>> lightingPassBloomTextures;

    private GraphicsPass displayPass;
    private RenderTarget displayPassRT;
    private Resource<Texture[]> displayPassColorTextures;

    private ShaderProgram displayPassShaderProgram;
    private Camera displayPassCamera;
    private Buffer[] displayPassVertexBuffers, displayPassIndexBuffers;
    private Buffer[] displayPassCameraBuffers;

    private int lightCount = 0;
    private final int COMPUTE_THREAD_GROUP_SIZE = 32;




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




        //Scene Color Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            sceneColor0Textures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16)
                            },
                            null
                    )
            );

            sceneColor1Textures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16)
                            },
                            null
                    )
            );



            sceneDepthStencilTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32),
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32)
                            },
                            null
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color0,
                            sceneColor0Textures.get().key,
                            sceneColor0Textures.get().value
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color1,
                            sceneColor1Textures.get().key,
                            sceneColor1Textures.get().value

                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Depth,
                            sceneDepthStencilTextures.get().key,
                            sceneDepthStencilTextures.get().value

                    )
            );
        }

        //Lighting Pass Resources
        {
            lightingPassRT = new RenderTarget(renderer);
            lightingPassColorTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16),
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth(), renderer.getHeight(), Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true),
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true)
                            }
                    )
            );

            lightingPassBloomTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth() / 2, renderer.getHeight() / 2, Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16),
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth() / 2, renderer.getHeight() / 2, Session.isDiscrete() ? TextureFormatType.ColorR32G32B32A32 : TextureFormatType.ColorR16G16B16A16)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true),
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true)
                            }
                    )
            );

            lightingPassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color0,
                            lightingPassColorTextures.get().key,
                            lightingPassColorTextures.get().value
                    )
            );

            lightingPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            lightingPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred/Lighting_compute.spv"), ShaderType.ComputeShader);
            lightingPassShaderProgram.assemble();

            lightingPassSceneDescBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                lightingPassSceneDescBuffers[i] = Buffer.newBuffer(
                        renderer,
                        lightingPassShaderProgram.getDescriptorByName("scene_desc").getSizeBytes(),
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

        }

        //Display Pass Resources
        {
            displayPassRT = renderer.getSwapchainRenderTarget();
            displayPassColorTextures = new Resource<>(
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
                            "OutputShadowMaps",
                            null,
                            DependencyTypes.RenderTargetDepthWrite
                    )
            );
        }
        scenePass = Pass.newGraphicsPass(renderGraph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.addDependencies(
                    new Dependency(
                            "OutputColorTextures",
                            sceneColor0Textures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "OutputPosTextures",
                            sceneColor1Textures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "OutputDepthStencilTextures",
                            sceneDepthStencilTextures,
                            DependencyTypes.RenderTargetDepthWrite
                    )
            );

        }


        lightingPass = Pass.newComputePass(renderer, "Lighting", renderer.getMaxFramesInFlight());
        {
            lightingPass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            sceneColor0Textures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputPosTextures",
                            sceneColor1Textures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputShadowMaps",
                            null,
                            DependencyTypes.ComputeShaderReadDepth
                    ),
                    new Dependency(
                            "InputDepthStencilTextures",
                            sceneDepthStencilTextures,
                            DependencyTypes.ComputeShaderReadDepth
                    ),
                    new Dependency(
                            "OutputBloomTextures",
                            lightingPassBloomTextures,
                            DependencyTypes.ComputeShaderWrite
                    ),
                    new Dependency(
                            "OutputColorTextures",
                            lightingPassColorTextures,
                            DependencyTypes.ComputeShaderWrite
                    )
            );
        }
        displayPass = Pass.newGraphicsPass(renderGraph, "Compose", renderer.getMaxFramesInFlight());
        {
            displayPass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            lightingPassColorTextures,
                            DependencyTypes.FragmentShaderRead
                    ),
                    new Dependency(
                            "SwapchainColorTextures",
                            displayPassColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "InputBloomTextures",
                            lightingPassBloomTextures,
                            DependencyTypes.FragmentShaderRead
                    ),
                    new Dependency(
                            "SwapchainColorTexturesPresent",
                            displayPassColorTextures,
                            DependencyTypes.Present
                    )
            );
        }

        renderGraph.addPasses(
                shadowMapGenPass,
                scenePass,
                lightingPass,
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




        if(displayPassRT != renderer.getSwapchainRenderTarget()) {
            displayPassRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color0);
            displayPassColorTextures = new Resource<>(colorAttachment.getTextures());

            displayPass.getDependency(
                    "SwapchainColorTextures"
            ).setDependency(displayPassColorTextures);

            displayPass.getDependency(
                    "SwapchainColorTexturesPresent"
            ).setDependency(displayPassColorTextures);

        }

        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(CameraComponent.class)) {
                CameraComponent cameraComponent = actor.getComponent(CameraComponent.class);
                sceneCamera = cameraComponent.camera;
            }
        });




        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
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


            Resource<Pair<Texture[], Sampler[]>> shadowMapTexturesResource = new Resource<>(new Pair<>(shadowMapTextures, shadowMapSamplers));


            shadowMapGenPass.getDependency(
                    "OutputShadowMaps"
            ).setDependency(shadowMapTexturesResource);

            lightingPass.getDependency(
                    "InputShadowMaps"
            ).setDependency(shadowMapTexturesResource);
        }

        //Update all in-memory scene desc/transform buffers before their descriptors are updated inside the passes
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

            //Update ShadowMapPass shaders
            {
                ByteBuffer sceneDescData = lightingPassSceneDescBuffers[renderer.getFrameIndex()].get();
                updateSceneDesc(sceneDescData, sceneCamera, scene);
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
                                                ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                                pPushConstants.putInt(mode);
                                                pPushConstants.putInt(shadowMapGenPassLightIndex);
                                                shadowMapGenPass.setPushConstants(pPushConstants);
                                            }
                                            shadowMapGenPass.drawIndexed(meshComponent.indexCount);
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
                                        ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                        pPushConstants.putInt(mode);
                                        pPushConstants.putInt(-1);
                                        scenePass.setPushConstants(pPushConstants);
                                    }
                                    scenePass.drawIndexed(meshComponent.indexCount);
                                }
                            }

                        });



                    }
                    scenePass.endRendering();
                }


            }
            scenePass.endRecording();

        });


        lightingPass.setPassExecuteCallback(() -> {

            lightingPassShaderProgram.setBuffers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "scene_desc",
                            lightingPassSceneDescBuffers[renderer.getFrameIndex()]
                    )
            );

            Resource<Pair<Texture[], Sampler[]>> inputColorTexturesDependency = lightingPass.getDependency("InputColorTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> outputColorTexturesDependency = lightingPass.getDependency("OutputColorTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> outputBloomTexturesDependency = lightingPass.getDependency("OutputBloomTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> inputPosTexturesDependency = lightingPass.getDependency("InputPosTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> inputDepthStencilTexturesDependency = lightingPass.getDependency("InputDepthStencilTextures").getResource();

            SkyboxFeatures skyboxFeatures = getFeatures(SkyboxFeatures.class);


            lightingPassShaderProgram.setTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "input_texture0",
                            inputColorTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "input_texture1",
                            inputPosTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "input_depth_stencil_psw_texture",
                            inputDepthStencilTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "output_color_texture",
                            outputColorTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "input_skybox_texture",
                            skyboxFeatures.getSkyboxTexture()
                    ),
                    new DescriptorUpdate<>(
                            "output_bloom_texture",
                            outputBloomTexturesDependency.get().key[renderer.getFrameIndex()]
                    )
            );
            lightingPassShaderProgram.setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_skybox_texture_sampler", skyboxFeatures.getSkyboxSampler()));




            //Update shadow maps
            {
                Resource<Pair<Texture[], Sampler[]>> shadowMapTexturesResource = lightingPass.getDependency("InputShadowMaps").getResource();


                for (int i = 0; i < lightCount; i++) {
                    lightingPassShaderProgram.setTextures(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "input_shadow_maps",
                                    shadowMapTexturesResource.get().key[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );
                    lightingPassShaderProgram.setSamplers(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "input_shadow_maps_samplers",
                                    shadowMapTexturesResource.get().value[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );

                }
            }

            lightingPass.startRecording(renderer.getFrameIndex());
            {
                lightingPass.resolveBarriers();
                lightingPass.setShaderProgram(lightingPassShaderProgram);

                try(MemoryStack stack = stackPush()) {
                    ByteBuffer pPushConstants = stack.calloc(Integer.BYTES * 3 + Float.BYTES);
                    pPushConstants.putInt(lightCount);
                    pPushConstants.putInt(renderer.getWidth());
                    pPushConstants.putInt(renderer.getHeight());
                    pPushConstants.putFloat((int) getFeatures(SkyboxFeatures.class).getSampleIntensity());
                    lightingPass.setPushConstants(pPushConstants);
                }

                int groupCountX = (int) Math.ceil((float) renderer.getWidth() / COMPUTE_THREAD_GROUP_SIZE);
                int groupCountY = (int) Math.ceil((float) renderer.getHeight() / COMPUTE_THREAD_GROUP_SIZE);

                lightingPass.dispatch(groupCountX, groupCountY, 1);
            }
            lightingPass.endRecording();
        });



        displayPass.setPassExecuteCallback(() -> {

            //Lighting main output
            {
                Resource<Pair<Texture[], Sampler[]>> inputTexturesResource = displayPass.getDependency("InputColorTextures").getResource();

                displayPassShaderProgram.setTextures(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_textures",
                                inputTexturesResource.get().key[renderer.getFrameIndex()]
                        ).arrayIndex(0)
                );

                displayPassShaderProgram.setSamplers(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_samplers",
                                inputTexturesResource.get().value[renderer.getFrameIndex()]
                        ).arrayIndex(0)
                );
            }

            //Lighting bloom
            {
                Resource<Pair<Texture[], Sampler[]>> inputBloomTexturesResource = displayPass.getDependency("InputBloomTextures").getResource();

                displayPassShaderProgram.setTextures(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_textures",
                                inputBloomTexturesResource.get().key[renderer.getFrameIndex()]
                        ).arrayIndex(1)
                );

                displayPassShaderProgram.setSamplers(
                        renderer.getFrameIndex(),
                        new DescriptorUpdate<>(
                                "input_samplers",
                                inputBloomTexturesResource.get().value[renderer.getFrameIndex()]
                        ).arrayIndex(1)
                );
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
