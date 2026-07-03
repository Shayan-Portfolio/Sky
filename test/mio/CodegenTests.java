package mio;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import engine.logging.SkyRuntimeException;
import engine.mio.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;

public class CodegenTests {
    public static void main(String[] args) {
        Context source = new Context("test.scene", """
                actor MySpotlight\s
                    add LightComponent
                        fovDeg = 120
                        eye = vec3(0, 10, 5)
                        center = vec3(0, 5, 0)
                        up = vec3(0, 0, 1)
                        aspectRatio = 1.0
                        zNear = 0.1
                        zFar = 50.0
                        zZeroToOne = true
                        invertY = true
                        color = vec3(3, 3, 3)
                    end
                end
                
                
                actor TerrainChunk0
                
                    add ShaderComponent
                        vertexShader = "core:assets/shaders/forwardplus/Default2_vertex.spv"
                        fragmentShader = "core:assets/shaders/forwardplus/Default2_fragment.spv"
                    end
                
                    add TransformComponent
                        translate = vec3(0, 0, 0)
                        rotateAxis = vec3(0.0, 0.0, 1.0)
                        rotateDeg = 0.0
                    end
                
                    add MaterialComponent
                        baseColor = "nsp:nspassets/textures/grass/basecolor.png"
                        normal = "nsp:nspassets/textures/grass/normal.png"
                        metallic = "nsp:nspassets/textures/default.png"
                        roughness = "nsp:nspassets/textures/grass/roughness.png"
                    end
                
                    add ScriptComponent
                        script = "luna.scripts.TerrainChunkScript"
                    end
                
                
                
                
                end
                
                actor Plants
                
                    add ShaderComponent
                        vertexShader = "core:assets/shaders/forwardplus/Default2_vertex.spv"
                        fragmentShader = "core:assets/shaders/forwardplus/Default2_fragment.spv"
                    end
                
                    add TransformComponent
                        translate = vec3(0, 0, 0)
                        rotateAxis = vec3(0.0, 0.0, 1.0)
                        rotateDeg = 0.0
                    end
                    add MaterialComponent
                        baseColor = "nsp:nspassets/textures/misc/img.png"
                        normal = "nsp:nspassets/textures/flat_normals.png"
                        metallic = "nsp:nspassets/textures/p_concrete/metallic.jpg"
                        roughness = "nsp:nspassets/textures/default.png"
                    end
                
                    add ScriptComponent
                        script = "luna.scripts.PlantsScript"
                    end
                
                end
                        """, new DefaultLogStrategy());

        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            ASTNode node = parser.parseAll(tokenizer);
            Codegen codegen = new Codegen(source);
            BytecodeStream stream = codegen.flatten(node);

            {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);

                Output output;
                try {
                    output = new Output(new DeflaterOutputStream(Files.newOutputStream(Path.of("output.mi"))));
                }
                catch (IOException e) {
                    throw new SkyRuntimeException(e);
                }

                kryo.writeObject(output, stream);

                output.flush();
                output.close();
            }
        }
        catch(ParseException e) {
            e.printStackTrace();
        }
    }
}
