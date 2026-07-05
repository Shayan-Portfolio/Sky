package engine.graphics;

public class AccessTypes {

    public static final int ColorRead = 1;
    public static final int ColorWrite = 1 << 1;
    public static final int ColorReadWrite = 1 << 2;
    public static final int ShaderRead = 1 << 3;
    public static final int ShaderWrite = 1 << 4;
    public static final int ShaderReadWrite = 1 << 5;
    public static final int DepthWrite = 1 << 6;
    public static final int DepthRead = 1 << 7;
    public static final int DepthReadWrite = 1 << 8;
    public static final int Present = 1 << 9;


    private AccessTypes(){}
}
