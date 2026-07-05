#include "../../common/Constants.slang"
struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color;
    float2 uv;
    float shape_mode;
    float op0;
    float op1;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
    float shape_mode;
    float op0;
    float op1;
}




[shader("vertex")]
VSOutput vertexMain(Vertex vertex, uniform float4x4 view, uniform float4x4 proj) {
    VSOutput output;
    output.pos = mul(proj, mul(view, float4(vertex.pos, 0, 1.0)));
    output.color = vertex.color;
    output.uv = vertex.uv;
    output.shape_mode = vertex.shape_mode;
    output.op0 = vertex.op0;
    output.op1 = vertex.op1;


    return output;
}

struct FSOutput {
    float4 color0SW : SV_Target0;
}

Texture2D input_textures[32];
SamplerState input_samplers[32];

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}





[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput) {


    FSOutput output;
    if(vsOutput.shape_mode >= 0) {
        Texture2D texture = input_textures[int(vsOutput.op0)];
        SamplerState sampler = input_samplers[int(vsOutput.op0)];
        output.color0SW = texture.Sample(sampler, vsOutput.uv) * vsOutput.color;
    }
    else if (vsOutput.shape_mode == -1) output.color0SW = vsOutput.color;
    else if (vsOutput.shape_mode == -2) {
        float2 uv = vsOutput.uv;
        float distance = length(uv);

        float angle = atan2(uv.y, uv.x);
        if(angle < 0) angle += 2 * PI;

        if (angle < vsOutput.op1 && distance <= 1.0 && distance >= (1.0 - vsOutput.op0))
            output.color0SW = vsOutput.color;
        else discard;
    }
    else if (vsOutput.shape_mode == -3) {
        float3 msd = input_textures[int(vsOutput.op0)] .Sample(input_samplers[int(vsOutput.op0)], vsOutput.uv) .xyz;
        float sd = median(msd.r, msd.g, msd.b);
        float screenPxDistance = vsOutput.op1 * (sd - 0.5);
        float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

        output.color0SW = lerp(0.0, vsOutput.color, opacity);
    }



    return output;
}