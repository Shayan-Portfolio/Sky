package app;

import engine.Time;
import engine.logging.Logger;
import engine.Application;
import engine.wsi.Surface;
import engine.bridge.ProjectLoader;
import engine.logging.SkyRuntimeException;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.List;


public class Launcher {


    public void initLogging() {
        //Logger.setFileTarget(new File("engine.log"));
        Logger.setConsoleTarget(System.out);
    }

    public void loadNatives() {
        File nativesDir = new File("natives");

        try {
            for (File file : nativesDir.listFiles()) {
                String path = file.getAbsolutePath();
                if(path.endsWith("jar")) {
                    String module = file.getName().replaceAll("\\.jar$", "");
                    String dll = file.getPath()
                            .replaceAll("\\.jar$", "")
                            .replace(File.separator, "/");

                    Logger.info(Launcher.class, "Loading native library " + module + " at " + dll);
                    Library.loadSystem(module, dll);
                }

            }
        }
        catch (UnsatisfiedLinkError e) {
            throw new SkyRuntimeException(e);
        }
    }


    public void launch(String[] args) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, MalformedURLException {
        System.setProperty("org.lwjgl.system.stackSize", "128");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            throw new SkyRuntimeException(e);
        });

        Application application = ProjectLoader.instantiateApplication(args);
        Application.set(application);
        Surface surface = Surface.newSurface(application, "SkyEngine", 1920, 1080);



        application.launch(args, surface);



        while(true){
            boolean success = application.update();
            Time.deltaTime = (float) (surface.getTime() - application.getStartTime());
            application.setStartTime((float) surface.getTime());
            //System.out.println(Time.framesPerSecond());
            if(!success) break;
        }


        application.close();
        surface.disposeAll();
    }

    private boolean isRunningInDebug() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();

        boolean isRunningInDebug = false;

        for(String arg : jvmArgs){
            if(arg.contains("jdwp=")) return true;
        }

        return isRunningInDebug;
    }

    public void enableDebugOptionsIfAttached() {
        if(isRunningInDebug()) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);

            Logger.info(Launcher.class, "A debugger is attached over JDWP. LWJGL memory allocations will ONLY appear on the console");
        }
    }

    public void logPlatformInfo() {
        Logger.info(Launcher.class,
                "JVM info: " + System.getProperty("java.vendor") + " " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") +
                        " [" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "]"
        );
    }
}
