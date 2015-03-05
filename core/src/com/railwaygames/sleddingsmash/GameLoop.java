package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.railwaygames.sleddingsmash.screens.LevelSelectScreen;
import com.railwaygames.sleddingsmash.screens.MainMenuScreen;
import com.railwaygames.sleddingsmash.screens.PlayLevelScreen;
import com.railwaygames.sleddingsmash.screens.ScreenFeedback;


public class GameLoop extends Game {

    private MainMenuScreen mainMenuScreen;
    private LevelSelectScreen levelSelectScreen;
    private PlayLevelScreen playLevelScreen;

    @Override
    public void create() {
/*
         * Assume OpenGL ES 2.0 support has been validated by platform specific
		 * file.
		 */
        GL20 gl = Gdx.graphics.getGL20();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glEnable(GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.input.setCatchBackKey(true);

        AssetManager assetManager = new AssetManager();
        assetManager.load("data/images/menus.atlas", TextureAtlas.class);
        assetManager.finishLoading();

        UISkin skin = new UISkin();
        skin.initialize(assetManager);

        Resources resources = new Resources();
        resources.skin = skin;
        resources.assetManager = assetManager;
        resources.fontShader = createShader("data/shaders/font-vs.glsl", "data/shaders/font-fs.glsl");

        Fonts.dispose();

        mainMenuScreen = new MainMenuScreen(resources);
        levelSelectScreen = new LevelSelectScreen(resources);
        playLevelScreen = new PlayLevelScreen(resources);

        setScreen(mainMenuScreen);
    }

    public ShaderProgram createShader(String vertexShader, String fragmentShader) {
        ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexShader), Gdx.files.internal(fragmentShader));
        if (!shader.isCompiled() && !shader.getLog().isEmpty()) {
            throw new IllegalStateException("Shader compilation fail: " + shader.getLog());
        }

        return shader;
    }

    @Override
    public void render() {
        super.render();

        if (getScreen() != null && ((ScreenFeedback) getScreen()).getRenderResult() != null) {
            Object returnObject = ((ScreenFeedback) getScreen()).getRenderResult();

            if (getScreen() instanceof MainMenuScreen) {
                String menuChoice = (String) returnObject;
                if (menuChoice.equals("New")) {
                    changeScreen(levelSelectScreen);
                    return;
                }
            } else if (getScreen() instanceof LevelSelectScreen) {
                String level = (String) returnObject;
                playLevelScreen.setLevelToLoad(level);
                changeScreen(playLevelScreen);
                return;
            }

            throw new IllegalArgumentException("Could not handle return value of '" + returnObject + "' from " + getScreen().getClass().getName());
        }
    }

    private void changeScreen(ScreenFeedback nextScreen) {
        setScreen(nextScreen);
    }
}
