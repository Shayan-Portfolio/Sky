#include "../../common/SceneDesc.slang"
#include "TileLightData.slang"

RWStructuredBuffer<float4> tiled_lighting_data;
StructuredBuffer<Scene> scene_desc;


static float4[] colors = {float4(1, 0, 0, 1), float4(0, 1, 0, 1), float4(0, 0, 1, 1)};


float3 unproject(float2 ndc, float depth, float4x4 inv_view, float4x4 inv_proj) {
    ndc.y *= -1;
    float4 projected_pos = float4(ndc, depth, 1.0);

    float4 view = mul(inv_proj, projected_pos);
    float4 pos = mul(inv_view, view);

    pos /= pos.w;

    return pos.xyz;
}


bool isSphereInFrustum(float3 P, float radius, float3 frustumCenter, float3[] F) {
    //Ax + By + Cz - d = 0
    float3 sphereCenter = P - frustumCenter;

    float3[] frustum = {
        F[0] - frustumCenter,
        F[1] - frustumCenter,
        F[2] - frustumCenter,
        F[3] - frustumCenter,
    };


    int success = 0;



    //Left
    {

        float3 A = frustum[1];
        float3 B = frustum[3];
        float3 N = normalize(cross(A, B));
        float d = dot(frustum[1], N);
        float dist = (dot(N, sphereCenter) - d);
        if(dist > 0 || -dist < radius) success++;
    }

    //Right
    {

        float3 A = frustum[0];
        float3 B = frustum[2];
        float3 N = normalize(cross(B, A));
        float d = dot(frustum[0], N);
        float dist = (dot(N, sphereCenter) - d);
        if(dist > 0 || -dist < radius) success++;
    }

    //Top
    {

        float3 A = frustum[2];
        float3 B = frustum[3];
        float3 N = normalize(cross(B, A));
        float d = dot(frustum[2], N);
        float dist = (dot(N, sphereCenter) - d);
        if(dist > 0 || -dist < radius) success++;
    }

    //Bottom
    {

        float3 A = frustum[0];
        float3 B = frustum[1];
        float3 N = normalize(cross(A, B));
        float d = dot(frustum[0], N);
        float dist = (dot(N, sphereCenter) - d);
        if(dist > 0 || -dist < radius) success++;
    }



    return success == 4;
}



[numthreads(1, 1, 1)]
void computeMain(int3 dispatch_ID: SV_DispatchThreadID, int3 group_id: SV_GroupID, uniform int tiles_w, uniform int tiles_h, uniform int tile_size, uniform int w, uniform int h, uniform int light_count, uniform int radius) {

    int index = group_id.y * tiles_w + group_id.x;

    float2 size = float2(2.0 / tiles_w, 2.0 / tiles_h);
    float2 ndc = float2(group_id.x, group_id.y) * size - float2(1, 1);

    Camera camera = scene_desc[0].camera;
    float3 camera_position = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

    float depth = 0.1;

    float3 tr = unproject(ndc + size, depth, camera.invView, camera.invProj);
    float3 tl = unproject(ndc + float2(0, size.y), depth, camera.invView, camera.invProj);
    float3 br = unproject(ndc + float2(size.x, 0), depth, camera.invView, camera.invProj);
    float3 bl = unproject(ndc, depth, camera.invView, camera.invProj);

    tiled_lighting_data[index] = float4(0.1, 0.1, 1, 1);



    for (int light_index = 0; light_index < light_count; light_index++) {

        Light light = scene_desc[0].lights[light_index];
        float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);

        if(isSphereInFrustum(light_pos_ws, radius, camera_position, {tr, tl, br, bl})) {
            tiled_lighting_data[index] = float4(1.0 - (light_index / 10), 0.35, 0.0, 1.0);

        }
    }

    //tiled_lighting_data[index] = float4(Q.xyz, 1);


    
}