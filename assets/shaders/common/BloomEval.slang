module common;

struct BloomEval {
    void write_bloom(RWTexture2D output_bloom_texture, int3 texture_xy_ss, int width, int height, float3 output_color) {

        //Luminance extraction
        {
            float2 xy = float2(texture_xy_ss.xy) / float2(width, height);

            int w, h, num_levels;
            output_bloom_texture.GetDimensions(0, w, h, num_levels);

            int2 ixy = xy * float2(w, h);

            float luminance = dot(output_color, float3(0.2126, 0.7152, 0.0722));
            if(luminance >= 2.0) output_bloom_texture[ixy] = float4(output_color, 1.0);
            else output_bloom_texture[ixy] = float4(0.0, 0.0, 0.0, 0.0);

        }


    }


}