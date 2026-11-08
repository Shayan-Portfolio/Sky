#include "../../common/SceneDesc.slang"

struct VSOutput {
    float4 pos : SV_POSITION;
    nointerpolation float4 color;
}

StructuredBuffer<Scene> scene_desc;

static float3 skybox_color[] = {
    float3(1, 0, 0),
    float3(0, 1, 0),
    float3(0, 0, 1),
    float3(0, 1, 1),
    float3(1, 1, 1),
    float3(1, 1, 0)
};

static float skybox_vertices[] = {
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,

            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,

            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,

             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,

            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,

            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
};



[shader("vertex")]
VSOutput vertexMain(int vertexID : SV_VertexID) {
    VSOutput output;
    float4x4 view = scene_desc[0].camera.view;
    float4x4 proj = scene_desc[0].camera.proj;

    view[0][3] = 0;
    view[1][3] = 0;
    view[2][3] = 0;

    float3 vertex = {
        skybox_vertices[(vertexID * 3)],
        skybox_vertices[(vertexID * 3) + 1],
        skybox_vertices[(vertexID * 3) + 2]
    };

    output.pos = mul(proj, mul(view, float4(vertex.xyz, 1.0))).xyww;
    output.color = float4(skybox_color[vertexID / 6], 1);


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