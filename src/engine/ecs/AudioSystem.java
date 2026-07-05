package engine.ecs;

import engine.audio.*;
import engine.audio.wav.WavUtil;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;

import java.io.ByteArrayInputStream;
import java.util.*;

public class AudioSystem extends ActorSystem {
    private AudioDevice device;
    private AudioContext context;
    private Thread thread;

    //Volatile thread shared variables
    //private volatile org.joml.Vector3f receiverPos = new Vector3f();
    private volatile float rx, ry, rz, ox, oy, oz;

    private volatile boolean running = true;
    private final List<AudioSourceComponent> sources = Collections.synchronizedList(new ArrayList<>());


    public AudioSystem() {
        device = AudioDevice.getSystemAudioDevice();
        context = new AudioContext(device);

        thread = new Thread(() -> {
            Logger.info(AudioSystem.class, "Acquired device " + device.getName());

            while(running) {
                synchronized (sources) {
                    for (AudioSourceComponent sourceComponent : sources) {

                        if(sourceComponent.source != null) {
                            sourceComponent.source.setPosition(
                                    sourceComponent.position.x,
                                    sourceComponent.position.y,
                                    sourceComponent.position.z
                            );
                        }


                        if(sourceComponent.sampleChanged) {
                            sourceComponent.sample = WavUtil.loadAudioWAV(device, new ByteArrayInputStream(sourceComponent.sampleAsset.getObject()));
                            sourceComponent.sampleChanged = false;
                        }

                        if (!sourceComponent.registered) {
                            sourceComponent.source = new AudioSource(device);
                            sourceComponent.registered = true;
                            context.addAudioSource(sourceComponent.source);
                        }
                        if (sourceComponent.playing) {
                            sourceComponent.source.play(sourceComponent.sample, new AudioPlayback(false));
                            sourceComponent.playing = false;
                        }


                    }

                    sources.removeIf(audioSourceComponent -> {
                        if(!audioSourceComponent.playing)
                            context.removeAudioSource(audioSourceComponent.source);
                        return !audioSourceComponent.playing;
                    });
                }



                AudioReceiver audioReceiver = AudioReceiver.getInstance();
                audioReceiver.setPosition(rx, ry, rz);
                audioReceiver.setOrientation(ox, oy, oz);


                Optional<AudioDeviceEvent> event = context.pollEvent();

                if(event.isPresent()) {


                    if(event.get().hasFlag(AudioDeviceEvent.DeviceChanged)) {
                        device.free();
                        context.free();

                        device = AudioDevice.getSystemAudioDevice();
                        context = new AudioContext(device);
                    }
                }


            }
        });
        thread.setName("Audio Thread");
        thread.start();

    }
    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {

            if(actor.has(AudioReceiverComponent.class)) {
                AudioReceiverComponent audioReceiverComponent = actor.getComponent(AudioReceiverComponent.class);
                org.joml.Vector3f pos = (audioReceiverComponent.position());
                rx = pos.x;
                ry = pos.y;
                rz = pos.z;

                org.joml.Vector3f dir = (audioReceiverComponent.direction());
                ox = dir.x;
                oy = dir.y;
                oz = dir.z;
            }
            if(actor.has(AudioSourceComponent.class)) {
                AudioSourceComponent audioSourceComponent = actor.getComponent(AudioSourceComponent.class);
                if(audioSourceComponent.playing && audioSourceComponent.sampleChanged) {
                    synchronized (sources) {
                        sources.add(audioSourceComponent);
                    }
                }
            }



        });

    }

    @Override
    public void dispose() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new SkyRuntimeException(e);
        }
        context.free();
        device.free();
    }
}
