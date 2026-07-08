package engine.util;

public class NamingUtil {
    public static String getCallStack() {
        StringBuilder builder = new StringBuilder();

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // Skip getStackTrace() and getCallStack()
        for (int i = 2; i < stack.length; i++) {
            builder.append(stack[i]);

            if (i < stack.length - 1) {
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }
}
