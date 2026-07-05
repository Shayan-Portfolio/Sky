package engine.gameui;

public class LineLayoutEngine extends LayoutEngine {
    public enum Line {
        Vertical,
        Horizontal,
    }

    private Line line;

    public LineLayoutEngine(Line line) {
        this.line = line;
    }

    @Override
    public int getComputedWidth() {
        int width = 0;
        if(line == Line.Horizontal) {
            for(Widget child : widget.getWidgets()) {
                width += child.getRequiredWidth();
            }
        }
        else if(line == Line.Vertical) {
            for(Widget child : widget.getWidgets()) {
                width = Math.max(width, child.getRequiredWidth());
            }
        }


        return width;
    }
    @Override
    public int getComputedHeight() {
        int height = 0;
        if(line == Line.Horizontal) {
            for(Widget child : widget.getWidgets()) {
                height = Math.max(height, child.getRequiredHeight());
            }
        }
        else if(line == Line.Vertical) {
            for(Widget child : widget.getWidgets()) {
                height += child.getRequiredHeight();
            }
        }


        return height;
    }

    @Override
    public void updateChildren(UIPainter platform, int x, int y, int w, int h) {
        if(line == Line.Horizontal) {
            int dx = x, dy = y;
            for(Widget child : widget.getWidgets()) {
                int width = child.getRequiredWidth();
                child.update(platform, dx, dy, width, child.getRequiredHeight());
                dx += width;
            }
        }
        else if(line == Line.Vertical) {
            int dx = x, dy = y;
            for(Widget child : widget.getWidgets()) {
                int height = child.getRequiredHeight();
                child.update(platform, dx, dy, child.getRequiredWidth(), height);
                dy += height;
            }
        }




    }
}
