package engine.ecs.compiler;

public class ParseException extends CompilerException {
    public ParseException(String message, int line) {
        super(message, line);
    }
}
