package engine.mio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class RDParser {

    public RDParser() {}
    private SceneBytecode ir = new SceneBytecode();

    private Analyzer.Token nextProperToken(Analyzer analyzer) {
        Analyzer.Token token;
        while ((token = analyzer.next()) != null) {
            return token;
        }
        return null;
    }

    private Analyzer.Token expect(Analyzer analyzer, Analyzer.Token.TokenType... types) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) {
            throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of EOF");
        }

        for(Analyzer.Token.TokenType type : types) {
            if(token.type.equals(type)) return token;
        }
        throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of " + token.type + " (" + token.content.toString() + ") on line " + token.line);
    }

    public SceneBytecode getEmittedBytecode() {
        return ir;
    }

    private Stack<Analyzer.Token> tokens = new Stack<>();
    public void parseContinuous(Analyzer analyzer){
        while(true) {
            if(!parseSpecific(analyzer)) break;
        }
        if(!tokens.isEmpty()) throw new RuntimeException("An actor declaration is missing a matching 'end'");
    }
    public boolean parseSpecific(Analyzer analyzer) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) return false;

        switch (token.type) {
            case ActorKeyword: {
                Analyzer.Token next = expect(analyzer, Analyzer.Token.TokenType.StringLiteral);
                tokens.push(next);
                ir.emit(new Instruction(
                        Opcode.PushActor,
                        new Object[]{ next.content.toString() }
                ));
                parseSpecific(analyzer);
                break;
            }
            case EndKeyword: {
                tokens.pop();
                ir.emit(new Instruction(
                        Opcode.PopActor,
                        new Object[]{}
                ));
                break;
            }
            case PassKeyword: {
                break;
            }

            case StringLiteral: {
                //Strings can also be preceded by 'actor_keyword'
                Analyzer.Token next = expect(
                        analyzer,
                        Analyzer.Token.TokenType.DataKeyword,
                        Analyzer.Token.TokenType.Float1Keyword,
                        Analyzer.Token.TokenType.Float2Keyword,
                        Analyzer.Token.TokenType.Float3Keyword,
                        Analyzer.Token.TokenType.Float4Keyword,
                        Analyzer.Token.TokenType.Euler3Keyword,
                        Analyzer.Token.TokenType.Quat4Keyword,
                        Analyzer.Token.TokenType.StringKeyword,
                        Analyzer.Token.TokenType.BoolKeyword,
                        Analyzer.Token.TokenType.ArrayKeyword
                );


                switch (next.type) {
                    case DataKeyword: {
                        ir.emit(new Instruction(
                                Opcode.AddData,
                                new Object[]{ token.content.toString() }
                        ));
                        parseContinuous(analyzer);
                        break;
                    }
                    case BoolKeyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.TrueKeyword, Analyzer.Token.TokenType.FalseKeyword);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);

                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), Boolean.parseBoolean(a1.content.toString()) }
                        ));

                        break;
                    }
                    case StringKeyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.StringLiteral);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);

                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), a1.content.toString() }
                        ));

                        break;
                    }
                    case Float1Keyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);

                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), Float.parseFloat(a1.content.toString()) }
                        ));

                        break;
                    }
                    case Float2Keyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a2 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);

                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), Float.parseFloat(a1.content.toString()), Float.parseFloat(a2.content.toString()) }
                        ));
                        break;
                    }
                    case Float3Keyword, Euler3Keyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a2 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a3 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);
                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), Float.parseFloat(a1.content.toString()), Float.parseFloat(a2.content.toString()), Float.parseFloat(a3.content.toString()) }
                        ));

                        break;
                    }
                    case Float4Keyword, Quat4Keyword: {
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        expect(analyzer, Analyzer.Token.TokenType.LeftParen);
                        Analyzer.Token a1 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a2 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a3 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter);
                        Analyzer.Token a4 = expect(analyzer, Analyzer.Token.TokenType.Numeric);
                        expect(analyzer, Analyzer.Token.TokenType.RightParen);
                        expect(analyzer, Analyzer.Token.TokenType.ArgDelimiter, Analyzer.Token.TokenType.RightParen);
                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                new Object[]{ token.content.toString(), Float.parseFloat(a1.content.toString()), Float.parseFloat(a2.content.toString()), Float.parseFloat(a3.content.toString()), Float.parseFloat(a4.content.toString()) }
                        ));
                        break;
                    }
                    case ArrayKeyword: {
                        expect(analyzer, Analyzer.Token.TokenType.ArrayStart);
                        int arraySize = 0;

                        ArrayList<Object> array = new ArrayList<>();
                        array.add(next.content.toString());
                        Analyzer.Token last = null;
                        while(true) {
                            Analyzer.Token t = expect(
                                    analyzer,
                                    Analyzer.Token.TokenType.Numeric,
                                    Analyzer.Token.TokenType.TrueKeyword,
                                    Analyzer.Token.TokenType.FalseKeyword,
                                    Analyzer.Token.TokenType.StringLiteral,
                                    Analyzer.Token.TokenType.ArgDelimiter,
                                    Analyzer.Token.TokenType.ArrayEnd
                            );

                            if(t.type == Analyzer.Token.TokenType.ArrayEnd){
                                break;
                            }
                            else if(t.type != Analyzer.Token.TokenType.ArgDelimiter) {
                                if(last != null && last.type != t.type) {
                                    throw new RuntimeException("Expected type " + last.type + " at index " + arraySize + " in the array instead of " + t.type);
                                }

                                last = t;
                                arraySize++;

                                switch (t.type) {
                                    case Numeric -> array.add(Float.parseFloat(t.content.toString()));
                                    case TrueKeyword -> array.add(true);
                                    case FalseKeyword -> array.add(false);
                                    case StringLiteral -> array.add(t.content.toString());
                                }
                            }

                        }

                        ir.emit(new Instruction(
                                Opcode.AddProperty,
                                array.toArray()
                        ));



                        break;
                    }

                }

                break;
            }






        }

        return true;
    }

}
