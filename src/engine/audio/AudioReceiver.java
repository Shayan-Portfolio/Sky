package engine.audio;

import static org.lwjgl.openal.AL11.*;
public class AudioReceiver {
    private static AudioReceiver instance;
    private float x, y, z;
    private float vx, vy, vz;
    private float ox, oy, oz;


    private AudioReceiver() {}
    public static AudioReceiver getInstance() {
        if(instance == null) instance = new AudioReceiver();

        return instance;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        alListener3f(AL_POSITION, x, y, z);
    }

    public void setVelocity(float vx, float vy, float vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        alListener3f(AL_VELOCITY, vx, vy, vz);
    }

    public void setOrientation(float ox, float oy, float oz) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        alListenerfv(AL_ORIENTATION, new float[]{ox, oy, oz, 0, 1, 0});
    }

}
