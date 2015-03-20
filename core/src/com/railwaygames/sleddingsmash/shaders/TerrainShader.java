package com.railwaygames.sleddingsmash.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.AmbientCubemap;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Map;

public class TerrainShader extends BaseShader {
    /**
     * Material attributes which are not required but always supported.
     */
    private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;
    /**
     * @deprecated Replaced by {@link Config#defaultCullFace} Set to 0 to disable culling
     */
    @Deprecated
    public static int defaultCullFace = GL20.GL_BACK;
    /**
     * @deprecated Replaced by {@link Config#defaultDepthFunc} Set to 0 to disable depth test
     */
    @Deprecated
    public static int defaultDepthFunc = GL20.GL_LEQUAL;
    protected static long implementedFlags = BlendingAttribute.Type | TextureAttribute.Diffuse | ColorAttribute.Diffuse
            | ColorAttribute.Specular | FloatAttribute.Shininess;
    private static String defaultVertexShader = null;
    private static String defaultFragmentShader = null;
    // Global uniforms
    public final int u_projTrans;
    public final int u_viewTrans;
    public final int u_projViewTrans;
    public final int u_cameraPosition;
    public final int u_cameraDirection;
    public final int u_cameraUp;
    public final int u_worldMin;
    public final int u_worldMax;
    public final int u_time;
    // Object uniforms
    public final int u_worldTrans;
    public final int u_viewWorldTrans;
    public final int u_projViewWorldTrans;
    public final int u_normalMatrix;
    // Material uniforms
    public final int u_opacity;
    public final int u_diffuseColor;
    public final int u_diffuseTexture;
    public final int u_diffuseUVTransform;
    public final int u_normalTexture;
    public final int u_normalUVTransform;
    public final int u_ambientTexture;
    public final int u_ambientUVTransform;
    public final int u_alphaTest;
    // Lighting uniforms
    protected final int u_ambientCubemap;
    protected final int u_dirLights0color = register(new Uniform("u_dirLights[0].color"));
    protected final int u_dirLights0direction = register(new Uniform("u_dirLights[0].direction"));
    protected final int u_dirLights1color = register(new Uniform("u_dirLights[1].color"));
    protected final int u_pointLights0color = register(new Uniform("u_pointLights[0].color"));
    protected final int u_pointLights0position = register(new Uniform("u_pointLights[0].position"));
    protected final int u_pointLights0intensity = register(new Uniform("u_pointLights[0].intensity"));
    protected final int u_pointLights1color = register(new Uniform("u_pointLights[1].color"));
    protected final int u_fogColor = register(new Uniform("u_fogColor"));
    protected final boolean lighting;
    protected final boolean fog;
    // FIXME Cache vertex attribute locations...
    protected final AmbientCubemap ambientCubemap = new AmbientCubemap();
    protected final DirectionalLight directionalLights[];
    protected final PointLight pointLights[];
    /**
     * The material attributes that this shader supports
     */
    protected final long materialMask;
    protected final Config config;
    private final Vector3 tmpV1 = new Vector3();
    protected int dirLightsLoc;
    protected int dirLightsColorOffset;
    protected int dirLightsDirectionOffset;
    protected int dirLightsSize;
    protected int pointLightsLoc;
    protected int pointLightsColorOffset;
    protected int pointLightsPositionOffset;
    protected int pointLightsIntensityOffset;
    protected int pointLightsSize;
    Material currentMaterial;
    /**
     * The renderable used to create this shader, invalid after the call to init
     */
    private Renderable renderable;
    private long vertexMask;
    private Matrix3 normalMatrix = new Matrix3();
    private Camera camera;
    private float time;
    private boolean lightsSet;

    public TerrainShader(final Renderable renderable) {
        this(renderable, new Config());
    }

    public TerrainShader(final Renderable renderable, final Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public TerrainShader(final Renderable renderable, final Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShader != null ? config.vertexShader : getDefaultVertexShader(),
                config.fragmentShader != null ? config.fragmentShader : getDefaultFragmentShader());
    }

    public TerrainShader(final Renderable renderable, final Config config, final String prefix, final String vertexShader,
                         final String fragmentShader) {
        this(renderable, config, new ShaderProgram(prefix + vertexShader, prefix + fragmentShader));
    }

