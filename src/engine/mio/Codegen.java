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
            if(tokens[0].type == Analyzer.Token.TokenType.Identifier) {
                Object[] operands;

                String name = tokens[0].content.toString();
                String type = tokens[1].type.toString();

                if(tokens.length == 2) {
                    String weakValue = tokens[1].content.toString();
                    operands = new Object[] { name, type, weakValue };
                }
                else {

                    operands = new Object[tokens.length];
                    {
                        operands[0] = name;
                        operands[1] = type;
                        for (int i = 2; i < tokens.length; i++) {
                            operands[i] = tokens[i].content.toString();
                        }
                    }
                }

                
                stream.emit(new Bytecode(Opcodes.DeclConstant, operands));
            }

            if(tokens[0].type == Analyzer.Token.TokenType.Actor) {
                String name = tokens[1].content.toString();
                stream.emit(new Bytecode(Opcodes.BeginActor, new Object[] { name }));
                visit(child, stream);
            }
            if(tokens[0].type == Analyzer.Token.TokenType.Add) {
                String name = tokens[1].content.toString();
                stream.emit(new Bytecode(Opcodes.BeginAdd, new Object[] { name }));
                visit(child, stream);
            }
            if(tokens[0].type == Analyzer.Token.TokenType.End) {
                stream.emit(new Bytecode(Opcodes.End, new Object[] {}));
            }










        }
    }
}
