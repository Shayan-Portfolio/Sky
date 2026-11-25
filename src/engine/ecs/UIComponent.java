package engine.ecs;

import engine.gameui.Loop;
import engine.gameui.Widget;
import engine.graphics.Rect2D;

import java.util.Optional;

@ComponentArray(mask = 1 << 10)
public class UIComponent implements Component {
    public Widget widget;
    public Loop loop;
    public boolean active;
    public Optional<Rect2D> rect2D;


    public UIComponent(Widget widget, Optional<Rect2D> rect2D) {
        this.widget = widget;
        this.rect2D = rect2D;
    }

    @Override
    public void removed(Actor a) {}
}
