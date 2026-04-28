package mio;

import engine.mio.SceneCompiler;

class MioTest {
    public static void main(String[] args) {
        String output = SceneCompiler.printTokens("""
                actor "MySpotlight"
                    "LightComponent" data(
                        "fovDeg" float1(160),
                        "eye" float3(0, 10, 0.0),
                        "center" float3(0.0, 5.0, 0.0),
                        "up" float3(0.0, 0.0, 1.0),
                        "aspectRatio" float1(1.0),
                        "zNear" float1(0.1),
                        "zFar" float1(50.0),
                        "zZeroToOne" bool(true),
                        "invertY" bool(true),
                        "color" float3(3, 3, 3)
                    )
                end
                
                actor "Hangar"
                
                    #"ScriptComponent" data(
                    #    "script" string("nsp.game.scripts.ChunkGenerator")
                    #)
                    "ShaderComponent" data(
                        "vertexShader" string("core:assets/shaders/deferred/Default2_vertex.spv"),
                        "fragmentShader" string("core:assets/shaders/deferred/Default2_fragment.spv")
                    )
                
                    "TransformComponent" data(
                        "translate" float3(0, 0.0, 0.0),
                        "rotateAxis" euler3(0.0, 0.0, 1.0),
                        "rotateDeg" float1(0.0)
                    )
                    "MaterialComponent" data(
                        "baseColor" string("nsp:nspassets/textures/hangar_base.png"),
                        "normal" string("nsp:nspassets/textures/p_concrete/normal.png"),
                        "metallic" string("nsp:nspassets/textures/p_concrete/metallic.jpg"),
                        "roughness" string("nsp:nspassets/textures/p_concrete/roughness.jpg"),
                    )
                    #"MeshComponent" data(
                    #    "type" string("gltf"),
                    #    "params" array["nsp:nspassets/models/Hangar.gltf", "nsp:nspassets/models/Hangar.bin"],
                    #    "maxVertexCount" float1(100000),
                    #    "maxIndexCount" float1(100000),
                    #)
                
                    "MeshComponent" data(
                        "type" string("box"),
                        "params" array[30, 1, 30],
                        "maxVertexCount" float1(100),
                        "maxIndexCount" float1(100),
                    )
                
                
                
                
                end
                """);


    }
}