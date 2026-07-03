package engine.mio;

import engine.logging.SkyRuntimeException;

public class Mio {
    public static BytecodeStream compile(String source) {
        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser();

        try {
            ASTNode node = parser.parseAll(tokenizer);
            Codegen codegen = new Codegen();
            return codegen.flatten(node);
        }
        catch (ParseException e) {
            throw new SkyRuntimeException(e);
        }


    }
}
