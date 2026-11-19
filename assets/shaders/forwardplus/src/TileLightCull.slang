#include "../../common/SceneDesc.slang"
#include "TileLightData.slang"

RWStructuredBuffer<float4> tiled_lighting_data;
StructuredBuffer<Scene> scene_desc;


float3 unproject(float2 ndc, float depth, float4x4 inv_view, float4x4 inv_proj) {
    float ndcX = ndc.x;
    float ndcY = ndc.y;
    float4 start = float4(ndcX, ndcY, depth, 1.0f);
    float4 view = mul(inv_proj, start);
    float4 w = mul(inv_view, view);

    w.div(w.w);

    return w.xyz;
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

    
  


    return success == 2;
}

static float4[] colors = {float4(0.1, 0, 0, 0), float4(0, 0.1, 0, 0), float4(0, 0, 0.1, 0)};

[numthreads(1, 1, 1)]
void computeMain(int3 dispatch_ID: SV_DispatchThreadID, int3 group_id: SV_GroupID, uniform int tiles_w, uniform int tiles_h, uniform int tile_size, uniform int w, uniform int h, uniform int light_count, uniform int radius) {

    int index = group_id.y * tiles_w + group_id.x;


    float nw = (2.0 / tiles_w), nh = (2.0 / tiles_h);
    float Fx = nw * group_id.x - 1;
    float Fy = nh * group_id.y - 1;

    float depth = 1;
    Camera camera = scene_desc[0].camera;
    float3 camera_pos_ws = float3(camera.invView[0][3], camera.invView[1][3], camera.invView[2][3]);

    tiled_lighting_data[index] = float4(0.0, 0.0, 0.0, 1.0);

    float3 tr = unproject(float2(Fx + nw, Fy + nh), depth, camera.invView, camera.invProj);
    float3 tl = unproject(float2(Fx, Fy + nh), depth, camera.invView, camera.invProj);
    float3 br = unproject(float2(Fx + nw, Fy), depth, camera.invView, camera.invProj);
    float3 bl = unproject(float2(Fx, Fy), depth, camera.invView, camera.invProj);
       



    for(int i = 0; i < light_count; i++) {
        Light light = scene_desc[0].lights[i];
        float3 light_pos_ws = float3(light.invView[0][3], light.invView[1][3], light.invView[2][3]);
    
        if(isSphereInFrustum(float3(0, 0, 0), radius, camera_pos_ws, {tr, tl, br, bl})) {
    
            tiled_lighting_data[index] = float4(1.0, 0.0, 0.0, 1.0);
        }
        



    }
  
    
}