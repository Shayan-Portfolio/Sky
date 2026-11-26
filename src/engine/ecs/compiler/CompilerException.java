package engine.ecs.compiler;

public class CompilerException extends Exception {
    protected int line;
    public CompilerException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getErrorLineNum() {
        return line;
    }
}
