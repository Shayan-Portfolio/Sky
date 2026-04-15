#pragma once
#include "CookTorrance.slang"
#include "SceneDesc.slang"

module common;

public struct CookTorranceEval {
    float3 get_color(Light light, float3 light_pos_ws, float3 light_dir_ws, float3 camera_pos_ws, float4 fragment_ws, float3 normal_ws, float metallic, float roughness, float3 albedo) {
        float distance = length(light_pos_ws - fragment_ws.xyz);
        float attenuation = 1.0 / (light.attenuationConstant + light.attenuationLinear * distance + light.attenuationQuadratic * (distance * distance));
        float3 radiance = light.color * attenuation;
        float cos = max(dot(light_dir_ws, normal_ws), 0.0);

        float3 camera_dir_ws = normalize(camera_pos_ws - fragment_ws.xyz);
        float3 halfway_dir_ws = normalize(light_dir_ws + camera_dir_ws);

        float3 F0 = float3(0.04);
        F0 = lerp(F0, albedo, metallic);
        float3 F = CT::fresnel(
                        max(dot(camera_dir_ws, halfway_dir_ws), 0),
                        F0
                    );



        float NDF = CT::distribution(
                        normal_ws, 
                        halfway_dir_ws, 
                        roughness
                    );

        float G = CT::geometry(
                        normal_ws, 
                        camera_dir_ws, 
                        light_dir_ws, 
                        roughness
                    );

        // BRDF specular numerator
        float3 numerator = NDF * G * F;
        // BRDF specular denominator
        float3 denominator = 4.0 * max(dot(normal_ws, camera_dir_ws), 0.0) * max(dot(normal_ws, light_dir_ws), 0.0) + 0.0001;

        float3 specular = numerator / denominator;

        float3 kS = F;
        float3 kD = float3(1.0) - kS;

        kD *= 1.0 - metallic;

        return float3((kD * albedo / PI + specular) * radiance * cos + (0.03 * albedo));
    }
}