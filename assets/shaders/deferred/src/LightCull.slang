#include "../../common/SceneDesc.slang"
#include "ForwardPlusData.slang"
#define SLICE_COUNT 10

StructuredBuffer<Scene> scene_desc;
RWStructuredBuffer<ForwardPlusData> output;

float4 ndc_to_view_space(float ux, float uy, float4x4 inv_proj, float depth_ps) {

    float ndc_x = ux * 2.0 - 1.0;
    float ndc_y = -(uy * 2.0 - 1.0);
    float4 projected_pos = float4(ndc_x, ndc_y, depth_ps, 1.0);

    float4 view = mul(inv_proj, projected_pos);
    view /= view.w;

    return view;
}

float2 screen_to_ndc(float sx, float sy, int width, int height) {
    return float2(sx / width, sy / height) * 2.0 - 1.0;
}

#define TILE_SIZE 64

struct SubFrustumSphere {
    float3 pos_vs;
    float radius_vs;

}

// https://bartwronski.com/2017/04/13/cull-that-cone/
bool TestConeVsSphere(in float3 origin, in float3 forward, in float size, in float angle, in float4 testSphere)
{
    const float3 V = testSphere.xyz - origin;
    const float VlenSq = dot(V, V);
    const float V1len = dot(V, forward);
    const float distanceClosestPoint = cos(angle) * sqrt(VlenSq - V1len * V1len) - V1len * sin(angle);

    const bool angleCull = distanceClosestPoint > testSphere.w;
    const bool frontCull = V1len > testSphere.w + size;
    const bool backCull = V1len < -testSphere.w;
    return !(angleCull || frontCull || backCull);
}


[numthreads(1, 1, 1)]
void computeMain(int3 dispatch_ID: SV_DispatchThreadID, int3 group_id: SV_GroupID) {
    int width = 1920, height = 1080;
    Scene scene = scene_desc[0];

    Camera camera = scene.camera;
    Light light = scene.lights[group_id.x];

    int tiles_w = ceil(float(width) / TILE_SIZE);
    int tiles_h = ceil(float(height) / TILE_SIZE);

    float zNear = 0.1f;
    float zFar = 50.0f;

    for (int s = 0; s < SLICE_COUNT; s++) {
        float z_slice = -zNear * pow(zFar / zNear, float(s) / SLICE_COUNT);
        float4 ndc_slice = mul(camera.proj, float4(0, 0, z_slice, 1));
        ndc_slice /= ndc_slice.w;

        float ndc_z = ndc_slice.z;


        for (int y = 0; y < tiles_h; y++) {
            for (int x = 0; x < tiles_w; x++) {
                float2 tile_pos_ss = float2(x * TILE_SIZE, y * TILE_SIZE) + float2(TILE_SIZE / 2, TILE_SIZE / 2);
                float3 tile_pos_ndc = float3(screen_to_ndc(tile_pos_ss.x, tile_pos_ss.y, width, height), ndc_z);

                float4 tile_pos_vs = ndc_to_view_space(tile_pos_ndc.x, tile_pos_ndc.y, camera.invProj, tile_pos_ndc.z);

                SubFrustumSphere s = { tile_pos_vs.xyz, 5.0 };

                float4 light_forward_ws = float4(light.invView[2].xyz, 0);
                float4 light_forward_vs = normalize(mul(camera.view, light_forward_ws));

                float4 light_pos_ws = float4(light.invView[0][3], light.invView[1][3], light.invView[2][3], 1.0);
                float4 light_pos_vs = mul(camera.view, light_pos_ws);

                if (TestConeVsSphere(light_pos_vs.xyz, light_forward_vs.xyz, 10, radians(60), float4(s.pos_vs, s.radius_vs))) {
                    int index = ((y * tiles_w) + x);
                    output[0].tileLights[index][group_id.x] = group_id.x;
                }




                


            }
        }


    }






}