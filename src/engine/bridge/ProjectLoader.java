package engine.bridge;

import engine.Application;
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

    public static Application instantiateApplication(String[] args) {
        if(args.length == 0) throw new SkyRuntimeException("No project JAR specified");
        if(args.length == 1) throw new SkyRuntimeException("No application class specified");


        Path path = Path.of(args[0]);
        try {
            URL url = path.toUri().toURL();
            Logger.info(ProjectLoader.class, "Loading project " + url);

            classLoader = new URLClassLoader(new URL[]{url});

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
