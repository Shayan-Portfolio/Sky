package engine.mio;

import java.util.ArrayList;
import java.util.List;

public class BytecodeStream {

    private List<Bytecode> frames = new ArrayList<>();

    public void emit(Bytecode bytecode) {
        frames.add(bytecode);
    }

    public List<Bytecode> getList() {
        return frames;
    }
}
