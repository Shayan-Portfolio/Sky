package engine.ecs.compiler;

public class DefaultLogStrategy implements LogStrategy {
    @Override
    public void warning(String message, int line, Context context, Class stage) {
        System.out.printf(
                "%s:%d: warning [%s]: %s%n",
                context.name(),
                line,
                stage.getSimpleName(),
                message
        );
    }
}
