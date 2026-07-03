package engine.mio;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private List<ASTNode> children = new ArrayList<>();
    private Analyzer.Token[] tokens;

    public ASTNode(Analyzer.Token... tokens) {
        this.tokens = tokens;
    }

    public Analyzer.Token[] getTokens() {
        return tokens;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }

    public void removeChild(ASTNode child) {
        children.remove(child);
    }
}
