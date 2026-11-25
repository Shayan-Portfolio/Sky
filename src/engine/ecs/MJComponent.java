package engine.ecs;


@ComponentArray(mask = 1 << 6)
public class MJComponent implements Component {

    @Override
    public void removed(Actor a) {}
}
