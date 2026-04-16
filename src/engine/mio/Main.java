package engine.mio;

public class Main {
    public static void main(String[] args) {

        long start = System.currentTimeMillis();
        SceneCompiler.printTokens("actor \"Fuselage\"\n" +
                "    \"ShaderComponent\" data(\n" +
                "        \"vertexShader\" string(\"core:assets/shaders/deferred/Default2_vertex.spv\"),\n" +
                "        \"fragmentShader\" string(\"core:assets/shaders/deferred/Default2_fragment.spv\")\n" +
                "    )\n" +
                "    \"MeshComponent\" data(\n" +
                "        \"type string(\"cylinder\"),\n" +
                "        \"params\" array[0.7, -.0 , 20],\n" +
                "        \"maxVertexCount\" float1(500),\n" +
                "        \"maxIndexCount\" float1(500),\n" +
                "    )\n" +
                "    \"TransformComponent\" data(\n" +
                "        \"translate\" float3(-2.0, 6.0, 2.0),\n" +
                "        \"rotateAxis\" euler3(0.0, 0.0, 1.0),\n" +
                "        \"rotateDeg\" float1(45.0)\n" +
                "    )\n" +
                "    \"MaterialComponent\" data(\n" +
                "        \"baseColor\" string(\"nsp:nspassets/textures/fuselage.png\"),\n" +
                "        \"normal\" string(\"nsp:nspassets/textures/polligon_metal/normal.png\"),\n" +
                "        \"metallic\" string(\"nsp:nspassets/textures/polligon_metal/metallic.jpg\"),\n" +
                "        \"roughness\" string(\"nsp:nspassets/textures/polligon_metal/roughness.jpg\"),\n" +
                "    )\n" +
                "\n" +
                "\n" +
                "\n" +
                "    \"RigidBodyComponent\" data(\n" +
                "        \"type\" string(\"cylinder\"),\n" +
                "        \"params\" array[0.7, 6],\n" +
                "        \"mass\" float1(1.0),\n" +
                "        \"interfaceFriction\" float1(0.8),\n" +
                "        \"interfaceRestitution\" float1(0.0),\n" +
                "        \"canRotate\" bool(true)\n" +
                "    )\n" +
                "end\n" +
                "\n");

        System.out.println("[" + (System.currentTimeMillis() - start) + " ms]");
       //for(Instruction frame : ir.getList()) {
       //    System.out.println(frame);
       //}





        
    }
}