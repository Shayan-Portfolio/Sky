package engine.ecs.compiler;

public interface LogStrategy {
    void warning(String message, int line, Context context, Class stage);
}