    public TerrainShader(final Renderable renderable, final Config config, final ShaderProgram shaderProgram) {
        this.config = config;
        this.program = shaderProgram;
        this.lighting = renderable.environment != null;
        this.fog = lighting && renderable.environment.has(ColorAttribute.Fog);
        this.renderable = renderable;
        materialMask = renderable.material.getMask() | optionalAttributes;
        vertexMask = renderable.mesh.getVertexAttributes().getMask();

        this.directionalLights = new DirectionalLight[lighting && config.numDirectionalLights > 0 ? config.numDirectionalLights : 0];
        for (int i = 0; i < directionalLights.length; i++)
            directionalLights[i] = new DirectionalLight();
        this.pointLights = new PointLight[lighting && config.numPointLights > 0 ? config.numPointLights : 0];
        for (int i = 0; i < pointLights.length; i++)
            pointLights[i] = new PointLight();

        if (!config.ignoreUnimplemented && (implementedFlags & materialMask) != materialMask)
            throw new GdxRuntimeException("Some attributes not implemented yet (" + materialMask + ")");

        // Global uniforms
        u_projTrans = register(Inputs.projTrans, Setters.projTrans);
        u_viewTrans = register(Inputs.viewTrans, Setters.viewTrans);
        u_projViewTrans = register(Inputs.projViewTrans, Setters.projViewTrans);
        u_cameraPosition = register(Inputs.cameraPosition, Setters.cameraPosition);
        u_cameraDirection = register(Inputs.cameraDirection, Setters.cameraDirection);
        u_cameraUp = register(Inputs.cameraUp, Setters.cameraUp);
        u_worldMin = register(Inputs.worldMin, Setters.worldMin);
        u_worldMax = register(Inputs.worldMax, Setters.worldMax);
        u_time = register(new Uniform("u_time"));
        // Object uniforms
        u_worldTrans = register(Inputs.worldTrans, Setters.worldTrans);
        u_viewWorldTrans = register(Inputs.viewWorldTrans, Setters.viewWorldTrans);
        u_projViewWorldTrans = register(Inputs.projViewWorldTrans, Setters.projViewWorldTrans);
        u_normalMatrix = register(Inputs.normalMatrix, Setters.normalMatrix);

        u_opacity = register(Inputs.opacity);
        u_diffuseColor = register(Inputs.diffuseColor, Setters.diffuseColor);
        u_diffuseTexture = register(Inputs.diffuseTexture, Setters.diffuseTexture);
        u_diffuseUVTransform = register(Inputs.diffuseUVTransform, Setters.diffuseUVTransform);
        u_normalTexture = register(Inputs.normalTexture, Setters.normalTexture);
        u_normalUVTransform = register(Inputs.normalUVTransform, Setters.normalUVTransform);
        u_ambientTexture = register(Inputs.ambientTexture, Setters.ambientTexture);
        u_ambientUVTransform = register(Inputs.ambientUVTransform, Setters.ambientUVTransform);
        u_alphaTest = register(Inputs.alphaTest);

        u_ambientCubemap = lighting ? register(Inputs.ambientCube, new Setters.ACubemap(config.numDirectionalLights,
                config.numPointLights)) : -1;
    }

    public static String getDefaultVertexShader() {
        if (defaultVertexShader == null)
            defaultVertexShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl").readString();
        return defaultVertexShader;
    }

