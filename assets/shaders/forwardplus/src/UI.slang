#include "../../common/Constants.slang"
struct VSOutput {
    float4 pos : SV_POSITION;
    float4 color;
    float2 uv;
    float shape_mode;
    float op0;
    float op1;
    float op2;
}

struct Vertex
{
    float2 pos : POSITION;
    float4 color : COLOR;
    float2 uv : TEXCOORD0;
    float shape_mode;
    float op0;
    float op1;
    float op2;
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
    output.op2 = vertex.op2;


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

//https://iquilezles.org/articles/distfunctions2d/
float sdRoundBox(float2 p, float2 b, float4 r )
{
    r.xy = (p.x>0.0)?r.xy : r.zw;
    r.x  = (p.y>0.0)?r.x  : r.y;
    float2 q = abs(p)-b+r.x;
    return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;
}

#define RECT -1
#define CIRCLE -2
#define MSDF_TEXT -3
#define ROUNDED_RECT -4




[shader("fragment")]
FSOutput fragmentMain(VSOutput vsOutput, float2 pixel_pos : SV_Position) {


    FSOutput output;
    if(vsOutput.shape_mode >= 0) {
        Texture2D texture = input_textures[int(vsOutput.op0)];
        SamplerState sampler = input_samplers[int(vsOutput.op0)];
        output.color0SW = texture.Sample(sampler, vsOutput.uv) * vsOutput.color;
    }
    else if (vsOutput.shape_mode == RECT) output.color0SW = vsOutput.color;
    else if (vsOutput.shape_mode == CIRCLE) {
        float2 uv = vsOutput.uv;
        float distance = length(uv);

        float angle = atan2(uv.y, uv.x);
        if(angle < 0) angle += 2 * PI;

        if (angle < vsOutput.op1 && distance <= 1.0 && distance >= (1.0 - vsOutput.op0))
            output.color0SW = vsOutput.color;
        else discard;
    }
    else if (vsOutput.shape_mode == MSDF_TEXT) {
        float3 msd = input_textures[int(vsOutput.op0)] .Sample(input_samplers[int(vsOutput.op0)], vsOutput.uv) .xyz;
        float sd = median(msd.r, msd.g, msd.b);
        float screenPxDistance = vsOutput.op1 * (sd - 0.5);
        float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

        output.color0SW = lerp(0.0, vsOutput.color, opacity);
    }
    else if (vsOutput.shape_mode == ROUNDED_RECT) {
        float2 wh = float2(vsOutput.op0, vsOutput.op1);
        float r = vsOutput.op2;

        float2 coord = (vsOutput.uv - 0.5) * wh;
        float2 half = wh / 2;
        float distance = sdRoundBox(coord, half, float4(r, r, r, r));

        if(distance < 0) output.color0SW = vsOutput.color;


    }



    return output;
}