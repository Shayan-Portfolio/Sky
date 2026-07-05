package engine.mio;

/*
God help you if you're actually trying to understand this lexer
 */
public class Analyzer {
    private String source;
    private Context context;
    private int index = 0;
    public Analyzer(Context context) {
        this.context = context;
        this.source = context.src() + Character.MIN_VALUE;
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
                index++;
                continue;
            }

            if(!comment) {



                //Precheck
                if(!string) {
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
                        case '[':
                            token.type = Token.TokenType.LBracket;
                            index++;
                            break top;
                        case ']':
                            token.type = Token.TokenType.RBracket;
                            index++;
                            break top;
                    }

                }

                if(Character.isWhitespace(character) && !string) {
                    if(!token.content.toString().isBlank()) {
                        token.type = Token.TokenType.Identifier;
                        break;
                    }
                    index++;
                    continue;
                }

                token.content.append(character);

                //Expanding
                {


                    if (!string) {
                        if ((Character.isDigit(character) || character == '.') && !hasLetters(token.content.toString())) {

                            numeric = true;
                            token.type = Token.TokenType.Numeric;
                            index++;
                            continue;
                        }
                        else numeric = false;
                    }
                    if (character == '\"') {
                        boolean escaped = token.content.length() > 1 && token.content.charAt(token.content.length() - 2) == '\\';

                        if(escaped) {
                            token.content.deleteCharAt(token.content.length() - 2);
                        }

                        else {


                            if (!string) {
                                string = true;
                                token.type = Token.TokenType.String;
                                index++;
                                continue;
                            } else {
                                string = false;
                                index++;

                                token.content.deleteCharAt(token.content.length() - 1);
                                token.content.deleteCharAt(0);



                                break;
                            }
                        }

                    }
                }


            }

            index++;

        }


        return token.type == null ? null : token;
    }

    private static boolean hasLetters(String text) {
        for(char character : text.toCharArray()) {
            if(Character.isLetter(character)) return true;
        }
        return false;
    }



    public class Token {
        public TokenType type;
        public enum TokenType {
            LParen("("),
            RParen(")"),
            Equals("="),
            Comma(","),
            Actor("actor"),
            Identifier(null),
            End("end"),
            Add("add"),
            Float("float"),
            Int("int"),
            True("true"),
            False("false"),
            Vec2("vec2"),
            Vec3("vec3"),
            Uses("uses"),
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
