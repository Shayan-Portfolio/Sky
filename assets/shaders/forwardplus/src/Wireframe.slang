#include "../../common/SceneDesc.slang"

struct VSOutput {
    float4 pos : SV_POSITION;
}

struct Vertex
{
    float3 pos;
}
StructuredBuffer<Scene> scene_desc;


[shader("vertex")]
VSOutput vertexMain(Vertex vertex) {
    VSOutput output;
    float4x4 view = scene_desc[0].camera.view;
    float4x4 proj = scene_desc[0].camera.proj;

    output.pos = mul(proj, mul(view, float4(vertex.pos, 1.0)));


    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
}


[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {
    FSOutput output;
    output.color0 = float4(1, 0, 0, 0);
    return output;
}