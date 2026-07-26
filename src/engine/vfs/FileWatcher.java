package engine.vfs;

import engine.logging.Logger;
import engine.logging.SkyRuntimeException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Queue;

public class FileWatcher {
    private Thread thread;
    private final Queue<FileEvent> queue = new java.util.LinkedList<>();
    private WatchService watcher;


    public FileWatcher(Path dir, int delay) {

        thread = new Thread(() -> {

            HashMap<WatchKey, Path> map = new HashMap<>();

            try {
                watcher = FileSystems.getDefault().newWatchService();

                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path subdir, BasicFileAttributes attrs) throws IOException {
                        WatchKey key = subdir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                        map.put(key, subdir);
                        return FileVisitResult.CONTINUE;
                    }
                });


                while (true) {
                    if(Thread.interrupted()) break;
                    WatchKey key = watcher.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        Path full = map.get(key).resolve(filename);

                        Logger.info(FileWatcher.class, "Waiting " + delay + "ms for " + full);
                        Thread.sleep(delay);

                        if(!Files.isDirectory(full)) {
                            synchronized (queue) {
                                queue.add(new FileEvent(full, kind));
                            }
                        }
                    }

                    if (!key.reset()) break;

                }
            }
            catch (InterruptedException _) {}
            catch (IOException e) {
                throw new SkyRuntimeException(e);
            }

        });
        thread.setName("FileSystem Thread");
    }

    public Queue<FileEvent> getQueueNonSync() {
        return queue;
    }

    public Thread getThread() {
        return thread;
    }

    public void start() {
        thread.start();
    }


    public void stop() {
        thread.interrupt();

    }
}
