package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.UBJsonReader;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.BumpyTerrainModifier;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.InterpolationChoice;
import com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator;
import com.railwaygames.sleddingsmash.levels.obstacles.StarObstacleGenerator;
import com.railwaygames.sleddingsmash.levels.obstacles.TreeObstacleGenerator;
import com.railwaygames.sleddingsmash.shaders.TerrainShaderProvider;
import com.railwaygames.sleddingsmash.utils.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.railwaygames.sleddingsmash.levels.modifiers.BumpyTerrainModifier.Hill;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS_START_RATIO;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.IMPACT_AMOUNT;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.IMPACT_AXIS;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.INTERPOLATION;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.MODIFICATION_TYPE;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.ANGLE;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.DENSITY;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.END_X;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.END_Z;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.HEIGHT_FROM_GROUND;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.MODEL;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.START_X;
import static com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator.START_Z;
import static com.railwaygames.sleddingsmash.utils.MathUtils.MinMax;

public class SleddingSmashEditor extends ApplicationAdapter {

    public Environment lights;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public ModelBatch terrainModelBatch;
    public Model model;
    public Map<String, Model> treeModelMap;
    public Model star;
    public Array<GameObject> instances;
    public Array<GameObject> terrainModelInstances;
    public CameraInputController camController;
    GameObject sphere;
    GameObject plane;
    btCollisionConfiguration collisionConfig;
    btDispatcher dispatcher;
    MyContactListener contactListener;
    btBroadphaseInterface broadphase;
    btDynamicsWorld dynamicsWorld;
    btConstraintSolver constraintSolver;
    List<GameObject.Constructor> constructors;
    private BitmapFont font;
    private Resources resources;
    private Stage stage;
    private Group leftMenus;
    private ScrollPane scrollPane;
    private Group rightMenus;
    private Map<String, Runnable> menuHandlerMap = new HashMap<String, Runnable>();
    private Level level = new Level();
    private Color textColor = Color.ORANGE;

    private String[] treeModels = new String[]{"tree_1"};
    private String[] starModels = new String[]{"star"};

    private String[] homeMenu = new String[]{"Add", "Reset", "Resize", "Camera", "Save"};

    @Override
    public void create() {
        Bullet.init();
        instances = new Array<GameObject>();
        terrainModelInstances = new Array<GameObject>();

        lights = new Environment();
        lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        constructors = new ArrayList<GameObject.Constructor>();
        modelBatch = new ModelBatch();
        terrainModelBatch = new ModelBatch(new TerrainShaderProvider(Gdx.files.internal("data/shaders/terrain.vertex.glsl"), Gdx.files.internal("data/shaders/terrain.fragment.glsl")));

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        stage = new Stage();

        AssetManager assetManager = new AssetManager();
        assetManager.load("data/images/menus.atlas", TextureAtlas.class);
        assetManager.finishLoading();

        TextureAtlas menusAtlas = assetManager.get("data/images/menus.atlas", TextureAtlas.class);

        resources = new Resources();

        resources.skin = new UISkin();
        resources.skin.initialize(assetManager);
        resources.skin.add("default", new Label.LabelStyle(font, textColor));

        TextureRegionDrawable trd = new TextureRegionDrawable(menusAtlas.findRegion("textFieldBg"));
        trd.setLeftWidth(20);
        trd.setRightWidth(20);
        TextureRegionDrawable cursor = new TextureRegionDrawable(menusAtlas.findRegion("cursor"));
        TextField.TextFieldStyle style = new TextField.TextFieldStyle(font, Color.BLACK, cursor, null, trd);
        resources.skin.add("default", style);
        resources.skin.add("default", new SelectBox.SelectBoxStyle(font, Color.BLACK, trd, new ScrollPane.ScrollPaneStyle(), new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(font, Color.BLUE, textColor, cursor)));

        treeModelMap = new HashMap<String, Model>();

        createPhysicsWorld();
        setupCamera();

        createTree();
        createStarModel();

        menuHandlerMap.put("New", createNewRunnable());
        menuHandlerMap.put("Add", createAddRunnable());
        menuHandlerMap.put("Reset", createResetRunnable());
        menuHandlerMap.put("Resize", createNewRunnable());
        menuHandlerMap.put("Camera", switchToCameraRunnable());
        menuHandlerMap.put("Save", saveLevelRunnable(null));
        menuHandlerMap.put("Load", loadLevelRunnable());
        menuHandlerMap.put("Transform", createSlopeModifierRunnable(ModifierType.TRANSFORM, null));
        menuHandlerMap.put("Scale", createSlopeModifierRunnable(ModifierType.SCALE, null));
        menuHandlerMap.put("Hill", createHillRunnable(null));
        menuHandlerMap.put("Trees", createObstaclesRunnable(ObstacleType.TREE, treeModels, null));
        menuHandlerMap.put("Star", createObstaclesRunnable(ObstacleType.STAR, starModels, null));

        Gdx.input.setInputProcessor(stage);

        float height = Gdx.graphics.getHeight();
        float width = Gdx.graphics.getWidth();

        leftMenus = new Group();
        leftMenus.setSize(width * 0.45f, 50);
        scrollPane = new ScrollPane(leftMenus);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, true);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setBounds(0.01f * width, 0, width * 0.45f, height);
        scrollPane.setColor(Color.RED);
        stage.addActor(scrollPane);

