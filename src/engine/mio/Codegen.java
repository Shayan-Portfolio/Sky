package engine.mio;

public class Codegen {
    private Context context;
    public Codegen(Context source) {
        this.context = source;
    }



    public BytecodeStream flatten(ASTNode node) {
        BytecodeStream stream = new BytecodeStream();
        visit(node, stream);

        return stream;
    }

    private Object toNativeOperandType(Analyzer.Token token) {
        switch (token.type) {
            case String : {
                return token.content.toString();
            }
            case Numeric: {
                return Float.parseFloat(token.content.toString());
            }
            case True: {
                return true;
            }
            case False: {
                return false;
            }

        }
        return null;
    }

    private void visit(ASTNode node, BytecodeStream stream) {
        for(ASTNode child : node.getChildren()) {
            Analyzer.Token[] tokens = child.getTokens();
            if(tokens[0].type == Analyzer.Token.TokenType.Identifier) {
                Object[] operands;

                String name = tokens[0].content.toString();
                String type = tokens[1].type.toString();

                if(tokens.length == 2) {
                    operands = new Object[] { name, type, toNativeOperandType(tokens[1]) };
                }
                else {

                    operands = new Object[tokens.length];
                    {
                        operands[0] = name;
                        operands[1] = type;
                        for (int i = 2; i < tokens.length; i++) {
                            operands[i] = toNativeOperandType(tokens[i]);
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
                boolean endingActor = node.getChildren()
                        .getFirst()
                        .getTokens()[0]
                        .type == Analyzer.Token.TokenType.Actor;

                stream.emit(new Bytecode(endingActor ? Opcodes.EndActor : Opcodes.EndAdd, new Object[] {}));
            }










        }
    }
}
