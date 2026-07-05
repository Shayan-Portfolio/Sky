package engine.mio;

public class SemanticAnalyzer {
    private Context context;

    public SemanticAnalyzer(Context context) {
        this.context = context;
    }

    private void visit(ASTNode node) throws SemanticAnalysisException {
        if(node.getTokens().length >= 2) {
            Analyzer.Token k = node.getTokens()[0];
            Analyzer.Token identifier = node.getTokens()[1];
            if (k.type == Analyzer.Token.TokenType.Actor) {
                if (node.getChildren().isEmpty()) {
                    context.logStrategy().warning("Actor '" + identifier.content + "' is empty", k.line, context, SemanticAnalyzer.class);
                }

            }
        }
        for(ASTNode child : node.getChildren()) {
            for(ASTNode other : node.getChildren()) {
                if(child == other) continue;

                Analyzer.Token t1 = child.getTokens()[0];
                Analyzer.Token t2 = other.getTokens()[0];

                boolean isRedefined =
                        t1.type == Analyzer.Token.TokenType.Identifier &&
                        t2.type == Analyzer.Token.TokenType.Identifier &&
                        t1.content.toString().contentEquals(t2.content);

                if(isRedefined) {
                    throw new SemanticAnalysisException("Constant '" + child.getTokens()[0].content + "' is already defined in this scope", child.getTokens()[0].line);
                }
            }
            visit(child);
        }
    }

    public void analyze(ASTNode node) throws SemanticAnalysisException {
        visit(node);
    }
}
