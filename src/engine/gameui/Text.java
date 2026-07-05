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
    public void update(UIPainter platform, int x, int y, int w, int h) {


        platform.drawString(x + (2 * padding), y + (2 * padding), value.string.toString(), font, null, platform.getTheme().textColor);
        updateChildren(platform, x + padding, y + padding, w - padding * 2, h - padding * 2);
    }
}
