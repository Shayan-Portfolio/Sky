package engine.logging;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Logger {

    private static File output;
    private static PrintWriter writer;

    private enum LogTarget {
        Console,
        File,
    }

    private static LogTarget target;

    private Logger(){}
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUD = "\u001b[41m";
    public static final String ANSI_WHITE = "\u001b[37m";


    public static void setFileTarget(File file){
        setConsoleTarget(System.out);
        info(Logger.class, "The log file can be found at " + file.getAbsolutePath());
        output = file;


        target = LogTarget.File;
        try {
            writer = new PrintWriter(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new SkyRuntimeException(e);
        }
    }

    public static void setConsoleTarget(OutputStream outputStream){
        output = null;
        target = LogTarget.Console;
        writer = new PrintWriter(new OutputStreamWriter(outputStream));
    }

    public static File getOutputFile() {
        return output;
    }

    private static String timeStr(){

        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    private static synchronized String logGeneric(String ansiColor, String source, String type, String message){
        if(target == LogTarget.Console)
            writer.println(ansiColor + "[" + Thread.currentThread().getName() + "|" + timeStr() + "] " + source + " " + type + ": " + message + ANSI_RESET);
        else
            writer.println(" (" + timeStr() + " " + source + " " + type + ") " + message);

        writer.flush();
        return message;
    }

    public static String info(Class source, String message){
        return logGeneric(ANSI_WHITE, source.getSimpleName(), "info", message);
    }

    public static String error(Class source, String message){
        return logGeneric(ANSI_RED, source.getSimpleName(), "error", message);
    }
    public static String meltdown(Class source, String message){
        return logGeneric(ANSI_RED_BACKGROUD + ANSI_BLACK, source.getSimpleName(), "!!MELTDOWN!!", message);
    }

    public static String todo(Class source, String message){
        return logGeneric(ANSI_YELLOW, source.getSimpleName(), "todo", message);
    }



}