    public static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null)
            defaultFragmentShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl").readString();
        return defaultFragmentShader;
    }

    private static final boolean and(final long mask, final long flag) {
        return (mask & flag) == flag;
    }

    private static final boolean or(final long mask, final long flag) {
        return (mask & flag) != 0;
    }

    public static String createPrefix(final Renderable renderable, final Config config) {
        String prefix = "";
        final long mask = renderable.material.getMask();
        final long attributes = renderable.mesh.getVertexAttributes().getMask();
        if (and(attributes, VertexAttributes.Usage.Position)) prefix += "#define positionFlag\n";
        if (or(attributes, VertexAttributes.Usage.Color | VertexAttributes.Usage.ColorPacked))
            prefix += "#define colorFlag\n";
        if (and(attributes, VertexAttributes.Usage.Normal)) prefix += "#define normalFlag\n";
        if (and(attributes, VertexAttributes.Usage.Normal) || and(attributes, VertexAttributes.Usage.Tangent | VertexAttributes.Usage.BiNormal)) {
            if (renderable.environment != null) {
                prefix += "#define lightingFlag\n";
                prefix += "#define ambientCubemapFlag\n";
                prefix += "#define numDirectionalLights " + config.numDirectionalLights + "\n";
                prefix += "#define numPointLights " + config.numPointLights + "\n";
                if (renderable.environment.has(ColorAttribute.Fog)) {
                    prefix += "#define fogFlag\n";
                }
            }
        }
        final int n = renderable.mesh.getVertexAttributes().size();
        for (int i = 0; i < n; i++) {
            final VertexAttribute attr = renderable.mesh.getVertexAttributes().get(i);
            if (attr.usage == VertexAttributes.Usage.TextureCoordinates)
                prefix += "#define texCoord" + attr.unit + "Flag\n";
        }
        if ((mask & BlendingAttribute.Type) == BlendingAttribute.Type) {
            prefix += "#define " + BlendingAttribute.Alias + "Flag\n";
        }

        prefix += "#define " + TextureAttribute.DiffuseAlias + "Flag\n";
        prefix += "#define " + ColorAttribute.DiffuseAlias + "Flag\n";

        if ((mask & TextureAttribute.Normal) == TextureAttribute.Normal) {
            prefix += "#define " + TextureAttribute.NormalAlias + "Flag\n";
            prefix += "#define " + TextureAttribute.NormalAlias + "Coord texCoord0\n"; // FIXME implement UV mapping
        }

        if ((mask & TextureAttribute.Ambient) == TextureAttribute.Ambient) {
            prefix += "#define " + TextureAttribute.AmbientAlias + "Flag\n";
            prefix += "#define " + TextureAttribute.AmbientAlias + "Coord texCoord0\n"; // FIXME implement UV mapping
        }

        if ((mask & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest)
            prefix += "#define " + FloatAttribute.AlphaTestAlias + "Flag\n";
        return prefix;
    }

    @Override
    public void init() {
        final ShaderProgram program = this.program;
        this.program = null;
        init(program, renderable);
        renderable = null;

        dirLightsLoc = loc(u_dirLights0color);
        dirLightsColorOffset = loc(u_dirLights0color) - dirLightsLoc;
        dirLightsDirectionOffset = loc(u_dirLights0direction) - dirLightsLoc;
        dirLightsSize = loc(u_dirLights1color) - dirLightsLoc;
        if (dirLightsSize < 0) dirLightsSize = 0;

        pointLightsLoc = loc(u_pointLights0color);
        pointLightsColorOffset = loc(u_pointLights0color) - pointLightsLoc;
        pointLightsPositionOffset = loc(u_pointLights0position) - pointLightsLoc;
        pointLightsIntensityOffset = has(u_pointLights0intensity) ? loc(u_pointLights0intensity) - pointLightsLoc : -1;
        pointLightsSize = loc(u_pointLights1color) - pointLightsLoc;
        if (pointLightsSize < 0) pointLightsSize = 0;
    }

    @Override
    public boolean canRender(final Renderable renderable) {
        return (materialMask == (renderable.material.getMask() | optionalAttributes))
                && (vertexMask == renderable.mesh.getVertexAttributes().getMask()) && (renderable.environment != null) == lighting
                && ((renderable.environment != null && renderable.environment.has(ColorAttribute.Fog)) == fog);
    }

    @Override
    public int compareTo(Shader other) {
        if (other == null) return -1;
        if (other == this) return 0;
        return 0; // FIXME compare shaders on their impact on performance
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof TerrainShader) ? equals((TerrainShader) obj) : false;
    }

    public boolean equals(TerrainShader obj) {
        return (obj == this);
    }

    @Override
    public void begin(final Camera camera, final RenderContext context) {
        super.begin(camera, context);

        for (final DirectionalLight dirLight : directionalLights)
            dirLight.set(0, 0, 0, 0, -1, 0);
        for (final PointLight pointLight : pointLights)
            pointLight.set(0, 0, 0, 0, 0, 0, 0);
        lightsSet = false;

        if (has(u_time)) set(u_time, time += Gdx.graphics.getDeltaTime());
    }

    @Override
    public void render(final Renderable renderable) {
//        if (!renderable.material.has(BlendingAttribute.Type))
        context.setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        bindMaterial(renderable);
        if (lighting) bindLights(renderable);
        super.render(renderable);
    }

    @Override
    public void end() {
        currentMaterial = null;
        super.end();
    }

    protected void bindMaterial(final Renderable renderable) {
        if (currentMaterial == renderable.material) return;

        int cullFace = config.defaultCullFace == -1 ? defaultCullFace : config.defaultCullFace;
        int depthFunc = config.defaultDepthFunc == -1 ? defaultDepthFunc : config.defaultDepthFunc;
        float depthRangeNear = 0f;
        float depthRangeFar = 1f;
        boolean depthMask = true;

        currentMaterial = renderable.material;
        for (final Attribute attr : currentMaterial) {
            final long t = attr.type;
            if (BlendingAttribute.is(t)) {
                context.setBlending(true, ((BlendingAttribute) attr).sourceFunction, ((BlendingAttribute) attr).destFunction);
                set(u_opacity, ((BlendingAttribute) attr).opacity);
            } else if ((t & IntAttribute.CullFace) == IntAttribute.CullFace)
                cullFace = ((IntAttribute) attr).value;
            else if ((t & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest)
                set(u_alphaTest, ((FloatAttribute) attr).value);
            else if ((t & DepthTestAttribute.Type) == DepthTestAttribute.Type) {
                DepthTestAttribute dta = (DepthTestAttribute) attr;
                depthFunc = dta.depthFunc;
                depthRangeNear = dta.depthRangeNear;
                depthRangeFar = dta.depthRangeFar;
                depthMask = dta.depthMask;
            } else if (!config.ignoreUnimplemented)
                throw new GdxRuntimeException("Unknown material attribute: " + attr.toString());
        }

        context.setCullFace(cullFace);
        context.setDepthTest(depthFunc, depthRangeNear, depthRangeFar);
        context.setDepthMask(depthMask);
    }

    protected void bindLights(final Renderable renderable) {
        final Environment lights = renderable.environment;
        final Array<DirectionalLight> dirs = lights.directionalLights;
        final Array<PointLight> points = lights.pointLights;

        if (dirLightsLoc >= 0) {
            for (int i = 0; i < directionalLights.length; i++) {
                if (dirs == null || i >= dirs.size) {
                    if (lightsSet && directionalLights[i].color.r == 0f && directionalLights[i].color.g == 0f
                            && directionalLights[i].color.b == 0f) continue;
                    directionalLights[i].color.set(0, 0, 0, 1);
                } else if (lightsSet && directionalLights[i].equals(dirs.get(i)))
                    continue;
                else
                    directionalLights[i].set(dirs.get(i));

                int idx = dirLightsLoc + i * dirLightsSize;
                program.setUniformf(idx + dirLightsColorOffset, directionalLights[i].color.r, directionalLights[i].color.g,
                        directionalLights[i].color.b);
                program.setUniformf(idx + dirLightsDirectionOffset, directionalLights[i].direction);
                if (dirLightsSize <= 0) break;
            }
        }

        if (pointLightsLoc >= 0) {
            for (int i = 0; i < pointLights.length; i++) {
                if (points == null || i >= points.size) {
                    if (lightsSet && pointLights[i].intensity == 0f) continue;
                    pointLights[i].intensity = 0f;
                } else if (lightsSet && pointLights[i].equals(points.get(i)))
                    continue;
                else
                    pointLights[i].set(points.get(i));

                int idx = pointLightsLoc + i * pointLightsSize;
                program.setUniformf(idx + pointLightsColorOffset, pointLights[i].color.r * pointLights[i].intensity,
                        pointLights[i].color.g * pointLights[i].intensity, pointLights[i].color.b * pointLights[i].intensity);
                program.setUniformf(idx + pointLightsPositionOffset, pointLights[i].position);
                if (pointLightsIntensityOffset >= 0)
                    program.setUniformf(idx + pointLightsIntensityOffset, pointLights[i].intensity);
                if (pointLightsSize <= 0) break;
            }
        }

        if (lights.has(ColorAttribute.Fog)) {
            set(u_fogColor, ((ColorAttribute) lights.get(ColorAttribute.Fog)).color);
        }

        lightsSet = true;
    }

    @Override
    public void dispose() {
        program.dispose();
        super.dispose();
    }

    public int getDefaultCullFace() {
        return config.defaultCullFace == -1 ? defaultCullFace : config.defaultCullFace;
    }

    public void setDefaultCullFace(int cullFace) {
        config.defaultCullFace = cullFace;
    }

    public int getDefaultDepthFunc() {
        return config.defaultDepthFunc == -1 ? defaultDepthFunc : config.defaultDepthFunc;
    }

    public void setDefaultDepthFunc(int depthFunc) {
        config.defaultDepthFunc = depthFunc;
    }

    public static class Config {
        /**
         * The uber vertex shader to use, null to use the default vertex shader.
         */
        public String vertexShader = null;
        /**
         * The uber fragment shader to use, null to use the default fragment shader.
         */
        public String fragmentShader = null;
        /**
         * The number of directional lights to use
         */
        public int numDirectionalLights = 2;
        /**
         * The number of point lights to use
         */
        public int numPointLights = 5;
        /**
         * The number of spot lights to use
         */
        public int numSpotLights = 0;
        /** */
        public boolean ignoreUnimplemented = true;
        /**
         * Set to 0 to disable culling, -1 to inherit from {@link DefaultShader#defaultCullFace}
         */
        public int defaultCullFace = -1;
        /**
         * Set to 0 to disable depth test, -1 to inherit from {@link DefaultShader#defaultDepthFunc}
         */
        public int defaultDepthFunc = -1;

        public Config() {
        }

        public Config(final String vertexShader, final String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }
    }

    public static class Inputs {
        public final static Uniform projTrans = new Uniform("u_projTrans");
        public final static Uniform viewTrans = new Uniform("u_viewTrans");
        public final static Uniform projViewTrans = new Uniform("u_projViewTrans");
        public final static Uniform cameraPosition = new Uniform("u_cameraPosition");
        public final static Uniform cameraDirection = new Uniform("u_cameraDirection");
        public final static Uniform cameraUp = new Uniform("u_cameraUp");
        public final static Uniform worldMin = new Uniform("u_worldMin");
        public final static Uniform worldMax = new Uniform("u_worldMax");

        public final static Uniform worldTrans = new Uniform("u_worldTrans");
        public final static Uniform viewWorldTrans = new Uniform("u_viewWorldTrans");
        public final static Uniform projViewWorldTrans = new Uniform("u_projViewWorldTrans");
        public final static Uniform normalMatrix = new Uniform("u_normalMatrix");

        public final static Uniform opacity = new Uniform("u_opacity", BlendingAttribute.Type);
        public final static Uniform diffuseColor = new Uniform("u_diffuseColor", ColorAttribute.Diffuse);
        public final static Uniform diffuseTexture = new Uniform("u_diffuseTexture", TextureAttribute.Diffuse);
        public final static Uniform diffuseUVTransform = new Uniform("u_diffuseUVTransform", TextureAttribute.Diffuse);
        public final static Uniform normalTexture = new Uniform("u_normalTexture", TextureAttribute.Normal);
        public final static Uniform normalUVTransform = new Uniform("u_normalUVTransform", TextureAttribute.Normal);
        public final static Uniform ambientTexture = new Uniform("u_ambientTexture", TextureAttribute.Ambient);
        public final static Uniform ambientUVTransform = new Uniform("u_ambientUVTransform", TextureAttribute.Ambient);
        public final static Uniform alphaTest = new Uniform("u_alphaTest");

        public final static Uniform ambientCube = new Uniform("u_ambientCubemap");
        public final static Uniform dirLights = new Uniform("u_dirLights");
        public final static Uniform pointLights = new Uniform("u_pointLights");
    }

    public static class Setters {
        public final static Setter projTrans = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.projection);
            }
        };
        public final static Setter viewTrans = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.view);
            }
        };
        public final static Setter projViewTrans = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.combined);
            }
        };
        public final static Setter cameraPosition = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.position.x, shader.camera.position.y, shader.camera.position.z,
                        1.1881f / (shader.camera.far * shader.camera.far));
            }
        };
        public final static Setter cameraDirection = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.direction);
            }
        };
        public final static Setter cameraUp = new GlobalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.up);
            }
        };
        public final static Setter worldMin = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                Vector3 worldMin = (Vector3) ((Map<String, Object>) renderable.userData).get("u_worldMin");
                shader.set(inputID, worldMin);
            }
        };
        public final static Setter worldMax = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                Vector3 worldMax = (Vector3) ((Map<String, Object>) renderable.userData).get("u_worldMax");
                shader.set(inputID, worldMax);
            }
        };
        public final static Setter worldTrans = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, renderable.worldTransform);
            }
        };
        public final static Setter viewWorldTrans = new LocalSetter() {
            final Matrix4 temp = new Matrix4();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, temp.set(shader.camera.view).mul(renderable.worldTransform));
            }
        };
        public final static Setter projViewWorldTrans = new LocalSetter() {
            final Matrix4 temp = new Matrix4();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, temp.set(shader.camera.combined).mul(renderable.worldTransform));
            }
        };
        public final static Setter normalMatrix = new LocalSetter() {
            private final Matrix3 tmpM = new Matrix3();

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, tmpM.set(renderable.worldTransform).inv().transpose());
            }
        };
        public final static Setter diffuseColor = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((ColorAttribute) (combinedAttributes.get(ColorAttribute.Diffuse))).color);
            }
        };
        public final static Setter diffuseTexture = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (combinedAttributes
                        .get(TextureAttribute.Diffuse))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter diffuseUVTransform = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final TextureAttribute ta = (TextureAttribute) (combinedAttributes.get(TextureAttribute.Diffuse));
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public final static Setter normalTexture = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (combinedAttributes
                        .get(TextureAttribute.Normal))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter normalUVTransform = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final TextureAttribute ta = (TextureAttribute) (combinedAttributes.get(TextureAttribute.Normal));
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };
        public final static Setter ambientTexture = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final int unit = shader.context.textureBinder.bind(((TextureAttribute) (combinedAttributes
                        .get(TextureAttribute.Ambient))).textureDescription);
                shader.set(inputID, unit);
            }
        };
        public final static Setter ambientUVTransform = new LocalSetter() {
            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                final TextureAttribute ta = (TextureAttribute) (combinedAttributes.get(TextureAttribute.Ambient));
                shader.set(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV);
            }
        };

        public static class ACubemap extends LocalSetter {
            private final static float ones[] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
            private final static Vector3 tmpV1 = new Vector3();
            public final int dirLightsOffset;
            public final int pointLightsOffset;
            private final AmbientCubemap cacheAmbientCubemap = new AmbientCubemap();

            public ACubemap(final int dirLightsOffset, final int pointLightsOffset) {
                this.dirLightsOffset = dirLightsOffset;
                this.pointLightsOffset = pointLightsOffset;
            }

            @Override
            public void set(BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
                if (renderable.environment == null)
                    shader.program.setUniform3fv(shader.loc(inputID), ones, 0, ones.length);
                else {
                    renderable.worldTransform.getTranslation(tmpV1);
                    if (renderable.environment.has(ColorAttribute.AmbientLight))
                        cacheAmbientCubemap.set(((ColorAttribute) renderable.environment.get(ColorAttribute.AmbientLight)).color);

                    for (int i = dirLightsOffset; i < renderable.environment.directionalLights.size; i++)
                        cacheAmbientCubemap.add(renderable.environment.directionalLights.get(i).color,
                                renderable.environment.directionalLights.get(i).direction);

                    for (int i = pointLightsOffset; i < renderable.environment.pointLights.size; i++)
                        cacheAmbientCubemap.add(renderable.environment.pointLights.get(i).color,
                                renderable.environment.pointLights.get(i).position, tmpV1,
                                renderable.environment.pointLights.get(i).intensity);

                    cacheAmbientCubemap.clamp();

                    shader.program.setUniform3fv(shader.loc(inputID), cacheAmbientCubemap.data, 0, cacheAmbientCubemap.data.length);
                }
            }
        }
    }
}