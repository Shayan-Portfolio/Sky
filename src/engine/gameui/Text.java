package engine.gameui;

import engine.graphics.text.MsdfFont;

;

public class Text extends Widget {
    private TextValue value;
    private MsdfFont font;

    public Text(TextValue value, MsdfFont font) {
        this.value = value;
        this.font = font;
    }

    @Override
    public int getRequiredWidth() {
        return (int) font.getStringWidth(value.string.toString()) + (4 * padding);
    }

    public TextValue getText() {
        return value;
    }

    @Override
    public int getRequiredHeight() {
        return (int) font.getStringHeight(value.string.toString()) + (4 * padding);
    }

    @Override
    public void update(Paint paint, int x, int y, int w, int h) {


        paint.drawString(x + (2 * padding), y + (2 * padding), value.string.toString(), font, null, paint.getTheme().textColor);
        updateChildren(paint, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
