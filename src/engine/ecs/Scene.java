package engine.ecs;


import engine.asset.Asset;
import engine.asset.AssetRegistry;
import engine.bridge.ProjectLoader;
import engine.gltf2.Importer;
import engine.graphics.*;
import engine.logging.Logger;
import engine.mio.SceneBytecode;
import engine.mio.Instruction;
import engine.physics.Collider;
import engine.physics.BodyParams;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Scene extends Disposable {
    private List<ActorSystem> systems = new ArrayList<>();
    private Actor root;
    private String name;

    private Actor actor;
    private Object component;


    public Scene(Disposable parent, String name) {
        super(parent);
        this.name = name;
    }

    public void tick() {
        for(ActorSystem system : systems) {
            system.run(root);
        }

    }

    public Actor getRootActor() {
        return root;
    }

    public void setRootActor(Actor root) {
        this.root = root;
        this.actor = root;
        addDisposable(this.actor);
    }

    public Actor newActor(String name, Object... components) {
        Actor actor = new Actor(name);
        for(Object component : components)
            actor.add(component);

        return actor;
    }

    public void exec(Renderer renderer, SceneBytecode sceneBytecode) {

        for (Iterator<Instruction> iterator = sceneBytecode.getList().iterator(); iterator.hasNext(); ) {
            Instruction instruction = iterator.next();
            switch (instruction.opcode()) {
                case PushActor -> {
                    String actorName = (String) instruction.operands()[0];
                    Actor child = new Actor(actorName);
                    if (actor != null) actor.addActor(child);
                    actor = child;
                }

                case AddData -> {

                    switch ((String) instruction.operands()[0]) {

                        case "MaterialComponent" -> {
                            Instruction baseColor = iterator.next();
                            Instruction normal = iterator.next();
                            Instruction metallic = iterator.next();
                            Instruction roughness = iterator.next();


                            actor.add(new MaterialComponent(new Material(
                                    Sampler.newSampler(actor, Texture.Filter.Linear, Texture.Filter.Linear, true),
                                    Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) baseColor.operands()[1]), TextureFormatType.ColorR8G8B8A8),
                                    Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) normal.operands()[1]), TextureFormatType.ColorR8G8B8A8unorm),
                                    Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) metallic.operands()[1]), TextureFormatType.ColorR8G8B8A8unorm),
                                    Texture.newColorTextureFromAsset(actor, AssetRegistry.getAsset((String) roughness.operands()[1]), TextureFormatType.ColorR8G8B8A8unorm)
                            )));

                        }
                        case "TransformComponent" -> {

                            Instruction translate = iterator.next();
                            Instruction rotateAxis = iterator.next();
                            Instruction rotateDeg = iterator.next();

                            Vector3f translation = new Vector3f(
                                    (float) translate.operands()[1],
                                    (float) translate.operands()[2],
                                    (float) translate.operands()[3]
                            );

                            Vector3f rotationAxis = new Vector3f(
                                    (float) rotateAxis.operands()[1],
                                    (float) rotateAxis.operands()[2],
                                    (float) rotateAxis.operands()[3]
                            );

                            float rotation = (float) Math.toRadians((float) rotateDeg.operands()[1]);


                            actor.add(new TransformComponent(new Matrix4f().identity().translate(translation).rotate(rotation, rotationAxis)));
                        }
                        case "RigidBodyComponent" -> {

                            Instruction type = iterator.next();
                            Instruction params = iterator.next();
                            Instruction mass = iterator.next();
                            Instruction interfaceFriction = iterator.next();
                            Instruction interfaceRestitution = iterator.next();
                            Instruction canRotate = iterator.next();


                            Collider collider = null;
                            switch ((String) type.operands()[1]) {
                                case "box": {
                                    float width = (float) params.operands()[1];
                                    float height = (float) params.operands()[2];
                                    float depth = (float) params.operands()[3];
                                    collider = Collider.newBoxCollider(width, height, depth);
                                    break;
                                }
                                case "cylinder": {
                                    float radius = (float) params.operands()[1];
                                    float height = (float) params.operands()[2];

                                    collider = Collider.newCylinderCollider(radius, height);
                                    break;
                                }
                            }

                            float colliderMass = (float) mass.operands()[1];
                            float colliderInterfaceFriction = (float) interfaceFriction.operands()[1];
                            float colliderInterfaceRestitution = (float) interfaceRestitution.operands()[1];
                            boolean colliderCanRotate = (boolean) canRotate.operands()[1];


                            actor.add(new RigidBodyComponent(collider, colliderMass, new BodyParams(colliderInterfaceFriction, colliderInterfaceRestitution), colliderCanRotate));
                        }
                        case "ShaderComponent" -> {

                            Instruction vertexShader = iterator.next();
                            Instruction fragmentShader = iterator.next();

                            ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(actor);
                            shaderProgram.add(
                                    AssetRegistry.getAsset((String) vertexShader.operands()[1]),
                                    ShaderType.VertexShader
                            );
                            shaderProgram.add(
                                    AssetRegistry.getAsset((String) fragmentShader.operands()[1]),
                                    ShaderType.FragmentShader
                            );
                            shaderProgram.assemble();

                            ShaderComponent shaderComponent = new ShaderComponent(shaderProgram);
                            actor.add(shaderComponent);
                        }
                        case "MeshComponent" -> {

                            Instruction type = iterator.next();
                            Instruction params = iterator.next();
                            Instruction maxVertexCount = iterator.next();
                            Instruction maxIndexCount = iterator.next();

                            MeshComponent meshComponent = new MeshComponent(
                                    actor,
                                    renderer,
                                    (int) ((float) maxVertexCount.operands()[1]),
                                    (int) ((float) maxIndexCount.operands()[1]),
                                    actor.getComponent(ShaderComponent.class).shaderProgram()
                            );

                            switch ((String) type.operands()[1]) {
                                case "box": {
                                    float width = (float) params.operands()[1];
                                    float height = (float) params.operands()[2];
                                    float depth = (float) params.operands()[3];

                                    MeshData meshData = MeshGenerator.newBox(width, height, depth);
                                    meshComponent.setMeshData(meshData);
                                    break;
                                }
                                case "cylinder": {
                                    float radius = (float) params.operands()[1];
                                    float height = (float) params.operands()[2];
                                    int segments = (int) (float) params.operands()[3];

                                    meshComponent.setMeshData(MeshGenerator.newCylinder(radius, height, segments));
                                    break;
                                }
                                case "gltf": {

                                    Asset<String> gltf = AssetRegistry.getAsset((String) params.operands()[1]);
                                    Asset<byte[]>[] glbs = new Asset[params.operands().length - 2];

                                    for(int i = 2; i < params.operands().length; i++) {
                                        glbs[i - 2] = AssetRegistry.getAsset((String) params.operands()[i]);
                                    }


                                    meshComponent.setMeshData(Importer.loadGLTF2(
                                            0.1f,
                                            gltf,
                                            glbs
                                    ));

                                    break;
                                }
                            }

                            actor.add(meshComponent);
                        }
                        case "LightComponent" -> {

                            Instruction fovDeg = iterator.next();
                            Instruction eye = iterator.next();
                            Instruction center = iterator.next();
                            Instruction up = iterator.next();
                            Instruction aspectRatio = iterator.next();
                            Instruction zNear = iterator.next();
                            Instruction zFar = iterator.next();
                            Instruction zZeroToOne = iterator.next();
                            Instruction invertY = iterator.next();
                            Instruction color = iterator.next();


                            LightComponent lightComponent = new LightComponent(
                                    this,
                                    new Matrix4f().lookAt(
                                            new Vector3f(
                                                    (float) eye.operands()[1],
                                                    (float) eye.operands()[2],
                                                    (float) eye.operands()[3]
                                            ),
                                            new Vector3f(
                                                    (float) center.operands()[1],
                                                    (float) center.operands()[2],
                                                    (float) center.operands()[3]
                                            ),
                                            new Vector3f(
                                                    (float) up.operands()[1],
                                                    (float) up.operands()[2],
                                                    (float) up.operands()[3]
                                            )
                                    ),
                                    new Matrix4f().perspective(
                                            (float) Math.toRadians((float) fovDeg.operands()[1]),
                                            ((float) aspectRatio.operands()[1]),
                                            ((float) zNear.operands()[1]),
                                            ((float) zFar.operands()[1]),
                                            (boolean) zZeroToOne.operands()[1]
                                    ),
                                    (boolean) invertY.operands()[1]
                            );
                            lightComponent.color = new Vector3f(
                                    (float) color.operands()[1],
                                    (float) color.operands()[2],
                                    (float) color.operands()[3]
                            );
                            actor.add(lightComponent);
                        }
                        case "ScriptComponent" -> {
                            Instruction script = iterator.next();
                            Logger.info(Scene.class, "Loading script " + script.operands()[1]);

                            try {
                                Class clazz = ProjectLoader.getClassLoader().loadClass((String) script.operands()[1]);
                                Constructor constructor = clazz.getConstructor();
                                Script s = (Script) constructor.newInstance();
                                actor.add(new ScriptComponent(s));
                            } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                                     InstantiationException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    }
                }

                case PopActor -> {
                    actor = actor.getParent();
                }
            }
        }


    }

    public Actor newRootActor(String name) {
        return new Actor(name);
    }



    public void addSystem(ActorSystem... actorSystems) {
        this.systems.addAll(Arrays.asList(actorSystems));
    }


    public void removeSystem(ActorSystem actorSystem) {
        systems.remove(actorSystem);
    }


    public void close() {
        for(ActorSystem system : systems) {
            system.dispose();
        }
    }


    @Override
    public void dispose() {

    }
}
