#pragma once
#define MAX_LIGHTS 10

public struct Camera {
    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;

    float zNear;
    float zFar;

    float fovY;
}

public struct Light {

    float4x4 view;
    float4x4 proj;

    float4x4 invView;
    float4x4 invProj;

    float attenuationConstant;
    float attenuationLinear;
    float attenuationQuadratic;
    float shadowTestOffsetBias;

    float3 color;

    float shadowNormalOffsetBias;

}

public struct Scene {
    Camera camera;
    Light lights[MAX_LIGHTS];
}