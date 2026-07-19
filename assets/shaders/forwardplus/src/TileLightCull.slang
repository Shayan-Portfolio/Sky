#include "../../common/SceneDesc.slang"
#include "TileLightData.slang"

RWStructuredBuffer<float4> tiled_lighting_data;
StructuredBuffer<Scene> scene_desc;

struct Plane {
    float3 N;
    float d;
}

struct Frustum {
    Plane planes[4];
    
}

float2 screen_to_ndc(int2 xy, int2 wh) {
    float2 ndc = xy / wh;

    ndc.x = ndc.x * 2.0 - 1.0;
    ndc.y = -(ndc.y * 2.0 - 1.0);

    return ndc;
}

float4 ndc_to_view(float2 ndc, float4x4 inv_proj, float ndc_depth) {

    //float ndc_x = ux * 2.0 - 1.0;
    //float ndc_y = -(uy * 2.0 - 1.0);
    float4 projected_pos = float4(ndc.xy, ndc_depth, 1.0);

    float4 view = mul(inv_proj, projected_pos);

    view /= view.w;

    return view;
}

Plane set_plane(float3 v1, float3 v2, float3 p) {
    Plane plane;
    plane.N = normalize(cross(p - v1, p - v2));
    plane.d = -dot(plane.N, p);
    return plane;
}

[numthreads(1, 1, 1)]
void computeMain(int3 dispatch_ID: SV_DispatchThreadID, int3 group_id: SV_GroupID, uniform int tiles_w, uniform int tiles_h, uniform int tile_size, uniform int w, uniform int h) {

    int index = group_id.y * tiles_w + group_id.x;
    tiled_lighting_data[index] = float4(0); //colors[(index + group_id.y) % 3];

    Camera camera = scene_desc[0].camera;

    int2 tile_0 = int2(tile_size) * int2(group_id.xy);
    int2 tile_1 = int2(tile_size) * int2(group_id.x + 1, group_id.y);
    int2 tile_2 = int2(tile_size) * int2(group_id.x + 1, group_id.y + 1);
    int2 tile_3 = int2(tile_size) * int2(group_id.x, group_id.y + 1);

    Frustum frustum;

    float3 v1 = float3(0, 0, 0);

    int2 wh = int2(w, h);

    
   
    //Up
    float3 I0 = ndc_to_view(screen_to_ndc(tile_0, wh), camera.invProj, 0).xyz;
    float3 J0 = ndc_to_view(screen_to_ndc(tile_1, wh), camera.invProj, 0).xyz;

    //Left
    float3 I1 = ndc_to_view(screen_to_ndc(tile_0, wh), camera.invProj, 0).xyz;
    float3 J1 = ndc_to_view(screen_to_ndc(tile_2, wh), camera.invProj, 0).xyz;

    //Right
    float3 I2 = ndc_to_view(screen_to_ndc(tile_1, wh), camera.invProj, 0).xyz;
    float3 J2 = ndc_to_view(screen_to_ndc(tile_3, wh), camera.invProj, 0).xyz;

    //Down
    float3 I3 = ndc_to_view(screen_to_ndc(tile_2, wh), camera.invProj, 0).xyz;
    float3 J3 = ndc_to_view(screen_to_ndc(tile_3, wh), camera.invProj, 0).xyz;



    frustum.planes[0] = set_plane(I0, J0, v1);
    frustum.planes[1] = set_plane(I1, J1, v1);
    frustum.planes[2] = set_plane(I2, J2, v1);
    frustum.planes[3] = set_plane(I3, J3, v1);

    








    


}