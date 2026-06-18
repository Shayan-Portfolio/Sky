#include "../../common/SceneDesc.slang"
#include "TileLighting.slang"

RWStructuredBuffer<float4> tiled_lighting_data;

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

    tiled_lighting_data[0] = float4(1.0, 1.0, 0.0, 0.0);

}