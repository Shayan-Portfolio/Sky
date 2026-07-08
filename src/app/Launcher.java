package app;

import engine.Time;
import engine.logging.Logger;
import engine.Application;
import engine.wsi.Surface;
import engine.bridge.ProjectLoader;
import engine.graphics.Session;
import engine.logging.SkyRuntimeException;
import org.lwjgl.system.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.List;


public class Launcher {


    public void initLogging() {
        //Logger.setFileTarget(new File("engine.log"));
        Logger.setConsoleTarget(System.out);
    }


    public void launch(String[] args) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, MalformedURLException {
        System.setProperty("org.lwjgl.system.stackSize", "128");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            throw new SkyRuntimeException(e);
        });

        Application application = ProjectLoader.instantiateApplication(args);
        Surface surface = Surface.newSurface(application, "SkyEngine", 1920, 1080);
        Session.setSurface(surface);


        application.launch(args, surface);



        while(true){
            boolean success = application.update();
            Time.deltaTime = (float) (surface.getTime() - application.getStartTime());
            application.setStartTime((float) surface.getTime());
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
