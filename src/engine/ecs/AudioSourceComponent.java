package engine.ecs;

import engine.asset.Asset;
import engine.audio.AudioBuffer;
import engine.audio.AudioSource;
import org.joml.Vector3f;

@ComponentArray(mask = 1 << 11)
public class AudioSourceComponent {
    public Vector3f position;
    public Asset<byte[]> sampleAsset;
    public boolean registered;
    public boolean sampleChanged;
    public boolean playing;
    public AudioBuffer sample;
    public AudioSource source;


    public Vector3f getPosition() {
        return position;
    }

    public AudioSourceComponent setPosition(Vector3f position) {
        this.position = position;
        return this;
    }

    public Asset<byte[]> getSampleAsset() {
        return sampleAsset;
    }
    public AudioSourceComponent setSampleAsset(Asset<byte[]> sampleAsset) {
        this.sampleAsset = sampleAsset;
        this.sampleChanged = true;
        return this;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public AudioSource getSource() {
        return source;
    }

    public void setSource(AudioSource source) {
        this.source = source;
    }

    public void play() {
        playing = true;
    }
}
