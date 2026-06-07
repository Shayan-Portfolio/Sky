package engine.graphics;

public class DependencyTypes {

    public static final int RenderTargetReadDepth = 1;
    public static final int RenderTargetWrite = 1 << 1;
    public static final int FragmentShaderRead = 1 << 2;
    public static final int FragmentShaderWrite = 1 << 3;
    public static final int Present = 1 << 4;
    public static final int ComputeShaderRead = 1 << 5;
    public static final int ComputeShaderWrite = 1 << 6;
    public static final int RenderTargetWriteDepth = 1 << 7;
    public static final int ComputeShaderReadDepth = 1 << 8;
    public static final int FragmentShaderReadDepth = 1 << 9;


    private DependencyTypes(){}
}
