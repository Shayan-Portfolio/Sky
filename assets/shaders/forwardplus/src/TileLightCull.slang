#include "../../common/SceneDesc.slang"
#include "TileLightData.slang"

RWStructuredBuffer<float4> tiled_lighting_data;
StructuredBuffer<Scene> scene_desc;

struct Plane {
    float3 N;
    float d;
}

struct Frustum {
    Plane planes[5];
}

struct Cone {
    float3 tip;
    float height;
    float3 dir;
    float radius;

}

float2 screen_to_ndc(int2 xy, int2 wh) {
    float2 ndc = float2(xy) / float2(wh);

    ndc.x = ndc.x * 2.0 - 1.0;
    ndc.y = -(ndc.y * 2.0 - 1.0);

    return ndc;
}

bool is_point_in_halfspace(Plane plane, float3 p) {
    return dot(plane.N, p) - plane.d > 0;
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
    plane.N = normalize(cross(p - v2, p - v1));
    plane.d = -dot(plane.N, p);
    return plane;
}


static float4[] colors = {float4(1, 0, 0, 0), float4(0, 1, 0, 0), float4(0, 0, 1, 0)};

[numthreads(1, 1, 1)]
void computeMain(int3 dispatch_ID: SV_DispatchThreadID, int3 group_id: SV_GroupID, uniform int tiles_w, uniform int tiles_h, uniform int tile_size, uniform int w, uniform int h) {

    int index = group_id.y * tiles_w + group_id.x;
    //tiled_lighting_data[index] = colors[(index + group_id.y) % 3];

    

    Camera camera = scene_desc[0].camera;

    int2 tile_0 = int2(tile_size) * int2(group_id.xy);
    int2 tile_1 = int2(tile_size) * int2(group_id.x + 1, group_id.y);
    int2 tile_2 = int2(tile_size) * int2(group_id.x, group_id.y + 1);
    int2 tile_3 = int2(tile_size) * int2(group_id.x + 1, group_id.y + 1);

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

    //Front
    float3 I4 = ndc_to_view(screen_to_ndc(tile_2, wh), camera.invProj, 1).xyz;
    float3 J4 = ndc_to_view(screen_to_ndc(tile_3, wh), camera.invProj, 1).xyz;
    float3 K4 = ndc_to_view(screen_to_ndc(tile_1, wh), camera.invProj, 1).xyz;


    

    frustum.planes[0] = set_plane(I0, J0, v1);
    frustum.planes[1] = set_plane(I1, J1, v1);
    frustum.planes[2] = set_plane(J2, I2, v1);
    frustum.planes[3] = set_plane(J3, I3, v1);
    frustum.planes[4] = set_plane(I4, J4, K4);

    //tiled_lighting_data[index] = float4(frustum.planes[4].N, 0.0);

    Light light = scene_desc[0].lights[0];
    float4 light_pos_vs = mul(float4(light.invView[0][3], light.invView[1][3], light.invView[2][3], 1), camera.view);

    float4 light_dir_vs = mul(camera.view, mul(float4(0, 0, 1, 0), light.invView));

    float dist = 1;
    float theta = radians(45);
    Cone cone = {
        light_pos_vs.xyz,
        dist,
        normalize(light_dir_vs.xyz),
        dist * sin(theta)
    };

    tiled_lighting_data[index] = float4(
        0,
        is_point_in_halfspace(frustum.planes[2], cone.tip) ? 0 : 1,
        0,
        1
    );






}