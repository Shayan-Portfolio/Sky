package app;


import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

public class Main {
    public static void main(String[] args) {

        Launcher launcher = new Launcher();
        launcher.initLogging();
        launcher.logPlatformInfo();
        launcher.loadNatives();
        launcher.enableDebugOptionsIfAttached();

        try {
            launcher.launch(args);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


    }
}
