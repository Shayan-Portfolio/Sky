#include "../../common/CookTorranceEval.slang"
#include "KnarkowiczOctahedral.slang"
#include "../../common/SceneDesc.slang"
#include "../../common/NormalDepthBiasEval.slang"
#include "../../common/ShadowEval.slang"
#include "../../common/BloomEval.slang"
#include "../../common/ReinhardTonemappingEval.slang"


StructuredBuffer<Scene> scene_desc;

Texture2D input_texture0;
RWTexture2D output_color_texture;
Texture2D input_texture1;
Texture2D input_depth_stencil_psw_texture;

Texture2D input_shadow_maps[MAX_LIGHTS];
SamplerState input_shadow_maps_samplers[MAX_LIGHTS];

TextureCube input_skybox_texture;
SamplerState input_skybox_texture_sampler;

RWTexture2D output_bloom_texture;




float4 get_pos_ws(float ux, float uy, float4x4 inv_proj, float4x4 inv_view, float depth_psw) {

    float ndc_x = ux * 2.0 - 1.0;
    float ndc_y = -(uy * 2.0 - 1.0);
    float4 projected_pos = float4(ndc_x, ndc_y, depth_psw, 1.0);

    float4 view = mul(inv_proj, projected_pos);
    float4 pos = mul(inv_view, view);

    pos /= pos.w;

    return pos;
}

[numthreads(32, 32, 1)]
void computeMain(uniform int light_count, uniform int width, uniform int height, uniform float cubemap_sample_intensity, int3 dispatch_ID : SV_DispatchThreadID) {
    int3 texture_xy_ss = int3(dispatch_ID.x, dispatch_ID.y, 0);

    float4 input_color = input_texture0.Load(texture_xy_ss, 0);

    float depth_psw = input_depth_stencil_psw_texture.Load(texture_xy_ss, 0).r;
    float3 output_color = float3(0.0, 0.0, 0.0);

    if (depth_psw != 1) {
        output_color = float3(0.0, 0.0, 0.0);

        Camera camera = scene_desc[0].camera;
        float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

        float4 color1 = input_texture1.Load(texture_xy_ss, 0);
        float metallic = color1.g;
        float roughness = color1.b;
        float3 albedo = input_color.rgb;

        float4 fragment_ws = get_pos_ws((float) (texture_xy_ss.x + 0.5) / width, (float) (texture_xy_ss.y + 0.5) / height, camera.invProj, camera.invView, depth_psw); //mul(camera.invView, float4(fragment_vs.xyz, 1.0));

        float shadow = 0;

        for (int light_index = 0; light_index < light_count; light_index++) {

            Light light = scene_desc[0].lights[light_index];

            float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
            float3 light_dir_ws = normalize(light_pos_ws - fragment_ws.xyz);
            float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);

            float3 normal_ws = normalize(decode(float2(input_color.w, color1.w)));

            NormalDepthBiasEval normalDepthBiasEval;
            float4 normal_biased_fragment_ws = normalDepthBiasEval.get_normal_biased_fragment_ws(light, fragment_ws, light_dir_ws, normal_ws);

            float4 fragment_shadow_map_ps = mul(light.proj, mul(light.view, normal_biased_fragment_ws));
            fragment_shadow_map_ps /= fragment_shadow_map_ps.w;
            float4 shadow_map_uv_ts = fragment_shadow_map_ps * 0.5 + 0.5;

            bool in_frustum = shadow_map_uv_ts.x >= 0 && shadow_map_uv_ts.x <= 1 && shadow_map_uv_ts.y >= 0 && shadow_map_uv_ts.y <= 1;
            if (in_frustum) {
            
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

            }

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

                float3 env = input_skybox_texture.SampleLevel(input_skybox_texture_sampler, -reflect(camera_dir_ws, normal_ws), 3).rgb * cubemap_sample_intensity;

                output_color.rgb += brdf + env;

            }
        }

        output_color.rgb *= (1 - max(0.0, shadow));






    }
    else {
        Camera camera = scene_desc[0].camera;
        float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

        float4 fragment_ws = get_pos_ws((float) (texture_xy_ss.x + 0.5) / width, (float) (texture_xy_ss.y + 0.5) / height, camera.invProj, camera.invView, 0.9);

        float3 dir = fragment_ws.xyz - camera_pos_ws;

        output_color.rgb = input_skybox_texture.Sample(input_skybox_texture_sampler, dir, 0).rgb;
    }

    BloomEval bloom_eval;

    bloom_eval.write_bloom(
        output_bloom_texture,
        texture_xy_ss,
        width,
        height,
        output_color
    );




    output_color_texture[texture_xy_ss.xy] = float4(reinhard_hdr(output_color), 1.0);
}