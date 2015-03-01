package com.railwaygames.sleddingsmash.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.railwaygames.sleddingsmash.Constants;
import com.railwaygames.sleddingsmash.Resources;
import com.railwaygames.sleddingsmash.utils.WidgetUtils;
import com.railwaygames.sleddingsmash.widgets.ShaderLabel;

public class MainMenuScreen implements ScreenFeedback {

    private Resources resources;
    private Stage stage;
    private ShaderLabel newLabel;
    private String renderResult = null;


    public MainMenuScreen(Resources resources) {
        this.resources = resources;
    }

    @Override
    public void show() {
        stage = new Stage();
        newLabel = new ShaderLabel(resources.fontShader, "New", resources.skin, Constants.UI.DEFAULT_FONT,
                Color.WHITE);
        newLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                renderResult = "New";
            }
        });
        stage.addActor(newLabel);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        float centerX = width * 0.5f;
        float y = height * 0.6f;

        WidgetUtils.centerLabelOnPoint(newLabel, centerX, y);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        stage.dispose();
        stage = null;

        renderResult = null;
    }

    @Override
    public void dispose() {

    }

    @Override
    public Object getRenderResult() {
        return renderResult;
    }
}
