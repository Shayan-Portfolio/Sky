package engine.graphics;

import java.util.*;

public class RenderGraph extends Disposable {

    private List<Pass> passes = new ArrayList<>();
    private Pass targetPass;

    public RenderGraph(Disposable parent) {
        super(parent);
    }

    public void setTargetPass(Pass targetPass) {
        this.targetPass = targetPass;
    }

    public Pass getTargetPass() {
        return targetPass;
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

    public List<Pass> walk(Pass targetPass) {
        LinkedList<Pass> passes = new LinkedList<>();
        tracePasses(passes, targetPass);
        passes.add(targetPass);

        return passes;
    }


    private Pass getWriter(Pass thisPass, Dependency dependency) {


        for(Pass otherPass : passes) {
            if(otherPass != thisPass) {
                for (Dependency otherDependency : otherPass.getDependencies()) {
                    if((otherDependency.getType() & DependencyTypes.RenderTargetWrite) != 0 ||
                            (otherDependency.getType() & DependencyTypes.RenderTargetWriteDepth) != 0 ||
                        (otherDependency.getType() & DependencyTypes.FragmentShaderWrite) != 0 ||
                        (otherDependency.getType() & DependencyTypes.ComputeShaderWrite) != 0) {
                        if(otherDependency.getResource() == dependency.getResource()) {
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

            if((dependency.getType() & DependencyTypes.RenderTargetRead) != 0 ||
                    (dependency.getType() & DependencyTypes.FragmentShaderRead) != 0 ||
                    (dependency.getType() & DependencyTypes.FragmentShaderReadDepth) != 0 ||

                    (dependency.getType() & DependencyTypes.ComputeShaderRead) != 0 ||
                    (dependency.getType() & DependencyTypes.ComputeShaderReadDepth) != 0) {

                Pass writer = getWriter(thisPass, dependency);

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
