package engine.mio;

import engine.logging.SkyRuntimeException;

public class Mio {
    public static BytecodeStream compile(Context source) {
        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        try {
            ASTNode node = parser.parseAll(tokenizer);
            Codegen codegen = new Codegen(source);
            return codegen.flatten(node);
        }
        catch (ParseException e) {
            throw new SkyRuntimeException(e);
        }


    }
}
