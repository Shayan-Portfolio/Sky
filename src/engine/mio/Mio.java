package engine.mio;

import engine.logging.SkyRuntimeException;

public class Mio {
    public static BytecodeStream compile(Context source) throws CompilerException {
        Analyzer tokenizer = new Analyzer(source);

        RecursiveDescentParser parser = new RecursiveDescentParser(source);

        ASTNode node = parser.parseAll(tokenizer);
        Codegen codegen = new Codegen(source);
        return codegen.flatten(node);


    }
}
