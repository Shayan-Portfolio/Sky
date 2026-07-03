package engine.mio;

import engine.logging.SkyRuntimeException;

import java.util.Arrays;
import java.util.Stack;


public class RecursiveDescentParser {

    public RecursiveDescentParser() {}
    private SceneBytecode ir = new SceneBytecode();

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

    public SceneBytecode getEmittedBytecode() {
        return ir;
    }

    private Stack<Analyzer.Token> tokens = new Stack<>();
    public void parseAll(Analyzer analyzer) throws ParseException {
        while(true) {
            if(!parseSpecific(analyzer)) break;
        }
        if(!tokens.isEmpty()) throw new RuntimeException("An actor declaration is missing a matching 'end'");
    }
    public boolean parseSpecific(Analyzer analyzer) throws ParseException {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) return false;

        switch (token.type) {

            case ActorKeyword: {
                Analyzer.Token next = expect(analyzer, Analyzer.Token.TokenType.IdentifierToken);
                tokens.push(token);

                break;
            }

            case AddKeyword: {
                Analyzer.Token next = expect(analyzer, Analyzer.Token.TokenType.IdentifierToken);
                tokens.push(token);

                break;
            }

            case IdentifierToken: {
                expect(analyzer, Analyzer.Token.TokenType.Equals);
                Analyzer.Token n2 = expect(
                        analyzer,
                        Analyzer.Token.TokenType.String,
                        Analyzer.Token.TokenType.Numeric,
                        Analyzer.Token.TokenType.TrueKeyword,
                        Analyzer.Token.TokenType.FalseKeyword,
                        Analyzer.Token.TokenType.Float1Keyword,
                        Analyzer.Token.TokenType.Int1Keyword,
                        Analyzer.Token.TokenType.Vec2Keyword,
                        Analyzer.Token.TokenType.Vec3Keyword
                );




                //Strong types
                if(n2.type != Analyzer.Token.TokenType.Numeric && n2.type != Analyzer.Token.TokenType.String && n2.type != Analyzer.Token.TokenType.FalseKeyword && n2.type != Analyzer.Token.TokenType.TrueKeyword) {
                    expect(analyzer, Analyzer.Token.TokenType.LParen);
                    {
                        switch (n2.type) {
                            case Int1Keyword -> {
                                Analyzer.Token n3 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                if(n3.content.toString().contains(".")) error(n3, "Expected an int literal");
                                break;
                            }
                            case Float1Keyword -> {
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                break;
                            }
                            case Vec2Keyword -> {
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                break;
                            }
                            case Vec3Keyword -> {
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                expect(analyzer, Analyzer.Token.TokenType.Comma);
                                expect(analyzer, Analyzer.Token.TokenType.Numeric);
                                break;
                            }
                        }

                    }
                    expect(analyzer, Analyzer.Token.TokenType.RParen);
                }

                break;
            }

            case EndKeyword: {
                tokens.pop();
                break;
            }









        }


        return true;
    }

}
