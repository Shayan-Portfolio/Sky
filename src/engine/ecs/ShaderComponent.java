package engine.ecs;

import engine.graphics.ShaderProgram;
@ComponentArray(mask = 1 << 8)
public record ShaderComponent(ShaderProgram shaderProgram) implements Component {
    @Override
    public void removed(Actor a) {
        shaderProgram.disposeAll();
        a.removeDisposable(shaderProgram);
    }
}
