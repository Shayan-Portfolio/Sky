package engine.graphics;

import engine.graphics.vulkan.VulkanComputePass;
import engine.graphics.vulkan.VulkanGraphicsPass;
import engine.logging.SkyRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class Pass extends Disposable {
    protected int frameIndex;
    protected int framesInFlight;
    protected Semaphore[] waitSemaphores;
    protected Semaphore[] finishedSemaphores;
    protected BarrierCallback barrierCallback;
    protected Runnable recorder;
    protected List<Dependency> dependencyList = new ArrayList<>();
    protected String name;



    public Pass(Disposable parent, String name, int framesInFlight) {
        super(parent);
        this.name = name;
        this.framesInFlight = framesInFlight;
    }

    public void addDependencies(Dependency... deps) {
        dependencyList.addAll(Arrays.asList(deps));
    }

    public List<Dependency> getDependencies() {
        return dependencyList;
    }



    public static GraphicsPass newGraphicsPass(Disposable disposable, String name, int framesInFlight) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanGraphicsPass(disposable, name, framesInFlight);
        return null;
    }

    public static ComputePass newComputePass(Disposable disposable, String name, int framesInFlight) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanComputePass(disposable, name, framesInFlight);
        return null;
    }

    public <T> void bind(String name, T resource) {
        Dependency dependency = null;
        for(Dependency rd : dependencyList) {
            if(rd.getName().equals(name)) dependency = rd;
        }
        if(dependency == null) throw new SkyRuntimeException("Unable to find dependency " + name);
        else dependency.setResource(new RenderGraphResource(resource));
    }

    public String getName() {
        return name;
    }


    public Runnable getRecorder() {
        return recorder;
    }

    public void submit(Runnable recorder) {
        this.recorder = recorder;
    }

    public <T> void reads(String name, T resource, int readType) {
        addDependencies(new Dependency(name, new RenderGraphResource(resource), readType));
    }

    public <T> void writes(String name, T resource, int writeType) {
        addDependencies(new Dependency(name, new RenderGraphResource(resource), writeType));
    }

    public void clearAll() {
        dependencyList.clear();
    }

    public BarrierCallback getBarrierInsertCallback() {
        return barrierCallback;
    }

    public void setBarrierCallback(BarrierCallback barrierCallback) {
        this.barrierCallback = barrierCallback;
    }

    public void startRecording(int frameIndex) {
        this.frameIndex = frameIndex;
    }
    public abstract void endRecording();

    public void setWaitSemaphores(Semaphore[] waitSemaphores) {
        this.waitSemaphores = waitSemaphores;
    }

    public void setFinishedSemaphores(Semaphore[] finishedSemaphores) {
        this.finishedSemaphores = finishedSemaphores;
    }

    public abstract void submit(Optional<Fence[]> submissionFences);
    public abstract void waitForFinish();

    public Semaphore[] getWaitSemaphores() {
        return waitSemaphores;
    }

    public Semaphore[] getFinishedSemaphores() {
        return finishedSemaphores;
    }

    public abstract void resolveBarriers();
}
