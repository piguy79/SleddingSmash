package com.railwaygames.sleddingsmash.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

public class ShaderButtonWithLabel extends Button {

    private ShaderProgram shader;
    private ShaderLabel label;

    public ShaderButtonWithLabel(ShaderProgram shader, String text, Skin skin, String buttonStyle, String textStyle, Color color) {
        super(skin, buttonStyle);
        this.shader = shader;

        label = new ShaderLabel(shader, text, skin, textStyle, color);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        addActor(label);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setShader(shader);
        super.draw(batch, parentAlpha);
        batch.setShader(null);
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        label.setBounds(0, 0, width, height);
    }
}
