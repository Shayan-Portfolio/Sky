package engine.graphics;

public class Dependency {
    private String name;
    private Resource dependency;
    private int type;


    public Dependency(String name, Resource dependency, int type) {
        this.name = name;
        this.dependency = dependency;
        this.type = type;
    }


    public String getName() {
        return name;
    }

    public Resource getResource() {
        return dependency;
    }

    public void setResource(Resource dependency) {
        this.dependency = dependency;
    }

    public int getType() {
        return type;
    }
}
