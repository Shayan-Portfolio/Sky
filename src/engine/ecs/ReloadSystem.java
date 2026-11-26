package engine.ecs;

import engine.Application;
import engine.asset.Asset;
import engine.asset.AssetPackage;
import engine.asset.AssetRegistry;
import engine.bridge.ProjectLoader;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import engine.vfs.FileEvent;
import engine.vfs.FileSystem;
import engine.vfs.FileWatcher;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;
import java.util.Queue;

public class ReloadSystem extends ActorSystem {
    private FileWatcher fileWatcher;
    public ReloadSystem() {
        fileWatcher = new FileWatcher(
                250,
                Path.of("."),
                ProjectLoader.getProjectPath()
        );
        fileWatcher.start();
    }

    @Override
    public void run(Actor root) {
        Queue<FileEvent> queue = fileWatcher.getQueueNonSync();
        synchronized (queue) {

            for (Iterator<FileEvent> iterator = queue.iterator(); iterator.hasNext(); ) {
                FileEvent fileEvent = iterator.next();
                Path path = fileEvent.path();
                iterator.remove();

                String key = new StringBuilder()
                        .append(path.toString().replace("\\", "/"))
                        .deleteCharAt(0)
                        .deleteCharAt(0)
                        .toString();

                boolean assetReloaded = false;

                for (AssetPackage assetPackage : AssetRegistry.getPackageRegistry().values()) {
                    Asset<?> asset = assetPackage.getAssetMap().get(key);
                    if(asset != null) {
                        Object res = AssetPackage.loadRes(asset.getPath(), Path.of(asset.getPath()), assetPackage.getNamespace());
                        asset.setObjectUnsafe(res);
                        asset.fireAssetListeners();

                        Logger.info(ReloadSystem.class, "Reloaded asset " + key + " from disk");
                        assetReloaded = true;
                    }
                }

                if(!assetReloaded) {

                    root.previsitAllActors(actor -> {
                        if(actor.has(ScriptComponent.class)) {
                            ScriptComponent s = actor.getComponent(ScriptComponent.class);
                            Script scr = s.script();


                            Path name = ProjectLoader
                                    .getProjectPath()
                                    .resolve(
                                            scr.getClass()
                                               .getCanonicalName()
                                               .replaceAll("\\.", "/")
                                            + ".class"
                                    );

                            if(path.equals(name)) {
                                if(!scr.detach(actor)) return;
                                scr.setActive(false);
                                actor.removeComponent(ScriptComponent.class);



                                try {
                                    ProjectLoader.updateClassLoader();
                                    Class clazz = ProjectLoader.getClassLoader().loadClass(scr.getClass().getCanonicalName());
                                    Constructor constructor = clazz.getConstructor();
                                    Script sn = (Script) constructor.newInstance();

                                    actor.addComponent(new ScriptComponent(sn));
                                } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                                         InstantiationException | IllegalAccessException e) {
                                    throw new SkyRuntimeException(e);
                                }

                            }

                        }
                    });
                }


            }
        }
    }

    @Override
    public void dispose() {
        fileWatcher.stop();

    }
}
