package engine.mio;

import java.nio.charset.Charset;

/*
God help you if you're actually trying to understand this lexer
 */
public class Analyzer {
    private String source;
    private int index = 0;
    public Analyzer(String source) {
        this.source = source + Character.MIN_VALUE;
    }

    private int line = 1;
    public Token next() {

        boolean string = false, numeric = false, comment = false;
        Token token = new Token(line);

top:
        while (index < source.length()) {
            char character = source.charAt(index);

            if(character == '#') comment = true;
            if(character == '\n') {
                line++;
                comment = false;
            }

            if(!comment) {


                //Precheck
                {
                    for (Token.TokenType type : Token.TokenType.values()) {
                        if (type.value != null && type.value.equals(token.content.toString().strip())) {
                            token.type = type;
                            break top;
                        }
                    }


                    if (!Character.isDigit(character) && character != '.' && numeric) break;
                    switch (character) {
                        case '(':
                            token.type = Token.TokenType.LParen;
                            index++;
                            break top;
                        case ')':
                            token.type = Token.TokenType.RParen;
                            index++;
                            break top;
                        case '=':
                            token.type = Token.TokenType.Equals;
                            index++;
                            break top;
                        case ',':
                            token.type = Token.TokenType.Comma;
                            index++;
                            break top;
                    }

                }

                if(Character.isWhitespace(character) && !string) {
                    if(!token.content.toString().isBlank()) {
                        token.type = Token.TokenType.UnknownToken;
                        break;
                    }
                    index++;
                    continue;
                }

                token.content.append(character);

                //Expanding
                {


                    if(!string) {
                        if (Character.isDigit(character) || character == '.') {
                            numeric = true;
                            token.type = Token.TokenType.Numeric;
                        } else if (numeric) break;
                    }

                    if (!string) {
                        if (Character.isDigit(character)) {
                            numeric = true;
                            token.type = Token.TokenType.Numeric;
                            index++;
                            continue;
                        } else {
                            numeric = false;
                        }
                    }
                    if (character == '\"') {
                        if (!string) {
                            string = true;
                            token.type = Token.TokenType.String;
                            index++;
                            continue;
                        } else {
                            string = false;
                            index++;
                            break;
                        }

                    }
                }


            }

            index++;

        }


        return token.type == null ? null : token;
    }



    public class Token {
        public TokenType type;
        public enum TokenType {
            LParen("("),
            RParen(")"),
            Equals("="),
            Comma(","),
            ActorKeyword("actor"),
            UnknownToken(null),
            EndKeyword("end"),
            AddKeyword("add"),
            Float1Keyword("float"),
            Int1Keyword("int"),
            BoolKeyword("bool"),
            Vec2Keyword("vec2"),
            Vec3Keyword("vec3"),
            Vec4Keyword("vec4"),
            UsesKeyword("uses"),
            LetKeyword("let"),
            LBracket("["),
            RBracket("]"),
            String(null),
            Numeric(null);


            public String value;

            TokenType(String value) {
                this.value = value;
            }
        }
        public StringBuilder content = new StringBuilder();
        public int line;
        public Token(int line) {
            this.line = line;
        }
    }
}
