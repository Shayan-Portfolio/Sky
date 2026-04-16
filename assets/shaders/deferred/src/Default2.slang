#include "KnarkowiczOctahedral.slang"
#include "../../common/SceneDesc.slang"

struct Transforms {
    float4x4 transform[1];
}

StructuredBuffer<Scene> scene_desc;
StructuredBuffer<Transforms> transforms;

struct VSOutput {
    float4 pos : SV_POSITION;
    float4 pos_vs;
    float4 color;
    float3 normal_ws;
    float2 uv_ts;
    float3x3 tbn_ws_inv;
}

struct Vertex
{
    float3 pos_ms;
    float4 color;
    float3 normal_ms;
    float2 uv_ts;
    float3 tangent_ms;
}



[shader("vertex")]
VSOutput vertexMain(Vertex vertex, uniform int render_mode, uniform int light_index) {
    VSOutput output;
    float4x4 view, proj;


    
    if(render_mode == 0) {
        view = scene_desc[0].camera.view;
        proj = scene_desc[0].camera.proj;
    }
    if(render_mode == 1) {
        view = scene_desc[0].lights[light_index].view;
        proj = scene_desc[0].lights[light_index].proj;
    }

    float4x4 transform = transforms[0].transform[0];

    output.normal_ws = normalize(mul(float3x3(transform), vertex.normal_ms));
    output.pos_vs = mul(view, mul(transform, float4(vertex.pos_ms, 1.0)));
    output.pos = mul(proj, output.pos_vs);
    output.color = vertex.color;
    output.uv_ts = vertex.uv_ts;

    float3 tangent_ws = normalize(mul(float3x3(transform), vertex.tangent_ms));
    float3 bitangent_ws = cross(output.normal_ws, tangent_ws);

    output.tbn_ws_inv = transpose(float3x3(tangent_ws, bitangent_ws, output.normal_ws));

    return output;
}

struct FSOutput {
    float4 color0 : SV_Target0;
    float4 color1 : SV_Target1;
}

Texture2D material[4];
SamplerState material_sampler;

[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, uniform int render_mode, uniform int light_index) {
    FSOutput output;

    if (render_mode == 0) {

        float3 normal_ts = material[1].Sample(material_sampler, vsOutput.uv_ts).xyz * 2.0 - 1.0;
        float3 normal_ws = normalize(mul(vsOutput.tbn_ws_inv, normalize(normal_ts)));

        float2 encoded_normal_ws = encode(normal_ws);

        float3 albedo = material[0].Sample(material_sampler, vsOutput.uv_ts).rgb;

        output.color0 = float4(vsOutput.color.xyz * albedo, encoded_normal_ws.x);

        float metallic = material[2].Sample(material_sampler, vsOutput.uv_ts).r;
        float roughness = material[3].Sample(material_sampler, vsOutput.uv_ts).r;

        output.color1 = float4(0.0, metallic, roughness, encoded_normal_ws.y);
    }




    return output;
}