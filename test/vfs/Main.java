package vfs;


import engine.logging.Logger;
import engine.vfs.FileEvent;
import engine.vfs.FileWatcher;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Queue;

public class Main {
    public static void main(String[] args) {
        Logger.setConsoleTarget(System.out);
        FileWatcher fileWatcher = new FileWatcher(Path.of("."), 250);
        fileWatcher.start();

        while(true) {
            Queue<FileEvent> queue = fileWatcher.getQueueNonSync();
            synchronized (queue) {

                for (Iterator<FileEvent> iterator = queue.iterator(); iterator.hasNext(); ) {
                    FileEvent fileEvent = iterator.next();
                    Path path = fileEvent.path();
                    if(fileEvent.kind() == java.nio.file.StandardWatchEventKinds.ENTRY_CREATE || fileEvent.kind() == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                        if (path.toString().endsWith("png")) {
                            try (MemoryStack stack = MemoryStack.stackPush()) {
                                IntBuffer w = stack.callocInt(1);
                                IntBuffer h = stack.callocInt(1);
                                IntBuffer channelsInFile = stack.callocInt(1);
                                Thread.sleep(500);
                                ByteBuffer texture = STBImage.stbi_load(
                                        path.toString(),
                                        w,
                                        h,
                                        channelsInFile,
                                        4
                                );
                                System.out.println(STBImage.stbi_failure_reason() + " " + fileEvent.kind());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }


                        System.out.println(path);
                    }
                    iterator.remove();
                }

            }


        }


    }
}
