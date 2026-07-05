float pcf(Texture2D input_shadow_map, SamplerState input_shadow_map_sampler, int pcf_kernel_w, float2 shadow_map_uv_ts, float4 fragment_shadow_map_ps, Light light) {

    float shadow_map_texel_w, shadow_map_texel_h, shadow_map_num_levels;
    input_shadow_map.GetDimensions(0, shadow_map_texel_w, shadow_map_texel_h, shadow_map_num_levels);
    shadow_map_texel_w = 1 / shadow_map_texel_w;
    shadow_map_texel_h = 1 / shadow_map_texel_h;

    float shadow_samples = 0;

    for (int x = -pcf_kernel_w; x <= pcf_kernel_w; x++) {
        for (int y = -pcf_kernel_w; y <= -pcf_kernel_w; y++) {
            float shadow_map_depth = input_shadow_map.Sample(input_shadow_map_sampler, shadow_map_uv_ts.xy + float2(x, y) * float2(shadow_map_texel_w, shadow_map_texel_h)).r;

            shadow_samples += (shadow_map_depth < fragment_shadow_map_ps.z - light.shadowTestOffsetBias) ? 1.0 : 0.0;
        }
    }

    shadow_samples /= pow(2 * pcf_kernel_w + 1, 2);

    return shadow_samples;
}