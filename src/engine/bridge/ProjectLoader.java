package engine.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import engine.Application;
import engine.vfs.FileSystem;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class ProjectLoader {
    private static ClassLoader classLoader;

    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    private static JsonArray parseClasspathJSON(Path classpathData) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(FileSystem.readString(classpathData), JsonObject.class);
        return jsonObject.getAsJsonArray("classpath");
    }

    public static Application instantiateApplication(String[] args) {
        boolean isRelease = args[0].endsWith("jar");
        Path gamepath = Path.of(args[0]);
        try {
            if(isRelease) {
                Logger.info(ProjectLoader.class, "Opening JAR " + gamepath);
                URL url = gamepath.toUri().toURL();
                classLoader = new URLClassLoader(new URL[]{url});
            }
            else {
                Logger.info(ProjectLoader.class, "Opening project " + gamepath);
                Path classpathData = Path.of(args[2]);
                JsonArray classpathJSON = parseClasspathJSON(classpathData);

                URL[] urls = new URL[1 + classpathJSON.size()];
                urls[0] = gamepath.toUri().toURL();
                for(int i = 0; i < classpathJSON.size(); i++) {
                    urls[i + 1] = Path.of(classpathJSON.get(i).getAsString()).toUri().toURL();
                }
                classLoader = new URLClassLoader(urls);
            }

            Class clazz = classLoader.loadClass(args[1]);
            Constructor constructor = clazz.getConstructor();
            Object appImpl = constructor.newInstance();
            return (Application) appImpl;
        } catch (MalformedURLException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                 InstantiationException | IllegalAccessException e) {
            throw new SkyRuntimeException(e);
        }


    }
}
