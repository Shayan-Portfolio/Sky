package engine.gameui;

import engine.graphics.Color;
import engine.graphics.text.MsdfFont;

import static engine.gameui.TextValue.text;

public class Node extends ContainerWidget {
    private boolean expanded;
    private Button button;
    private TextValue value;
    private int indent = 20;
    private Color color;
    public Node(TextValue value, MsdfFont msdfFont) {
        this.value = value;
        setLayoutEngine(new LineLayoutEngine(LineLayoutEngine.Line.Vertical));
        button = new Button(value, msdfFont);
        setIgnore(true);

        button.addEventHandler(new EventHandler() {
            @Override
            public void onClick() {
                expanded = !expanded;
            }
        });
        color = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);

    }

    @Override
    public void onAdded() {
        if(parent instanceof Node) {
            setIgnore(true);
            setPadding(0);
        }
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    public TextValue getValue() {
        return value;
    }

    @Override
    public int getRequiredWidth() {
        if(expanded) return super.getRequiredWidth() + (indent);
        else return button.getRequiredWidth();
    }

    @Override
    public int getRequiredHeight() {
        int h = button.getRequiredHeight();
        if(expanded) return super.getRequiredHeight() + h;
        return h;
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {
        int bx = button.getRequiredWidth(), by = button.getRequiredHeight();
        button.update(platform, x, y, bx, by);
        if(expanded) super.update(platform, x + indent, y + by, w, h);

    }
}
