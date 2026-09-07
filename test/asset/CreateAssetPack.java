package asset;

import engine.asset.AssetPackage;
import engine.logging.Logger;

import java.nio.file.Path;

public class CreateAssetPack {
    public static void main(String[] args) {
        Logger.setConsoleTarget(System.out);
        Path assetPackPathCore = Path.of("assets.pkg");
        AssetPackage.createPackage(assetPackPathCore, AssetPackage.openLocal("core", Path.of("assets")));
    }
}