        rightMenus = new Group();
        rightMenus.setBounds(0.75f * width, 0, width * 0.25f, height);
        stage.addActor(rightMenus);

        showMenus(true, "New", "Load");
    }

    private Runnable switchToCameraRunnable() {
        return new Runnable() {
            public void run() {
                leftMenus.clear();
                rightMenus.clear();

                Gdx.input.setInputProcessor(camController);
            }
        };
    }

    private Runnable loadLevelRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                FileHandle fh = Gdx.files.local("levels/raw/");

                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();

                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                FileHandle[] files = fh.list();
                Array<String> fileNames = new Array<String>(files.length);
                for (FileHandle file : files) {
                    if (!file.isDirectory()) {
                        fileNames.add(file.name());
                    }
                }

                final SelectBox<String> selectBox = createLabelWithSelectBox(group, "File", height * 0.9f, width, fileNames);

                {
                    Label label = new Label("ok", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, height * 0.8f, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            FileHandle fileHandle = Gdx.files.internal("levels/raw/" + selectBox.getSelected());

                            Json json = new Json();
                            level = json.fromJson(Level.class, fileHandle);
                            if (applyModifiers(group)) {
                                group.remove();
                                showLeftMenus();
                                showMenus(true, homeMenu);
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, height * 0.8f, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, "New", "Load");
                        }
                    });
                }
            }
        };
    }

    private Runnable saveLevelRunnable(final String existingFileName) {
        return new Runnable() {
            @Override
            public void run() {
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();

                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                final TextField fileNameTextField;
                if (existingFileName == null) {
                    fileNameTextField = createLabelWithTextField(group, "File Name", height * 0.9f, width);
                } else {
                    fileNameTextField = null;
                }

                float y = height * 0.8f;
                {
                    String labelText = existingFileName == null ? "save" : "overwrite";
                    Label label = new Label(labelText, resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            Json json = new Json();
                            String levelAsString = json.prettyPrint(level);

                            String fileName = existingFileName != null ? existingFileName : fileNameTextField.getText();
                            if (!fileName.trim().isEmpty()) {
                                FileHandle fh = Gdx.files.local("levels/raw/" + fileName);
                                if (existingFileName == null && fh.exists()) {
                                    group.remove();
                                    saveLevelRunnable(fileName).run();
                                } else {
                                    fh.writeString(levelAsString, false);
                                    group.remove();
                                    showMenus(true, homeMenu);
                                    showLeftMenus();
                                }
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, homeMenu);
                            showLeftMenus();
                        }
                    });
                }
            }
        };
    }

    private void createTree() {
        UBJsonReader jsonReader = new UBJsonReader();
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        treeModelMap.put("tree_1", modelLoader.loadModel(Gdx.files.getFileHandle("data/tree_1.g3db", Files.FileType.Internal)));
    }

    private void createStarModel() {
        UBJsonReader jsonReader = new UBJsonReader();
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        star = modelLoader.loadModel(Gdx.files.getFileHandle("data/star.g3db", Files.FileType.Internal));
    }

    private void showMenus(boolean right, String... menus) {
        float space = Gdx.graphics.getHeight() * 0.04f;
        float height = Math.max(Gdx.graphics.getHeight(), (menus.length + 1) * space);

        if (right) {
            rightMenus.clear();
        } else {
            leftMenus.clear();
            leftMenus.setHeight(height);
            scrollPane.layout();
        }

        float y = height - space;
        for (final String menu : menus) {
            Label label = new Label(menu, resources.skin, "default");
            label.setColor(textColor);

            if (!right) {
                Button upButton = new Button(resources.skin, Constants.UI.UP_BUTTON);
                upButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        moveModifier(true, menu);
                    }
                });
                upButton.setBounds(0, y - 5, 15, 15);
                leftMenus.addActor(upButton);
                Button downButton = new Button(resources.skin, Constants.UI.DOWN_BUTTON);
                downButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        moveModifier(false, menu);
                    }
                });
                downButton.setBounds(20, y - 5, 15, 15);
                leftMenus.addActor(downButton);
            }

            BitmapFont.TextBounds bounds = label.getTextBounds();
            label.setBounds(40, y, bounds.width, bounds.height);
            label.addListener(new ClickListener() {
                public void clicked(InputEvent event, float x, float y) {
                    leftMenus.clear();
                    rightMenus.clear();

                    if (menuHandlerMap.containsKey(menu)) {
                        menuHandlerMap.get(menu).run();
                    } else {
                        edit(menu);
                    }
                }
            });

            y -= space;
            if (right) {
                rightMenus.addActor(label);
            } else {
                leftMenus.addActor(label);
            }
        }
    }

    private void moveModifier(boolean up, String menu) {
        String[] split = menu.split(":");

        int i = Integer.valueOf(split[1]);
        if (split[0].equals("OBSTACLE")) {
            Obstacle o = level.obstacles.get(i);
            level.obstacles.remove(i);
            if (up) {
                i--;
            } else {
                i++;
            }
            i = Math.min(level.obstacles.size(), Math.max(i, 0));
            level.obstacles.add(i, o);
        } else if (split[0].equals("MODIFIER")) {
            Modifier o = level.modifiers.get(i);
            level.modifiers.remove(i);
            if (up) {
                i--;
            } else {
                i++;
            }
            i = Math.min(level.modifiers.size(), Math.max(i, 0));
            level.modifiers.add(i, o);
        } else if (split[0].equals("HILL")) {
            Hill o = level.hills.get(i);
            level.hills.remove(i);
            if (up) {
                i--;
            } else {
                i++;
            }
            i = Math.min(level.modifiers.size(), Math.max(i, 0));
            level.hills.add(i, o);
        }

        applyModifiers(null);
        showMenus(true, homeMenu);
        showLeftMenus();
    }

    private void edit(String menu) {
        String[] split = menu.split(":");

        int pos = Integer.valueOf(split[1]);
        if (split[0].equals("OBSTACLE")) {
            Obstacle obstacle = level.obstacles.get(pos);
            createObstaclesRunnable(obstacle.type, obstacle.availableModels, obstacle).run();
        } else if (split[0].equals("MODIFIER")) {
            Modifier modifier = level.modifiers.get(pos);
            createSlopeModifierRunnable(modifier.type, modifier).run();
        } else {
            Hill hill = level.hills.get(pos);
            createHillRunnable(hill).run();
        }
    }

    private void createPlane(Level level) {
        TextureLoader.TextureParameter tp = new TextureLoader.TextureParameter();
        tp.magFilter = Texture.TextureFilter.Linear;
        tp.minFilter = Texture.TextureFilter.Linear;

        AssetManager assetManager = new AssetManager();
        assetManager.load("data/images/menus.atlas", TextureAtlas.class);
        assetManager.load("data/images/levels/finish_line.png", Texture.class, tp);
        assetManager.finishLoading();

        UISkin skin = new UISkin();
        skin.initialize(assetManager);

        Resources resources = new Resources();
        resources.skin = skin;
        resources.assetManager = assetManager;

        model = LevelBuilder.generate(level.width, level.length, resources);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(BumpyTerrainModifier.HILLS, level.bumps);
        new BumpyTerrainModifier().modify(model, params);

        params.put(BumpyTerrainModifier.HILLS, level.hills);
        new BumpyTerrainModifier().modify(model, params);
    }

    private void createPlane(float width, float length, int numberOfBumps) {
        level.width = width;
        level.length = length;

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(BumpyTerrainModifier.COUNT, numberOfBumps);

        level.bumps = new BumpyTerrainModifier().generate(width, length, params);
        createPlane(level);
    }

    private void finalizePlane() {
        LevelBuilder.calculateNormals(model);
        plane = new GameObject.Constructor(model, GameObject.GameObjectType.PLANE, new btBvhTriangleMeshShape(model.meshParts), 0f).construct();
        constructors.add(plane.constructor);
        plane.transform.setToTranslation(-level.width * 0.5f, 0, 0);

        plane.getBody().setWorldTransform(plane.transform);

        Map<String, MinMax> minMaxMap = getMinMaxMap(model);
        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put("u_worldMin", new Vector3(minMaxMap.get("x").min, minMaxMap.get("y").min, minMaxMap.get("z").min));
        userData.put("u_worldMax", new Vector3(minMaxMap.get("x").max, minMaxMap.get("y").max, minMaxMap.get("z").max));
        plane.userData = userData;

        terrainModelInstances.add(plane);
        dynamicsWorld.addRigidBody(plane.getBody());
    }

    private Map<String, MinMax> getMinMaxMap(Model model) {
        Mesh mesh = model.meshes.get(0);
        int vertexSize = mesh.getVertexSize() / 4;
        float[] vertices = new float[mesh.getNumVertices() * vertexSize];
        mesh.getVertices(vertices);
        return MathUtils.calculateAxisMinMax(vertices, vertexSize);
    }

    private void setupCamera() {
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0f, 80f, 80f);
        cam.lookAt(0, 0, -60);
        cam.near = 1f;
        cam.far = 50000f;
        cam.update();

        camController = new CameraInputController(cam) {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.input.setInputProcessor(stage);
                    showLeftMenus();
                    showMenus(true, homeMenu);
                    return false;
                }
                return super.keyDown(keycode);
            }
        };
        camController.translateUnits = 200f;
    }

    private void createPhysicsWorld() {
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
        contactListener = new MyContactListener();
    }

    @Override
    public void render() {
        final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

        dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.4f, 0.7f, 1.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        for (GameObject obj : instances) {
            obj.getBody().getWorldTransform(obj.transform);
        }

        camController.update();

        terrainModelBatch.begin(cam);
        terrainModelBatch.render(terrainModelInstances, lights);
        terrainModelBatch.end();

        modelBatch.begin(cam);
        modelBatch.render(instances, lights);
        modelBatch.end();

        stage.act(delta);
        stage.draw();
    }

    private void reset() {
        dispose();
        createPhysicsWorld();
        createPlane(level);
    }

    @Override
    public void dispose() {
        for (GameObject obj : instances)
            obj.dispose();
        instances.clear();

        for (GameObject obj : terrainModelInstances)
            obj.dispose();
        terrainModelInstances.clear();

        for (GameObject.Constructor constructor : constructors)
            constructor.dispose();
        constructors.clear();

        modelBatch.dispose();
        if (model != null) {
            model.dispose();
            model = null;
        }
        contactListener.dispose();
        dynamicsWorld.dispose();
        constraintSolver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    private Runnable createAddRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                showMenus(true, "Transform", "Scale", "Trees", "Star", "Hill");
                showMenus(false);
            }
        };
    }

    private TextField.TextFieldListener createTextListener(final Map<String, Object> params, final String field, final Class clazz) {
        return new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                try {
                    Object val = null;
                    if (clazz == String.class) {
                        val = new String(textField.getText());
                    } else if (clazz == Float.class && !textField.getText().equals("-")) {
                        val = Float.valueOf(textField.getText());
                    }
                    params.put(field, val);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
    }

    private boolean applyObstacles(Group group) {
        for (Obstacle obstacle : level.obstacles) {
            ObstacleGenerator generator;
            if (obstacle.type.equals(ObstacleType.TREE)) {
                generator = new TreeObstacleGenerator(treeModelMap.get(obstacle.getModelToUse()));
            } else {
                generator = new StarObstacleGenerator(star);
            }
            List<GameObject> gameObjects = new ArrayList<GameObject>();
            boolean needsPositions = obstacle.generatedPositions == null;

            if (needsPositions) {
                obstacle.generatedPositions = new ArrayList<Vector3>();
                gameObjects = generator.generateObstacles(plane.model, obstacle.params, new Vector3(0, 1, 0), new Vector3(-level.width * 0.5f, 0, 0));
            } else {
                gameObjects = generator.generateAt(obstacle.generatedPositions, obstacle.params, new Vector3(-level.width * 0.5f, 0, 0));
            }

            for (GameObject object : gameObjects) {
                if (needsPositions) {
                    obstacle.generatedPositions.add(object.position);
                }
                constructors.add(object.constructor);
                instances.add(object);
                dynamicsWorld.addRigidBody(object.getBody());
            }
        }

        return true;

    }

    private boolean applyModifiers(Group group) {
        reset();
        for (Modifier existingModifier : level.modifiers) {
            try {
                if (existingModifier.type == ModifierType.TRANSFORM) {
                    SlopeModifier slopeModifier = new SlopeModifier();
                    existingModifier.params.put(MODIFICATION_TYPE, "t");
                    slopeModifier.modify(model, existingModifier.params);
                } else if (existingModifier.type == ModifierType.SCALE) {
                    SlopeModifier slopeModifier = new SlopeModifier();
                    existingModifier.params.put(MODIFICATION_TYPE, "s");
                    slopeModifier.modify(model, existingModifier.params);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                showError(group, t.getMessage());
                return false;
            }
        }
        finalizePlane();
        applyObstacles(group);

        return true;
    }

    private void showError(Group group, String message) {
        Label errorLabel = new Label(message, resources.skin, "default");
        errorLabel.setColor(textColor);
        float height = Gdx.graphics.getHeight();
        float width = Gdx.graphics.getWidth();

        float y = height * 0.1f;

        errorLabel.setBounds(width * 0.05f, y, width, y);
        group.addActor(errorLabel);
    }

    private void setText(TextField textField, String value) {
        if (value == null) {
            return;
        }
        textField.setText(value);
    }

    private void setText(TextField textField, Float value) {
        if (value == null) {
            return;
        }
        textField.setText(value.toString());
    }

    private Runnable createObstaclesRunnable(final ObstacleType type, final String[] models, final Obstacle obstacleToEdit) {
        return new Runnable() {
            @Override
            public void run() {
                final Obstacle obstacle;
                if (obstacleToEdit != null) {
                    obstacle = obstacleToEdit;
                } else {
                    obstacle = new Obstacle(type, models);
                    level.obstacles.add(obstacle);
                }
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                float y = height * 0.9f;
                final TextField startingXPercent = createLabelWithTextField(group, "x start (0.0 - 1.0)", y, width);
                startingXPercent.setTextFieldListener(createTextListener(obstacle.params, START_X, Float.class));
                setText(startingXPercent, (Float) obstacle.params.get(START_X));


                y -= height * 0.07f;
                final TextField endingXPercent = createLabelWithTextField(group, "x end (0.0 - 1.0)", y, width);
                endingXPercent.setTextFieldListener(createTextListener(obstacle.params, END_X, Float.class));
                setText(endingXPercent, (Float) obstacle.params.get(END_X));

                y -= height * 0.07f;
                final TextField startingZPercent = createLabelWithTextField(group, "z start (0.0 - 1.0)", y, width);
                startingZPercent.setTextFieldListener(createTextListener(obstacle.params, START_Z, Float.class));
                setText(startingZPercent, (Float) obstacle.params.get(START_Z));

                y -= height * 0.07f;
                final TextField endingZPercent = createLabelWithTextField(group, "z end (0.0 - 1.0)", y, width);
                endingZPercent.setTextFieldListener(createTextListener(obstacle.params, END_Z, Float.class));
                setText(endingZPercent, (Float) obstacle.params.get(END_Z));

                y -= height * 0.07f;
                final TextField density = createLabelWithTextField(group, "density", y, width);
                density.setTextFieldListener(createTextListener(obstacle.params, DENSITY, Float.class));
                setText(density, (Float) obstacle.params.get(DENSITY));

                y -= height * 0.07f;
                final TextField angle = createLabelWithTextField(group, "angle (0 - 360)", y, width);
                angle.setTextFieldListener(createTextListener(obstacle.params, ANGLE, Float.class));
                setText(angle, (Float) obstacle.params.get(ANGLE));

                if (obstacle.type.equals(ObstacleType.STAR)) {
                    y -= height * 0.07f;
                    final TextField heightFromGround = createLabelWithTextField(group, "distanceFromGround", y, width);
                    heightFromGround.setTextFieldListener(createTextListener(obstacle.params, HEIGHT_FROM_GROUND, Float.class));
                    setText(heightFromGround, (Float) obstacle.params.get(HEIGHT_FROM_GROUND));
                }


                y -= height * 0.07f;
                Array<String> choices = new Array<String>();
                for (String choice : models) {
                    choices.add(choice);
                }

                obstacle.params.put(MODEL, choices.get(0));
                final SelectBox<String> modelSelectBox = createLabelWithSelectBox(group, "Model", y, width, choices);
                modelSelectBox.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        obstacle.params.put(MODEL, modelSelectBox.getSelected());
                    }
                });
                if (obstacle.params.containsKey(MODEL)) {
                    modelSelectBox.setSelected((String) obstacle.params.get(MODEL));
                }

                y -= height * 0.07f;

                {
                    Label label = new Label("save", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            obstacle.generatedPositions = null;
                            if (applyModifiers(group)) {
                                group.remove();
                                showMenus(true, homeMenu);
                                showLeftMenus();
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, homeMenu);
                            showLeftMenus();
                        }
                    });
                }

                y -= height * 0.07f;

                if (obstacleToEdit != null) {
                    {
                        Label label = new Label("delete", resources.skin, "default");
                        label.setColor(textColor);
                        BitmapFont.TextBounds bounds = label.getTextBounds();
                        label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                        group.addActor(label);

                        label.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                group.remove();
                                level.obstacles.remove(obstacleToEdit);
                                applyModifiers(group);
                                showMenus(true, homeMenu);
                                showLeftMenus();
                            }
                        });
                    }
                }
            }
        };
    }

    private Runnable createHillRunnable(final Hill hill) {
        return new Runnable() {
            @Override
            public void run() {
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                final Map<String, Object> params = new HashMap<String, Object>();
                if (hill != null) {
                    params.put("xRatio", hill.xRatio);
                    params.put("zRatio", hill.zRatio);
                    params.put("xRadius", hill.xRadius);
                    params.put("zRadius", hill.zRadius);
                    params.put("height", hill.height);
                }

                float y = height * 0.9f;
                final TextField xTextField = createLabelWithTextField(group, "x (0 - 1)", y, width);
                xTextField.setTextFieldListener(createTextListener(params, "xRatio", Float.class));
                if (hill != null) {
                    xTextField.setText(Float.toString(hill.xRatio));
                }

                y -= height * 0.07f;
                final TextField zTextField = createLabelWithTextField(group, "z (0 - 1)", y, width);
                zTextField.setTextFieldListener(createTextListener(params, "zRatio", Float.class));
                if (hill != null) {
                    zTextField.setText(Float.toString(hill.zRatio));
                }

                y -= height * 0.07f;
                final TextField xRadiusTextField = createLabelWithTextField(group, "x radius", y, width);
                xRadiusTextField.setTextFieldListener(createTextListener(params, "xRadius", Float.class));
                if (hill != null) {
                    xRadiusTextField.setText(Float.toString(hill.xRadius));
                }

                y -= height * 0.07f;
                final TextField zRadiusTextField = createLabelWithTextField(group, "z radius", y, width);
                zRadiusTextField.setTextFieldListener(createTextListener(params, "zRadius", Float.class));
                if (hill != null) {
                    zRadiusTextField.setText(Float.toString(hill.zRadius));
                }

                y -= height * 0.07f;
                final TextField heightTextField = createLabelWithTextField(group, "height", y, width);
                heightTextField.setTextFieldListener(createTextListener(params, "height", Float.class));
                if (hill != null) {
                    heightTextField.setText(Float.toString(hill.height));
                }

                y -= height * 0.07f;

                {
                    Label label = new Label("save", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            Map<String, MinMax> minMaxMap = getMinMaxMap(model);
                            float xPos = (minMaxMap.get("x").max - minMaxMap.get("x").min) * (Float) params.get("xRatio");
                            float zPos = (minMaxMap.get("z").max - minMaxMap.get("z").min) * (Float) params.get("zRatio");

                            Hill newHill = new Hill(xPos, -zPos,
                                    (Float) params.get("xRatio"), (Float) params.get("zRatio"),
                                    (Float) params.get("xRadius"), (Float) params.get("zRadius"),
                                    (Float) params.get("height"));
                            if (hill != null) {
                                int index = level.hills.indexOf(hill);
                                level.hills.set(index, newHill);
                            } else {
                                level.hills.add(newHill);
                            }

                            if (applyModifiers(group)) {
                                group.remove();
                                showMenus(true, homeMenu);
                                showLeftMenus();
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, homeMenu);
                            showLeftMenus();
                        }
                    });
                }
            }
        };
    }

    private Runnable createSlopeModifierRunnable(final ModifierType type, final Modifier modToEdit) {
        return new Runnable() {
            @Override
            public void run() {
                final Modifier modifier;
                if (modToEdit != null) {
                    modifier = modToEdit;
                } else {
                    modifier = new Modifier(type);
                    level.modifiers.add(modifier);
                }
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                float y = height * 0.9f;
                final TextField evalAxisTextField = createLabelWithTextField(group, "eval axis", y, width);
                evalAxisTextField.setTextFieldListener(createTextListener(modifier.params, EVAL_AXIS, String.class));
                setText(evalAxisTextField, (String) modifier.params.get(EVAL_AXIS));

                y -= height * 0.07f;
                final TextField impactAxisTextField = createLabelWithTextField(group, "impact axis", y, width);
                impactAxisTextField.setTextFieldListener(createTextListener(modifier.params, IMPACT_AXIS, String.class));
                setText(impactAxisTextField, (String) modifier.params.get(IMPACT_AXIS));

                y -= height * 0.07f;
                final TextField startTextField = createLabelWithTextField(group, "start (0.0 - 1.0)", y, width);
                startTextField.setTextFieldListener(createTextListener(modifier.params, EVAL_AXIS_START_RATIO, Float.class));
                setText(startTextField, (Float) modifier.params.get(EVAL_AXIS_START_RATIO));

                y -= height * 0.07f;
                final TextField durationTextField = createLabelWithTextField(group, "duration (0.0 - 1.0)", y, width);
                durationTextField.setTextFieldListener(createTextListener(modifier.params, EVAL_AXIS_INTERPOLATION_DURATION, Float.class));
                setText(durationTextField, (Float) modifier.params.get(EVAL_AXIS_INTERPOLATION_DURATION));

                y -= height * 0.07f;
                final TextField impactAmountTextField = createLabelWithTextField(group, "impact amount", y, width);
                impactAmountTextField.setTextFieldListener(createTextListener(modifier.params, IMPACT_AMOUNT, Float.class));
                setText(impactAmountTextField, (Float) modifier.params.get(IMPACT_AMOUNT));

                y -= height * 0.07f;
                Array<InterpolationChoice> choices = new Array<InterpolationChoice>();
                for (InterpolationChoice choice : InterpolationChoice.values()) {
                    choices.add(choice);
                }

                final SelectBox<InterpolationChoice> interpolationSelectBox = createLabelWithSelectBox(group, "interpolation", y, width, choices);
                interpolationSelectBox.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        modifier.params.put(INTERPOLATION, interpolationSelectBox.getSelected());
                    }
                });
                if (modifier.params.containsKey(INTERPOLATION)) {
                    interpolationSelectBox.setSelected((InterpolationChoice) modifier.params.get(INTERPOLATION));
                }

                y -= height * 0.07f;

                {
                    Label label = new Label("save", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            clearObstaclePositions();
                            if (applyModifiers(group)) {
                                group.remove();
                                showMenus(true, homeMenu);
                                showLeftMenus();
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (modToEdit == null) {
                                level.modifiers.remove(modifier);
                            }
                            group.remove();
                            showMenus(true, homeMenu);
                            showLeftMenus();
                        }
                    });
                }

                y -= height * 0.07f;

                if (modToEdit != null) {
                    {
                        Label label = new Label("delete", resources.skin, "default");
                        label.setColor(textColor);
                        BitmapFont.TextBounds bounds = label.getTextBounds();
                        label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                        group.addActor(label);

                        label.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                clearObstaclePositions();
                                group.remove();
                                level.modifiers.remove(modToEdit);
                                applyModifiers(group);
                                showMenus(true, homeMenu);
                                showLeftMenus();
                            }
                        });
                    }
                }
            }
        };
    }

    private void clearObstaclePositions() {
        if (level != null) {
            for (Obstacle obstacle : level.obstacles) {
                obstacle.generatedPositions = null;
            }
        }
    }

    private Runnable createResetRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                float y = height * 0.9f;
                {
                    Label label = new Label("Are you sure?", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);
                }
                y -= height * 0.1f;
                {
                    Label label = new Label("ok", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            model.dispose();
                            model = null;
                            instances.clear();
                            createPhysicsWorld();
                            setupCamera();
                            level.obstacles.clear();
                            level.modifiers.clear();

                            group.remove();
                            showMenus(true, "New", "Load");
                        }
                    });
                }
                {
                    Label label = new Label("cancel", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, homeMenu);
                            showLeftMenus();
                        }
                    });
                }
            }
        };
    }

    private void showLeftMenus() {
        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < level.modifiers.size(); ++i) {
            strings.add("MODIFIER:" + i + ": " + level.modifiers.get(i).toString());
        }

        for (int i = 0; i < level.obstacles.size(); i++) {
            strings.add("OBSTACLE:" + i + ":" + level.obstacles.get(i).toString());
        }

        for (int i = 0; i < level.hills.size(); i++) {
            strings.add("HILL:" + i + ":" + level.hills.get(i).toString());
        }

        showMenus(false, strings.toArray(new String[strings.size()]));
    }

    private Runnable createNewRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                stage.addActor(group);

                float y = height * 0.9f;
                final TextField widthTextField = createLabelWithTextField(group, "width", y, width);
                if (level.width > 0.0f) {
                    widthTextField.setText(Integer.toString((int) level.width));
                }

                y -= height * 0.1f;
                final TextField lengthTextField = createLabelWithTextField(group, "length", y, width);
                if (level.length > 0.0f) {
                    lengthTextField.setText(Integer.toString((int) level.length));
                }

                y -= height * 0.1f;
                final TextField numberOfBumps = createLabelWithTextField(group, "bumps", y, width);
                numberOfBumps.setText(Integer.toString((int) level.bumps.size()));

                y -= height * 0.1f;
                {
                    Label label = new Label("ok", resources.skin, "default");
                    label.setColor(textColor);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            dispose();
                            createPlane(Integer.valueOf(widthTextField.getText()), Integer.valueOf(lengthTextField.getText()), Integer.valueOf(numberOfBumps.getText()));
                            for (Obstacle o : level.obstacles) {
                                o.generatedPositions = null;
                            }
                            applyModifiers(group);
                            group.remove();
                            showMenus(true, homeMenu);
                        }
                    });
                }
            }
        };
    }

    private TextField createLabelWithTextField(Group group, String labelText, float y, float width) {
        Label label = new Label(labelText, resources.skin, "default");
        label.setColor(textColor);
        BitmapFont.TextBounds bounds = label.getTextBounds();
        label.setBounds(width * 0.48f - bounds.width, y, bounds.width, bounds.height);
        group.addActor(label);

        TextField textField = new TextField("", resources.skin, "default");
        textField.setBounds(width * 0.5f, y - 5, width * 0.2f, bounds.height + 10);
        group.addActor(textField);

        return textField;
    }

    private <T> SelectBox<T> createLabelWithSelectBox(Group group, String labelText, float y, float width, Array<T> items) {
        Label label = new Label(labelText, resources.skin, "default");
        label.setColor(textColor);
        BitmapFont.TextBounds bounds = label.getTextBounds();
        label.setBounds(width * 0.48f - bounds.width, y, bounds.width, bounds.height);
        group.addActor(label);

        SelectBox<T> selectBox = new SelectBox<T>(resources.skin, "default");
        selectBox.setItems(items);
        selectBox.setBounds(width * 0.5f, y - 5, width * 0.2f, bounds.height + 10);
        group.addActor(selectBox);

        return selectBox;
    }

    public enum ModifierType {
        TRANSFORM, SCALE
    }

    public enum ObstacleType {
        TREE, STAR
    }

    public static class Level {
        public List<Modifier> modifiers = new ArrayList<Modifier>();
        public List<Obstacle> obstacles = new ArrayList<Obstacle>();
        public List<Hill> hills = new ArrayList<Hill>();
        public List<Hill> bumps = new ArrayList<Hill>();
        public float width = 0.0f;
        public float length = 0.0f;
    }

    public static class Modifier {
        public ModifierType type;
        public Map<String, Object> params = new HashMap<String, Object>();

        public Modifier() {
        }

        public Modifier(ModifierType type) {
            this.type = type;
        }

        public void setType(ModifierType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            if (type == ModifierType.SCALE || type == ModifierType.TRANSFORM) {
                return type + " (" + params.get(EVAL_AXIS_START_RATIO) + params.get(EVAL_AXIS) + " for " + params.get(EVAL_AXIS_INTERPOLATION_DURATION) + ") -> " + params.get(IMPACT_AXIS) + params.get(IMPACT_AMOUNT);
            }
            return type.toString();
        }
    }

    public static class Obstacle {
        public Map<String, Object> params = new HashMap<String, Object>();
        public List<Vector3> generatedPositions;
        public String[] availableModels;

        public ObstacleType type;

        public Obstacle() {
        }

        public Obstacle(ObstacleType type, String[] availableTypes) {
            this.type = type;
            this.availableModels = availableTypes;
        }

        public String getModelToUse() {
            return (String) params.get(MODEL);
        }

        @Override
        public String toString() {
            return type + " x(" + params.get(START_X) + ":" + params.get(END_X) + ")z(" + params.get(START_Z) + ", " + params.get(END_Z) + ")" + params.get(DENSITY);
        }
    }


    private abstract class MenuHandler {
        public String text;

        public MenuHandler(String text) {
            this.text = text;
        }

        public abstract void run();
    }

    class MyContactListener extends ContactListener {
        @Override
        public boolean onContactAdded(int userValue0, int partId0, int index0, int userValue1, int partId1, int index1) {
            return true;
        }
    }
}