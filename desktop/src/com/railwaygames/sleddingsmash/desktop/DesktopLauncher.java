package com.railwaygames.sleddingsmash.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.railwaygames.sleddingsmash.GameLoop;

public class DesktopLauncher {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 1000;
        config.height = 600;
        new LwjglApplication(new GameLoop(), config);
//        new LwjglApplication(new SleddingSmashEditor(), config);
    }
}
