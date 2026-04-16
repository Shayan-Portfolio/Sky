package engine.logging;

import engine.util.ExceptionUtil;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class SkyRuntimeException extends RuntimeException {
    public SkyRuntimeException(String msg, Exception e) {
        super(msg);
        Logger.meltdown(SkyRuntimeException.class, msg + "\n" + ExceptionUtil.exceptionToString(e));
        showDialogBox();
    }
    public SkyRuntimeException(Exception e) {
        super("A fatal error occurred and the engine exited");
        Logger.meltdown(SkyRuntimeException.class, "\n" + ExceptionUtil.exceptionToString(e));
        showDialogBox();
    }
    public SkyRuntimeException(String msg) {
        super(msg);
        Logger.meltdown(SkyRuntimeException.class, "\n" + ExceptionUtil.exceptionToString(this));
        showDialogBox();
    }

    private static void showDialogBox() {
        try {
            SwingUtilities.invokeAndWait((Runnable) () -> {

                String message = "An irrecoverable error occurred and the application will now exit.\n ";
                if(Logger.getOutputFile() == null) {
                    message += "The log can be found in the console";
                }
                else
                    message += "The log file is located at " + Logger.getOutputFile().getAbsolutePath();

                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                         UnsupportedLookAndFeelException e) {
                    throw new RuntimeException(e);
                }
                JOptionPane jOptionPane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
                jOptionPane.createDialog(null, "SkyEngine").setVisible(true);
                System.exit(-1);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
