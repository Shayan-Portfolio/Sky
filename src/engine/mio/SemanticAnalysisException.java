package engine.mio;

public class SemanticAnalysisException extends CompilerException {
    public SemanticAnalysisException(String message, int line) {
        super(message, line);
    }
}
