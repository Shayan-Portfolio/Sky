package engine.graphics;

public class Attachment {
    private long flags = 0;
    private Texture[] textures;
    private Sampler[] samplers;

    public Attachment(long flags, Texture[] textures, Sampler[] samplers) {
        this.flags = flags;
        this.textures = textures;
        this.samplers = samplers;
    }

    public long getFlags() {
        return flags;
    }

    public Sampler[] getSamplers() {
        return samplers;
    }

    public Texture[] getTextures() {
        return textures;
    }
}
