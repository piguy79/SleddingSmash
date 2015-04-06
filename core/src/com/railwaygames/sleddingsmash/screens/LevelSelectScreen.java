package com.railwaygames.sleddingsmash.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.railwaygames.sleddingsmash.Constants;
import com.railwaygames.sleddingsmash.Resources;
import com.railwaygames.sleddingsmash.utils.WidgetUtils;
import com.railwaygames.sleddingsmash.widgets.ShaderLabel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelSelectScreen implements ScreenFeedback {

    private final Resources resources;
    private Stage stage;
    private String renderResult = null;
    private final List<ShaderLabel> levelLabels = new ArrayList<ShaderLabel>();
    private final List<Integer> levels = new ArrayList<Integer>();

    public LevelSelectScreen(Resources resources) {
        this.resources = resources;

        FileHandle fh = Gdx.files.internal("levels/stage_1");
        for (FileHandle file : fh.list()) {
            levels.add(Integer.valueOf(file.name()));
        }
        Collections.sort(levels);
    }

    @Override
    public void show() {
        stage = new Stage();

        for (int i = 0; i < levels.size(); ++i) {
            ShaderLabel label = new ShaderLabel(resources.fontShader, "Level " + (i + 1), resources.skin, Constants.UI.DEFAULT_FONT,
                    Color.WHITE);
            final int index = i;
            label.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    renderResult = "levels/stage_1/" + levels.get(index);
                }
            });
            levelLabels.add(label);
            stage.addActor(label);
        }

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
        float y = height * 0.9f;

        for (ShaderLabel label : levelLabels) {
            WidgetUtils.centerLabelOnPoint(label, centerX, y);
            y -= height * 0.12f;
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        levelLabels.clear();

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
