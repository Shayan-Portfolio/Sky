package engine.gltf2;

import engine.asset.Asset;

public record Source(Asset<String> gltf, Asset<byte[]>... bin) {
}
