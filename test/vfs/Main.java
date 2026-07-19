package vfs;


import engine.vfs.FileWatcher;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Queue;

public class Main {
    public static void main(String[] args) {
        FileWatcher fileWatcher = new FileWatcher(Path.of("."));
        fileWatcher.start();

        while(true) {
            Queue<Path> queue = fileWatcher.getQueue();
            synchronized (queue) {

                for (Iterator<Path> iterator = queue.iterator(); iterator.hasNext(); ) {
                    Path path = iterator.next();
                    System.out.println(path);
                    iterator.remove();
                }

            }


        }


    }
}
