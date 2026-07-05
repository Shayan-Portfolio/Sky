package engine.asset;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;

import java.util.HashMap;

public class AssetRegistry {
    private static HashMap<String, AssetPackage> assetPackageRegistry = new HashMap<>();
    private AssetRegistry(){}


    public static <T> Asset<T> getAsset(String identifier) {
        Logger.info(AssetRegistry.class, "Loading asset " + identifier);


        String[] tokens = identifier.split(":");

        String assetPackNamespace = tokens[0];
        String assetPath = tokens[1];

        AssetPackage assetPackage = assetPackageRegistry.get(assetPackNamespace);

        String error = "";

        if(assetPackage == null){
            error = "Failed to locate asset package for namespace " + assetPackNamespace;
            throw new SkyRuntimeException(error);
        }

        Asset<T> asset = assetPackage.getAsset(assetPath);
        if(asset == null){
            error = "Failed to locate asset " + assetPath + " in package namespace " + assetPackNamespace;
            throw new SkyRuntimeException(error);

        }

        return asset;
    }

    public static void addPackage(AssetPackage assetPackage){
        assetPackageRegistry.put(assetPackage.getNamespace(), assetPackage);
    }

}
