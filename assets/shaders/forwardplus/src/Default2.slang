#include "../../common/CookTorranceEval.slang"
#include "../../common/SceneDesc.slang"
#include "../../common/NormalDepthBiasEval.slang"
#include "../../common/ShadowEval.slang"
#include "../../common/ReinhardTonemappingEval.slang"



struct Transforms {
    float4x4 transform[1];
}

StructuredBuffer<Scene> scene_desc;
StructuredBuffer<Transforms> transforms;


Texture2D input_shadow_maps[MAX_LIGHTS];
SamplerState input_shadow_maps_samplers[MAX_LIGHTS];

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
VSOutput vertexMain(Vertex vertex, uniform int render_mode, uniform int light_index, uniform int light_count) {
    VSOutput output;
    float4x4 view, proj;


    
    if(render_mode == 0 || render_mode == 2) {
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
}

Texture2D material[4];
SamplerState material_sampler;

[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, uniform int render_mode, uniform int light_index, uniform int light_count) {
    FSOutput output;
    float3 output_color = float3(0, 0, 0);

    if (render_mode == 0) {

        float3 normal_ts = material[1].Sample(material_sampler, vsOutput.uv_ts).xyz * 2.0 - 1.0;
        float3 normal_ws = normalize(mul(vsOutput.tbn_ws_inv, normalize(normal_ts)));

        float3 albedo = material[0].Sample(material_sampler, vsOutput.uv_ts).rgb;

        float3 color = vsOutput.color.xyz * albedo;

        float metallic = material[2].Sample(material_sampler, vsOutput.uv_ts).r;
        float roughness = material[3].Sample(material_sampler, vsOutput.uv_ts).r;
        

        
        //BRDF
        {
           
             Camera camera = scene_desc[0].camera;
             float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);


             float4 fragment_ws = mul(camera.invView, float4(vsOutput.pos_vs.xyz, 1.0));

             float shadow = 0;

             for (int light_index = 0; light_index < light_count; light_index++) {

                 Light light = scene_desc[0].lights[light_index];

                 float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
                 float3 light_dir_ws = normalize(light_pos_ws - fragment_ws.xyz);
                 float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);

                 NormalDepthBiasEval normalDepthBiasEval;
                 float4 normal_biased_fragment_ws = normalDepthBiasEval.get_normal_biased_fragment_ws(light, fragment_ws, light_dir_ws, normal_ws);

                 float4 fragment_shadow_map_ps = mul(light.proj, mul(light.view, normal_biased_fragment_ws));
                 fragment_shadow_map_ps /= fragment_shadow_map_ps.w;
                 float4 shadow_map_uv_ts = fragment_shadow_map_ps * 0.5 + 0.5;

                 bool in_frustum = shadow_map_uv_ts.x >= 0 && shadow_map_uv_ts.x <= 1 && shadow_map_uv_ts.y >= 0 && shadow_map_uv_ts.y <= 1;
                 /*if (in_frustum) {

                     // Shadows
                     {
                         float shadow_per_pixel = pcf(
                             input_shadow_maps[light_index],
                             input_shadow_maps_samplers[light_index],
                             1,
                             shadow_map_uv_ts.xy,
                             fragment_shadow_map_ps,
                             light
                         );

                         shadow += shadow_per_pixel;
                     }

                 }*/

                 // Lighting BRDF
                 {


                     CookTorranceEval lighting_eval;
                     float3 brdf = lighting_eval.get_color(
                         light,
                         light_pos_ws,
                         light_dir_ws,
                         camera_pos_ws,
                         fragment_ws,
                         normal_ws,
                         metallic,
                         roughness,
                         albedo
                     );

                     float distance = length(light_pos_ws - fragment_ws.xyz);
                     float sigmoid = 0;
                     {
                         float n = 0.2;
                         float k = 0.7;

                         sigmoid = (-n / (1 + exp(-distance * 0.1))) + 1;
                     }

                     brdf *= sigmoid;

                     //float3 env = input_skybox_texture.SampleLevel(input_skybox_texture_sampler, -reflect(camera_dir_ws, normal_ws), 3).rgb * cubemap_sample_intensity;

                     output_color.rgb += brdf;// + env;

                 }
             }

             output_color.rgb *= (1 - max(0.0, shadow));
             output.color0 = float4(reinhard_hdr(output_color), 1.0);
        }

    }




    return output;
}