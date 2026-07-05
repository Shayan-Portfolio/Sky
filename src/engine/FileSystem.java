package engine;

import engine.logging.SkyRuntimeException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileSystem {
    public static String readString(Path path){
        StringBuilder total = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile()));

            String line = "";
            while((line = bufferedReader.readLine()) != null){
                total.append(line).append("\n");
            }

            return total.toString();
        }
        catch (IOException e) {
            throw new SkyRuntimeException(e);
        }
    }

    public static byte[] readBytes(Path path){
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new SkyRuntimeException(e);
        }
    }
}
