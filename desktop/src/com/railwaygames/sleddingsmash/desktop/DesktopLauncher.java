package com.railwaygames.sleddingsmash.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.railwaygames.sleddingsmash.SleddingSmashEditor;

public class DesktopLauncher {
    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = 1600;
        config.height = 900;
//       new LwjglApplication(new GameLoop(), config);
        new LwjglApplication(new SleddingSmashEditor(), config);
    }
}
