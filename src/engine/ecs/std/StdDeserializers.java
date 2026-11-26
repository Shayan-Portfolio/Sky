package engine.ecs.std;

import engine.asset.AssetRegistry;
import engine.bridge.ProjectLoader;
import engine.ecs.*;
import engine.graphics.*;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.ecs.compiler.Bytecode;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class StdDeserializers {
    public static void install(Scene scene) {
        {
            scene.registerDeserializer("MeshComponent", (iterator) -> {
                Actor actor = scene.getActorBeingLoaded();

                Bytecode type = iterator.next();
                Bytecode params = iterator.next();
                Bytecode maxVertexCount = iterator.next();
                Bytecode maxIndexCount = iterator.next();

                MeshComponent meshComponent = new MeshComponent(
                        actor,
                        1,
                        (int) ((float) maxVertexCount.operands()[2]),
                        (int) ((float) maxIndexCount.operands()[2]),
                        actor.getComponent(ShaderComponent.class).shaderProgram()
                );

                switch ((String) type.operands()[2]) {
                    case "box": {
                        float width = (float) params.operands()[2];
                        float height = (float) params.operands()[3];
                        float depth = (float) params.operands()[4];

                        MeshData meshData = MeshGenerator.newBox(width, height, depth);
                        meshComponent.setMeshData(meshData);
                        break;
                    }
                    case "cylinder": {
                        float radius = (float) params.operands()[2];
                        float height = (float) params.operands()[3];
                        int segments = (int) (float) params.operands()[4];

                        meshComponent.setMeshData(MeshGenerator.newCylinder(radius, height, segments));
                        break;
                    }
                    case "gltf": {

                        Logger.todo(Scene.class, "GLTF loading is not implemented");

                        /*Asset<String> gltf = AssetRegistry.getAsset((String) params.operands()[1]);
                        Asset<byte[]>[] glbs = new Asset[params.operands().length - 2];

                        for(int i = 2; i < params.operands().length; i++) {
                            glbs[i - 2] = AssetRegistry.getAsset((String) params.operands()[i]);
                        }


                        meshComponent.setMeshData(Importer.loadGLTF2(
                                0.1f,
                                gltf,
                                glbs
                        ));*/

                        break;
                    }
                }

                return meshComponent;
            });
            scene.registerDeserializer("ScriptComponent", (iterator) -> {
                Bytecode script = iterator.next();
                Logger.info(Scene.class, "Loading script " + script.operands()[2]);

                try {
                    Class clazz = ProjectLoader.getClassLoader().loadClass((String) script.operands()[2]);
                    Constructor constructor = clazz.getConstructor();
                    Script s = (Script) constructor.newInstance();
                    return (new ScriptComponent(s));
                } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                         InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            scene.registerDeserializer("MaterialComponent", (iterator) -> {
                Actor actor = scene.getActorBeingLoaded();
                Bytecode baseColor = iterator.next();
                Bytecode normal = iterator.next();
                Bytecode metallic = iterator.next();
                Bytecode roughness = iterator.next();
                return (new MaterialComponent(new Material(
                        Sampler.newSampler(actor, Texture.Filter.Linear, Texture.Filter.Linear, true),
                        Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) baseColor.operands()[2]), TextureFormatType.ColorR8G8B8A8),
                        Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) normal.operands()[2]), TextureFormatType.ColorR8G8B8A8unorm),
                        Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) metallic.operands()[2]), TextureFormatType.ColorR8G8B8A8unorm),
                        Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) roughness.operands()[2]), TextureFormatType.ColorR8G8B8A8unorm)
                )));
            });
            scene.registerDeserializer("TransformComponent", (iterator) -> {
                Bytecode translate = iterator.next();
                Bytecode rotateAxis = iterator.next();
                Bytecode rotateDeg = iterator.next();

                Vector3f translation = new Vector3f(
                        (float) translate.operands()[2],
                        (float) translate.operands()[3],
                        (float) translate.operands()[4]
                );

                Vector3f rotationAxis = new Vector3f(
                        (float) rotateAxis.operands()[2],
                        (float) rotateAxis.operands()[3],
                        (float) rotateAxis.operands()[4]
                );

                float rotation = (float) Math.toRadians((float) rotateDeg.operands()[2]);


                return (new TransformComponent(new Matrix4f().identity().rotate(rotation, rotationAxis).translate(translation)));
            });
            scene.registerDeserializer("ShaderComponent", (iterator) -> {
                Actor actor = scene.getActorBeingLoaded();
                Bytecode vertexShader = iterator.next();
                Bytecode fragmentShader = iterator.next();

                ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(actor);
                shaderProgram.add(
                        AssetRegistry.getAsset((String) vertexShader.operands()[2]),
                        ShaderType.VertexShader
                );
                shaderProgram.add(
                        AssetRegistry.getAsset((String) fragmentShader.operands()[2]),
                        ShaderType.FragmentShader
                );

                shaderProgram.assemble();

                return new ShaderComponent(shaderProgram);
            });
            scene.registerDeserializer("LightComponent", (iterator) -> {
                Actor actor = scene.getActorBeingLoaded();

                Bytecode fovDeg = iterator.next();
                Bytecode eye = iterator.next();
                Bytecode center = iterator.next();
                Bytecode up = iterator.next();
                Bytecode aspectRatio = iterator.next();
                Bytecode zNear = iterator.next();
                Bytecode zFar = iterator.next();
                Bytecode zZeroToOne = iterator.next();
                Bytecode invertY = iterator.next();
                Bytecode color = iterator.next();


                LightComponent lightComponent = new LightComponent(
                        actor,
                        new LightData(
                                new Matrix4f().lookAt(
                                        new Vector3f(
                                                ((float) eye.operands()[2]),
                                                ((float) eye.operands()[3]),
                                                ((float) eye.operands()[4])
                                        ),
                                        new Vector3f(
                                                ((float) center.operands()[2]),
                                                ((float) center.operands()[3]),
                                                ((float) center.operands()[4])
                                        ),
                                        new Vector3f(
                                                ((float) up.operands()[2]),
                                                ((float) up.operands()[3]),
                                                ((float) up.operands()[4])
                                        )
                                ),
                                new Matrix4f().perspective(
                                        (float) Math.toRadians(
                                                (float) fovDeg.operands()[2]
                                        ),
                                        ((float) aspectRatio.operands()[2]),
                                        ((float) zNear.operands()[2]),
                                        ((float) zFar.operands()[2]),
                                        ((boolean) zZeroToOne.operands()[2])
                                ),
                                ((boolean) invertY.operands()[2]),
                                new Color(
                                        ((float) color.operands()[2]),
                                        ((float) color.operands()[3]),
                                        ((float) color.operands()[4]),
                                        1f
                                )
                        )

                );
                return lightComponent;
            });
            scene.registerDeserializer("MujocoComponent", (iterator) -> {
                throw new SkyRuntimeException("Not implemented");
            });
        }

    }
}
