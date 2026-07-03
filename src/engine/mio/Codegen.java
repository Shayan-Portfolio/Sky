package engine.mio;

public class Codegen {
    public Codegen() {

    }



    public BytecodeStream flatten(ASTNode node) {
        BytecodeStream stream = new BytecodeStream();
        visit(node, stream);

        return stream;
    }

    private void visit(ASTNode node, BytecodeStream stream) {
        for(ASTNode child : node.getChildren()) {
            Analyzer.Token[] tokens = child.getTokens();
            if(tokens[0].type == Analyzer.Token.TokenType.IdentifierToken) {
                String name = tokens[0].content.toString();
                String value = tokens[1].content.toString();
                stream.emit(new Bytecode(Opcodes.DeclConstant, new Object[] { name, value }));
            }

            if(tokens[0].type == Analyzer.Token.TokenType.ActorKeyword) {
                String name = tokens[1].content.toString();
                stream.emit(new Bytecode(Opcodes.BeginActor, new Object[] { name }));
                visit(child, stream);
            }
            if(tokens[0].type == Analyzer.Token.TokenType.AddKeyword) {
                String name = tokens[1].content.toString();
                stream.emit(new Bytecode(Opcodes.BeginAdd, new Object[] { name }));
                visit(child, stream);
            }
            if(tokens[0].type == Analyzer.Token.TokenType.EndKeyword) {
                stream.emit(new Bytecode(Opcodes.End, new Object[] {}));
            }










        }
    }
}
