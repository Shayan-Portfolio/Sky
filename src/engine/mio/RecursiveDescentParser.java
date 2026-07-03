package engine.mio;

import java.util.Arrays;
import java.util.Stack;


public class RecursiveDescentParser {

    public RecursiveDescentParser() {}


    private Analyzer.Token nextProperToken(Analyzer analyzer) {
        Analyzer.Token token;
        while ((token = analyzer.next()) != null) {
            return token;
        }
        return null;
    }

    private Analyzer.Token expect(Analyzer analyzer, Analyzer.Token.TokenType... types) throws ParseException {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) {
            throw new ParseException("Expected any of " + Arrays.asList(types) + " next instead of EOF");
        }

        for(Analyzer.Token.TokenType type : types) {
            if(token.type.equals(type)) return token;
        }
        throw new ParseException("Expected any of " + Arrays.asList(types) + " next instead of " + token.type + " (" + token.content.toString() + ") on line " + token.line);
    }
    private void error(Analyzer.Token token, String message) throws ParseException {
        throw new ParseException(message + " on line " + token.line);
    }


    private Stack<Analyzer.Token> blocks = new Stack<>();
    private Stack<ASTNode> ast = new Stack<>();
    public ASTNode parseAll(Analyzer analyzer) throws ParseException {
        ASTNode root = new ASTNode();
        ast.push(root);
        while(true) {
            if(!parseSpecific(analyzer)) break;
        }
        if(!blocks.isEmpty()) throw new RuntimeException("An actor declaration is missing a matching 'end'");
        return root;
    }
    private void pushASTNode(ASTNode node) {
        ast.peek().addChild(node);
        ast.push(node);
    }

    private void popASTNode() {
        ast.pop();
    }

    public boolean parseSpecific(Analyzer analyzer) throws ParseException {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) return false;

        switch (token.type) {

            case Actor, Add: {
                Analyzer.Token next = expect(analyzer, Analyzer.Token.TokenType.Identifier);
                blocks.push(token);
                pushASTNode(new ASTNode(token, next));

                break;
            }

            case Identifier: {
                expect(analyzer, Analyzer.Token.TokenType.Equals);
                Analyzer.Token n2 = expect(
                        analyzer,
                        Analyzer.Token.TokenType.String,
                        Analyzer.Token.TokenType.Numeric,
                        Analyzer.Token.TokenType.True,
                        Analyzer.Token.TokenType.False,
                        Analyzer.Token.TokenType.Float,
                        Analyzer.Token.TokenType.Int,
                        Analyzer.Token.TokenType.Vec2,
                        Analyzer.Token.TokenType.Vec3
                );




                //Strong types
                if(n2.type != Analyzer.Token.TokenType.Numeric && n2.type != Analyzer.Token.TokenType.String && n2.type != Analyzer.Token.TokenType.False && n2.type != Analyzer.Token.TokenType.True) {
                    expect(analyzer, Analyzer.Token.TokenType.LParen);
                    {
                        switch (n2.type) {
                            case Int -> {
                                Analyzer.Token i1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                if(i1.content.toString().contains(".")) error(i1, "Expected an int literal");
                                pushASTNode(new ASTNode(token, n2, i1));
                                popASTNode();
                                break;
                            }
                            case Float -> {
                                Analyzer.Token f1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                pushASTNode(new ASTNode(token, n2, f1));
                                popASTNode();
                                break;
                            }
                            case Vec2 -> {
                                Analyzer.Token f1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                Analyzer.Token f2 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                pushASTNode(new ASTNode(token, n2, f1, f2));
                                popASTNode();
                                break;
                            }
                            case Vec3 -> {
                                Analyzer.Token f1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                Analyzer.Token f2 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                Analyzer.Token f3 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                pushASTNode(new ASTNode(token, n2, f1, f2, f3));
                                popASTNode();
                                break;
                            }
                        }

                    }
                    expect(analyzer, Analyzer.Token.TokenType.RParen);
                }
                else {
                    pushASTNode(new ASTNode(token, n2));
                    popASTNode();
                }

                break;
            }

            case End: {
                blocks.pop();
                popASTNode();
                pushASTNode(new ASTNode(token));
                popASTNode();
                break;
            }

            default: {
                error(token, "Unexpected token '" + token.content + "'");
            }









        }


        return true;
    }

}
