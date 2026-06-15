package engine.graphics;

import engine.logging.SkyRuntimeException;

import java.util.*;

public class RenderGraph extends Disposable {

    private List<Pass> passes = new ArrayList<>();

    public RenderGraph(Disposable parent) {
        super(parent);
    }



    public void addPasses(Pass... passes) {
        this.passes.addAll(Arrays.asList(passes));
    }

    public List<Pass> getPasses() {
        return passes;
    }

    @Override
    public void dispose() {

    }

    public List<Pass> compile(Pass sink) {
        LinkedList<Pass> passes = new LinkedList<>();
        tracePasses(passes, sink);
        passes.add(sink);

        return passes;
    }


    private Pass getWriter(Pass thisPass, Dependency dependency) {


        for(Pass otherPass : passes) {
            if(otherPass != thisPass) {
                for (Dependency otherDependency : otherPass.getDependencies()) {
                    if((otherDependency.getAccessType() & AccessTypes.ColorWrite) != 0 ||
                            (otherDependency.getAccessType() & AccessTypes.DepthWrite) != 0 ||
                        (otherDependency.getAccessType() & AccessTypes.ShaderWrite) != 0 ||
                        (otherDependency.getAccessType() & AccessTypes.ColorReadWrite) != 0 ||
                            (otherDependency.getAccessType() & AccessTypes.DepthReadWrite) != 0 ||
                            (otherDependency.getAccessType() & AccessTypes.ShaderReadWrite) != 0) {
                        if(otherDependency.getResource().get() == dependency.getResource().get()) {
                            return otherPass;
                        }
                    }
                }


            }
        }

        return null;
    }
    private void tracePasses(LinkedList<Pass> passes, Pass thisPass) {
        for(Dependency dependency : thisPass.getDependencies()) {

            if((dependency.getAccessType() & AccessTypes.ColorRead) != 0 ||
                    (dependency.getAccessType() & AccessTypes.ColorReadWrite) != 0 ||
                    (dependency.getAccessType() & AccessTypes.ShaderReadWrite) != 0 ||
                    (dependency.getAccessType() & AccessTypes.ShaderRead) != 0 ||
                    (dependency.getAccessType() & AccessTypes.DepthRead) != 0 ||
                    (dependency.getAccessType() & AccessTypes.DepthReadWrite) != 0) {

                Pass writer = getWriter(thisPass, dependency);
                if(writer == null) throw new SkyRuntimeException("No writer for " + dependency.getName());

                if(!passes.contains(writer)) {
                    tracePasses(passes, writer);
                    passes.add(writer);
                }
                else {
                    passes.remove(writer);
                    passes.add(writer);
                }


            }

        }

    }
}
