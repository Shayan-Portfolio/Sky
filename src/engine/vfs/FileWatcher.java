package engine.vfs;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Queue;

public class FileWatcher {
    private Thread thread;
    private final Queue<Path> queue = new java.util.LinkedList<>();


    public FileWatcher(Path dir) {

        thread = new Thread(() -> {

            HashMap<WatchKey, Path> map = new HashMap<>();

            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();

                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path subdir, BasicFileAttributes attrs) throws IOException {
                        WatchKey key = subdir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                        map.put(key, subdir);
                        return FileVisitResult.CONTINUE;
                    }
                });


                while (true) {
                    WatchKey key = watcher.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        Path full = map.get(key).resolve(filename);

                        if(!Files.isDirectory(full)) {
                            synchronized (queue) {
                                queue.add(full);
                            }
                        }
                    }

                    if (!key.reset()) break;

                }
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        });
        thread.setName("FileSystem Thread");
    }

    public Queue<Path> getQueue() {
        return queue;
    }

    public Thread getThread() {
        return thread;
    }

    public void start() {
        thread.start();
    }


}
