package com.railwaygames.sleddingsmash.shaders;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider;

public class TerrainShaderProvider extends BaseShaderProvider {
    public final TerrainShader.Config config;

    public TerrainShaderProvider(final TerrainShader.Config config) {
        this.config = (config == null) ? new TerrainShader.Config() : config;
    }

    public TerrainShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new TerrainShader.Config(vertexShader, fragmentShader));
    }

    public TerrainShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(vertexShader.readString(), fragmentShader.readString());
    }

    public TerrainShaderProvider() {
        this(null);
    }

    @Override
    protected Shader createShader(final Renderable renderable) {
        return new TerrainShader(renderable, config);
    }
}
