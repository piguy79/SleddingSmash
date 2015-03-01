package com.railwaygames.sleddingsmash.utils;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class WidgetUtils {

    /**
     * Set the bounds of a label to be centered on (x,y) by calculating the text bounds.
     */
    public static void centerLabelOnPoint(Label label, float x, float y) {
        BitmapFont.TextBounds bounds = label.getTextBounds();
        label.setBounds(x - bounds.width * 0.5f, y, bounds.width, bounds.height);
    }
}
