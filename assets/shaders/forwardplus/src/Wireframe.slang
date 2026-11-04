#include "../../common/SceneDesc.slang"

struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color;
}

struct Vertex {
    float3 pos;
    float4 color;
}
StructuredBuffer<Scene> scene_desc;


[shader("vertex")]
VSOutput vertexMain(Vertex vertex) {
    VSOutput output;
    float4x4 view = scene_desc[0].camera.view;
    float4x4 proj = scene_desc[0].camera.proj;

    output.pos = mul(proj, mul(view, float4(vertex.pos, 1.0)));
    output.color = vertex.color;


    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
}


[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {
    FSOutput output;
    output.color0 = vsOutput.color;
    return output;
}