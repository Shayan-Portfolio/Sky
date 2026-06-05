package engine.gameui;

import engine.Time;

public class FunctionGraph extends Widget {
    private int width, height;
    private Function function;
    private float scale;

    public FunctionGraph(int width, int height, float scale, Function function) {
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.function = function;
    }

    @Override
    public int getRequiredWidth() {
        return width;
    }

    @Override
    public int getRequiredHeight() {
        return height;
    }

    private float phase = 0;
    @Override
    public void update(UIPainter platform, int x, int y, int w, int h) {
        platform.drawRect(x, y, w, h, platform.getTheme().containerBackgroundColor);

        int steps = 30;

        float ws = (float) w / steps;
        float hs = (float) h / steps;

        float lx = 0, ly = 0;

        phase += Time.deltaTime * 10f;



        for(int i = 0; i < steps; i++) {
            if(i % 4 == 0) {
                platform.drawRect(x + (i * ws), y, 1, h, platform.getTheme().containerBackgroundColor);
                platform.drawRect(x, y + (i * hs), w, 1, platform.getTheme().containerBackgroundColor);
            }
        }

        for(int i = 0; i < steps; i++) {
            float nx = i * ws;
            float fofx = (function.f(nx + phase) / scale) * (h / 2f);



            platform.drawLine(x + lx, y + ly, x + nx, y + fofx + hs * (steps / 2f), platform.getTheme().buttonHoverColor);


            lx = nx;
            ly = fofx + hs * (steps / 2f);
        }




    }
}
