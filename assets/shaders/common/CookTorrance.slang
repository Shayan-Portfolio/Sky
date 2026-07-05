#pragma once
#include "Constants.slang"
namespace CT {

float3 fresnel(float cosTheta, float3 F0) {
    return schlick(cosTheta, F0);
}

float3 schlick(float cosTheta, float3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

float distribution(float3 N, float3 H, float roughness) {
    return trowbridgeReitz(N, H, roughness);
}

float trowbridgeReitz(float3 N, float3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return num / denom;
}

float geometry(float3 N, float3 V, float3 L, float roughness) {
    return smithGeometry(N, V, L, roughness);
}

float schlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;

    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return num / denom;
}
float smithGeometry(float3 N, float3 V, float3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = schlickGGX(NdotV, roughness);
    float ggx1 = schlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

}