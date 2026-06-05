package engine.graphics;

public class Dependency {
    private String name;
    private RenderGraphResource dependency;
    private int type;


    public Dependency(String name, RenderGraphResource dependency, int type) {
        this.name = name;
        this.dependency = dependency;
        this.type = type;
    }


    public String getName() {
        return name;
    }

    public RenderGraphResource getResource() {
        return dependency;
    }

    public void setResource(RenderGraphResource dependency) {
        this.dependency = dependency;
    }

    public int getType() {
        return type;
    }
}
