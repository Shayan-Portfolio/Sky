package engine.mio;

public interface LogStrategy {
    void warning(String message, int line, Context context, Class stage);
}
