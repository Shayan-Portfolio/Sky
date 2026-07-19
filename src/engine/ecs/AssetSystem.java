package engine.ecs;

import engine.asset.Asset;
import engine.asset.AssetListener;
import engine.asset.AssetPackage;
import engine.asset.AssetRegistry;
import engine.logging.Logger;
import engine.vfs.FileWatcher;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Queue;

public class AssetSystem extends ActorSystem {
    private FileWatcher fileWatcher;
    public AssetSystem() {
        fileWatcher = new FileWatcher(Path.of("."));
        fileWatcher.start();
    }

    @Override
    public void run(Actor root) {
        Queue<Path> queue = fileWatcher.getQueue();
        synchronized (queue) {

            for (Iterator<Path> iterator = queue.iterator(); iterator.hasNext(); ) {
                Path path = iterator.next();
                iterator.remove();

                String key = new StringBuilder()
                        .append(path.toString().replace("\\", "/"))
                        .deleteCharAt(0)
                        .deleteCharAt(0)
                        .toString();

                Logger.info(AssetSystem.class, "Hot-Reloading " + key);

                for (AssetPackage assetPackage : AssetRegistry.getPackageRegistry().values()) {
                    Asset<?> asset = assetPackage.getAssetMap().get(key);
                    if(asset != null) {

                        Object res = AssetPackage.loadRes(asset.getPath(), Path.of(asset.getPath()), assetPackage.getNamespace());
                        asset.setObjectUnsafe(res);

                        asset.fireAssetListeners();
                    }
                }


            }
        }
    }

    @Override
    public void dispose() {

    }
}
