package com.railwaygames.sleddingsmash.screens;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.UBJsonReader;
import com.railwaygames.sleddingsmash.Constants;
import com.railwaygames.sleddingsmash.Resources;
import com.railwaygames.sleddingsmash.SleddingSmashEditor;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.BumpyTerrainModifier;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;
import com.railwaygames.sleddingsmash.levels.obstacles.ObstacleGenerator;
import com.railwaygames.sleddingsmash.levels.obstacles.StarObstacleGenerator;
import com.railwaygames.sleddingsmash.levels.obstacles.TreeObstacleGenerator;
import com.railwaygames.sleddingsmash.overlay.DialogOverlay;
import com.railwaygames.sleddingsmash.shaders.TerrainShaderProvider;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;
import com.railwaygames.sleddingsmash.utils.WidgetUtils;
import com.railwaygames.sleddingsmash.widgets.ShaderButtonWithLabel;
import com.railwaygames.sleddingsmash.widgets.ShaderLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.railwaygames.sleddingsmash.Constants.CharacterState.SLEEP;
import static com.railwaygames.sleddingsmash.Constants.CharacterState.VICTORY;
import static com.railwaygames.sleddingsmash.SleddingSmashEditor.Level;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.MODIFICATION_TYPE;
import static com.railwaygames.sleddingsmash.utils.MathUtils.MinMax;

public class PlayLevelScreen implements ScreenFeedback {

    private final Resources resources;
    private String levelToLoad;
    private String renderResult = null;
    private GameState gs;
    private Hud hud;

    public PlayLevelScreen(Resources resources) {
        this.resources = resources;
        Bullet.init();
    }

    @Override
    public Object getRenderResult() {
        return renderResult;
    }

    public void setLevelToLoad(String path) {
        this.levelToLoad = path;
    }

    @Override
    public void show() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        FileHandle fileHandle = Gdx.files.internal(levelToLoad);

        Json json = new Json();
        Level level = json.fromJson(Level.class, fileHandle);

        gs = new GameState();
        gs.buildLevel(level);

        hud = new Hud(gs);
    }

    @Override
    public void render(float delta) {
        gs.render();

        if (gs.state != null) {
            if (gs.state.equals(SLEEP) || gs.state.equals(VICTORY)) {
                hud.showEndGame(gs.state);
                gs.state = null;
                gs.pause = true;
            }
        }
        hud.render();
    }

    @Override
    public void resize(int width, int height) {
        hud.resize(width, height);

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        gs.dispose();
        gs = null;

        hud.dispose();
        hud = null;

        renderResult = null;
    }

    @Override
    public void dispose() {

    }

    private class GameState {
        private static final float MASS_OF_SLED = 10;
        private static final float MASS_OF_CHARACTER = 200;
        private static final float PHYSICS_SCALE_FACTOR = 1f;
        private static final float sideMove = 10f;
        private static final float forwardMove = 0.4f;
        private static final float rotation = 0.5f;
        private static final float LINEAR_SLEEP = 10;
        private static final float ANGULAR_SLEEP = 10;
        private final Vector3 GRAVITY_VEC = new Vector3(0, -10 * 4, 0);

        public Environment lights;
        public PerspectiveCamera cam;
        public ModelBatch modelBatch;
        public ModelBatch terrainModelBatch;
        public Model model;
        public Map<String, Model> treeModels;
        public Model star;
        public CameraInputController camController;
        public Array<GameObject> instances = new Array<GameObject>();
        public Array<GameObject> terrainModelInstances = new Array<GameObject>();
        public Array<ModelInstance> modelInstances = new Array<ModelInstance>();
        private List<GameObject.Constructor> constructors = new ArrayList<GameObject.Constructor>();
        private GameObject sphere;
        private GameObject plane;
        private Map<String, MinMax> minMaxMap;
        private btCollisionConfiguration collisionConfig;
        private btDispatcher dispatcher;
        private SSContactListener contactListener;
        private btBroadphaseInterface broadphase;
        private btDynamicsWorld dynamicsWorld;
        private btConstraintSolver constraintSolver;
        private Level level;
        private boolean accelerate = false;
        private boolean decelerate = false;
        private boolean pause = false;
        private String state = null;
        private DebugDrawer debugDrawer;
        private btCollisionWorld collisionWorld;
        private boolean pushed = false;
        private Vector3 sphereStartPosition;
        int starsCollected = 0;

        public void buildLevel(Level level) {
            this.level = level;
            lights = new Environment();
            lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
            lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
            lights.set(new ColorAttribute(ColorAttribute.Fog, 0.8f, 0.8f, 0.9f, 1f));

            modelBatch = new ModelBatch();
            terrainModelBatch = new ModelBatch(new TerrainShaderProvider(Gdx.files.internal("data/shaders/terrain.vertex.glsl"), Gdx.files.internal("data/shaders/terrain.fragment.glsl")));

            createPhysicsWorld();
            setupCamera();
            createPlane(level.width, level.length);
            createTree();
            createStarModel();

            for (SleddingSmashEditor.Modifier existingModifier : level.modifiers) {
                try {
                    if (existingModifier.type == SleddingSmashEditor.ModifierType.TRANSFORM) {
                        SlopeModifier slopeModifier = new SlopeModifier();
                        existingModifier.params.put(MODIFICATION_TYPE, "t");
                        slopeModifier.modify(model, existingModifier.params);
                    } else if (existingModifier.type == SleddingSmashEditor.ModifierType.SCALE) {
                        SlopeModifier slopeModifier = new SlopeModifier();
                        existingModifier.params.put(MODIFICATION_TYPE, "s");
                        slopeModifier.modify(model, existingModifier.params);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            for (SleddingSmashEditor.Obstacle obstacle : level.obstacles) {
                ObstacleGenerator generator;
                if(obstacle.type.equals(SleddingSmashEditor.ObstacleType.TREE)){
                    generator = new TreeObstacleGenerator(treeModels.get(obstacle.getModelToUse()));
                }else{
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
                    // DEBUG
                    if(object.gameObjectType.equals(GameObject.GameObjectType.STAR)) {
                        collisionWorld.addCollisionObject(object.getBody());
                        constructors.add(object.constructor);
                        instances.add(object);

                    }else{
                        constructors.add(object.constructor);
                        instances.add(object);
                        dynamicsWorld.addCollisionObject(object.getBody());
                    }

                }
            }

            List<Model> sides = LevelBuilder.createSides(model);
            for (Model side : sides) {
                LevelBuilder.calculateNormals(side);
                ModelInstance instance = new ModelInstance(side);
                instance.transform.setToTranslation(-level.width * 0.5f, 0, 0);
                modelInstances.add(instance);
            }

            Model bg = LevelBuilder.createBackground();
            ModelInstance bgInstance = new ModelInstance(bg);
            bgInstance.transform.idt();
            bgInstance.transform.setToTranslation(0.0f, 550.0f, -10000.0f);
            bgInstance.transform.scale(2100.0f, 750.0f, 1.0f);
            modelInstances.add(bgInstance);

            finalizePlane();
            createBall();
        }

        private void createPlane(float width, float length) {
            this.level.width = width;
            this.level.length = length;

            model = LevelBuilder.generate(width, length, resources);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(BumpyTerrainModifier.COUNT, 300);
            new BumpyTerrainModifier().modify(model, params);
        }

        private void createBall() {
            ModelBuilder mb = new ModelBuilder();
            mb.begin();
            mb.node().id = "sphere";
            mb.part("sphere", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED)))
                    .sphere(4f, 4f, 4f, 10, 10);
            Model model = mb.end();

            sphere = new GameObject.Constructor(model, GameObject.GameObjectType.CHARACTER, new btSphereShape(2f), MASS_OF_SLED).construct();
            constructors.add(sphere.constructor);

            sphere.getBody().setFriction(1);
            sphere.getBody().setSleepingThresholds(LINEAR_SLEEP, ANGULAR_SLEEP);

            sphereStartPosition = findStartPos();
            sphere.transform.setToTranslation(sphereStartPosition);
            sphere.getBody().setWorldTransform(sphere.transform);
            sphere.getBody().setContactCallbackFlag(Constants.CollisionsFlag.SPHERE_FLAG);
            sphere.getBody().setContactCallbackFilter(Constants.CollisionsFlag.SPHERE_FLAG);

            instances.add(sphere);
            dynamicsWorld.addRigidBody(sphere.getBody());
        }

        private Vector3 findStartPos() {
            List<Vector3> locations = ModelUtils.findAreaInModel(plane.model, new ModelUtils.RectangleArea(0.45f, 0.01f, 0.5f, 0.03f), new Vector3(0, 1, 0), 70);
            int randomIndex = (int) MathUtils.randomInRange(0, locations.size());
            return locations.get(randomIndex).add(new Vector3(-level.width * 0.5f, 1, 0));

        }

        private void createTree() {
            this.treeModels = new HashMap<String, Model>();
            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

            treeModels.put("tree_1", modelLoader.loadModel(Gdx.files.getFileHandle("data/tree_1.g3db", Files.FileType.Internal)));
        }

        private void createStarModel() {
            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            star = modelLoader.loadModel(Gdx.files.getFileHandle("data/star.g3db", Files.FileType.Internal));
        }

        private void createPhysicsWorld() {
            collisionConfig = new btDefaultCollisionConfiguration();
            dispatcher = new btCollisionDispatcher(collisionConfig);
            broadphase = new btDbvtBroadphase();
            constraintSolver = new btSequentialImpulseConstraintSolver();
            dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
            dynamicsWorld.setGravity(GRAVITY_VEC);
            contactListener = new SSContactListener();

            collisionWorld = new btCollisionWorld(dispatcher, broadphase, collisionConfig);
            debugDrawer = new DebugDrawer();
            collisionWorld.setDebugDrawer(debugDrawer);
            debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);
        }

        private void finalizePlane() {
            LevelBuilder.calculateNormals(model);

            plane = new GameObject.Constructor(model, GameObject.GameObjectType.PLANE, new btBvhTriangleMeshShape(model.meshParts), 0f).construct();
            constructors.add(plane.constructor);
            plane.transform.setToTranslation(-level.width * 0.5f, 0, 0);
            plane.getBody().setWorldTransform(plane.transform);

            Mesh mesh = model.meshes.get(0);
            int vertexSize = mesh.getVertexSize() / 4;
            float[] vertices = new float[mesh.getNumVertices() * mesh.getVertexSize()];
            mesh.getVertices(vertices);

            minMaxMap = MathUtils.calculateAxisMinMax(vertices, vertexSize);
            Map<String, Object> userData = new HashMap<String, Object>();
            userData.put("u_worldMin", new Vector3(minMaxMap.get("x").min, minMaxMap.get("y").min, minMaxMap.get("z").min));
            userData.put("u_worldMax", new Vector3(minMaxMap.get("x").max, minMaxMap.get("y").max, minMaxMap.get("z").max));
            plane.userData = userData;

            plane.getBody().setContactCallbackFilter(Constants.CollisionsFlag.PLANE_FLAG);

            terrainModelInstances.add(plane);
            short group = 1;
            short mask = 1;
            dynamicsWorld.addRigidBody(plane.getBody(), group, mask);
        }

        private void setupCamera() {
            cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            cam.position.set(0f, 10f, 10f);
            cam.lookAt(0, 0, -20);
            cam.near = 1f;
            cam.far = 20000f;
            cam.update();

            camController = new CameraInputController(cam);
            camController.translateUnits = 200.0f;
        }

        public void dispose() {
            for (GameObject.Constructor constructor : constructors)
                constructor.dispose();
            constructors.clear();
        }

        private float getZPercentComplete() {
            MinMax zMinMax = minMaxMap.get("z");
            return (getSphereLocation().z - zMinMax.max) / (zMinMax.min - zMinMax.max);
        }

        public void render() {
            if (!pause) {
                final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
                dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);
                applyForce();

                if (!sphere.getBody().isActive()) {
                    state = SLEEP;
                } else {
                    if (getZPercentComplete() >= 0.95f) {
                        state = VICTORY;
                    }
                }
            }

            if(!pushed){
                sphere.applyForce(new Vector3(0,0,-90));
                pushed = true;
            }

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            camController.camera.position.set(sphere.getLocationInWorld()
                    .x, sphere.getLocationInWorld().y + 10f, sphere.getLocationInWorld().z + 20f);
            camController.camera.update();
            camController.update();

            terrainModelBatch.begin(cam);
            terrainModelBatch.render(terrainModelInstances, lights);
            terrainModelBatch.end();

            modelBatch.begin(cam);
            modelBatch.render(instances, lights);
            modelBatch.render(modelInstances, lights);
            modelBatch.end();

            // DEBUG PHYSICS
            //debugDrawer.begin(cam);
            //collisionWorld.debugDrawWorld();
            //debugDrawer.end();
        }

        public Vector3 getSphereLocation() {
            return sphere.getLocationInWorld();
        }

        public Vector3 getSphereStartPosition() {
            return sphereStartPosition;
        }

        private void applyForce() {
            // TODO possibly scale based on Linear velocity of the object.
            if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
                if(Gdx.input.getAccelerometerY() > 0.5){
                    sphere.getBody().applyCentralForce(new Vector3(40f * MASS_OF_SLED, 0, 0));
                }else if(Gdx.input.getAccelerometerY() < -0.5){
                    sphere.getBody().applyCentralForce(new Vector3(-40f* MASS_OF_SLED, 0, 0));
                }
                //sphere.getBody().applyCentralForce(new Vector3(Gdx.input.getAccelerometerY() * 4f, 0, 0));

                if (accelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, -10f* MASS_OF_SLED));
                } else if (decelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, 50f* MASS_OF_SLED));
                }
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
                    sphere.getBody().applyCentralForce(new Vector3(-40f* MASS_OF_SLED, 0, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
                    sphere.getBody().applyCentralForce(new Vector3(40f* MASS_OF_SLED, 0, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_UP) || accelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, -10f* MASS_OF_SLED));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN) || decelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, 50f* MASS_OF_SLED));
                }
            }
        }

        private void sledForward(float sideMovement, float forward) {
            Matrix4 out = new Matrix4();
            sphere.getBody().getMotionState().getWorldTransform(out);
            Quaternion q = new Quaternion();

            out.getRotation(q);

            if (q.nor().getPitch() < 0) {
                sideMovement = -sideMovement;
            }
            sphere.getBody().applyCentralForce(new Vector3(sideMovement * MASS_OF_SLED, 0, -forward * MASS_OF_SLED));
        }

        public void setAccelerate(boolean accelerate) {
            this.accelerate = accelerate;
        }

        public void setDecelerate(boolean decelerate) {
            this.decelerate = decelerate;
        }

        class SSContactListener extends ContactListener {

            @Override
            public void onContactProcessed(btCollisionObject colObj0, boolean match0, btCollisionObject colObj1, boolean match1) {
                if(typeExists(GameObject.GameObjectType.TREE, colObj0, colObj1)){
                    sphere.getBody().setActivationState(Collision.ISLAND_SLEEPING);
                } else if(typeExists(GameObject.GameObjectType.STAR, colObj0, colObj1)){
                    btCollisionObject object = findObject(GameObject.GameObjectType.STAR, colObj0, colObj1);
                    GameObject star = (GameObject)object.userData;
                    starsCollected++;
                    int removal = -1;
                    for(int i = 0; i < instances.size; i++){
                        if(instances.get(i).equals(star)){
                            removal = i;
                        }
                    }

                    if(removal > 0){
                        dynamicsWorld.removeRigidBody(instances.get(removal).getBody());
                        instances.removeIndex(removal);
                    }

                }

            }

            private boolean typeExists(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2){
                if(((GameObject) obj1.userData).gameObjectType.equals(entity) || ((GameObject) obj2.userData).gameObjectType.equals(entity)){
                    return true;
                }

                return false;
            }

            private btCollisionObject findObject(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2) {
                if (((GameObject) obj1.userData).gameObjectType.equals(entity)) {
                    return obj1;
                }
                return obj2;
            }

            private boolean collision(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2) {
                return ((GameObject) obj1.userData).gameObjectType == entity || ((GameObject) obj2.userData).gameObjectType == entity;
            }
        }
    }

    private class Hud {
        private Stage stage;
        private Button upButton;
        private Button downButton;
        private Button pauseButton;
        //private ShaderLabel timerLabel;
        private ShaderLabel distanceTraveledLabel;
        private ShaderLabel starsCollectedLabel;
        private float totalTimeInSeconds = 0.0f;
        private float distanceTraveledInMeters = 0.0f;
        private boolean paused = false;

        public Hud(final GameState gs) {
            stage = new Stage();

            upButton = new Button(resources.skin, Constants.UI.UP_BUTTON);
            upButton.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    gs.setAccelerate(true);
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    gs.setAccelerate(false);
                }
            });
            stage.addActor(upButton);

            downButton = new Button(resources.skin, Constants.UI.DOWN_BUTTON);
            downButton.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    gs.setDecelerate(true);
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    gs.setDecelerate(false);
                }
            });
            stage.addActor(downButton);

            pauseButton = new Button(resources.skin, Constants.UI.PAUSE_BUTTON);
            pauseButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    paused = true;
                    gs.pause = true;
                    showMenu();
                }
            });
            stage.addActor(pauseButton);

            //timerLabel = new ShaderLabel(resources.fontShader, "Current: ", resources.skin, Constants.UI.X_SMALL_FONT, Color.GREEN);
            //timerLabel.setAlignment(Align.right);
            //stage.addActor(timerLabel);

            starsCollectedLabel = new ShaderLabel(resources.fontShader, "Stars: ", resources.skin, Constants.UI.X_SMALL_FONT, Color.GREEN);
            starsCollectedLabel.setAlignment(Align.right);
            stage.addActor(starsCollectedLabel);

            distanceTraveledLabel = new ShaderLabel(resources.fontShader, "", resources.skin, Constants.UI.X_SMALL_FONT, Color.GREEN);
            distanceTraveledLabel.setAlignment(Align.right);
            stage.addActor(distanceTraveledLabel);

            Gdx.input.setInputProcessor(stage);
        }

        public void showEndGame(String state) {
            int width = Gdx.graphics.getWidth();
            int height = Gdx.graphics.getHeight();

            final DialogOverlay ovr = new DialogOverlay(resources);
            stage.addActor(ovr);

            ShaderLabel statusLabel;
            float centerX = width * 0.5f;
            if (state.equals(SLEEP)) {
                statusLabel = new ShaderLabel(resources.fontShader, "Game Over", resources.skin, Constants.UI.LARGE_FONT,
                        Color.RED);
            } else {
                statusLabel = new ShaderLabel(resources.fontShader, "Victory", resources.skin, Constants.UI.LARGE_FONT,
                        Color.GREEN);
                String bestTimePref = "bestTime:" + levelToLoad;
                Preferences prefs = Gdx.app.getPreferences(Constants.PREFERENCE_STORE);
                float bestTime = prefs.getFloat(bestTimePref, 100000.0f);

                if (totalTimeInSeconds < bestTime) {
                    ShaderLabel newRecordLabel = new ShaderLabel(resources.fontShader, "New Record!", resources.skin, Constants.UI.DEFAULT_FONT,
                            Color.GREEN);
                    WidgetUtils.centerLabelOnPoint(newRecordLabel, centerX, height * 0.66f);
                    prefs.putFloat(bestTimePref, totalTimeInSeconds);
                    prefs.flush();
                    ovr.addActor(newRecordLabel);
                } else if (bestTime < 100000.0f) {
                    ShaderLabel previousBestLabel = new ShaderLabel(resources.fontShader, "Current Record: " + formatTime(bestTime), resources.skin, Constants.UI.DEFAULT_FONT,
                            Color.GREEN);
                    WidgetUtils.centerLabelOnPoint(previousBestLabel, centerX, height * 0.66f);
                    ovr.addActor(previousBestLabel);
                }

                ShaderLabel timeLabel = new ShaderLabel(resources.fontShader, "Time: " + formatTime(totalTimeInSeconds), resources.skin, Constants.UI.DEFAULT_FONT,
                        Color.GREEN);
                WidgetUtils.centerLabelOnPoint(timeLabel, centerX, height * 0.6f);
                ovr.addActor(timeLabel);
            }

            WidgetUtils.centerLabelOnPoint(statusLabel, centerX, height * 0.75f);


            ShaderButtonWithLabel restartButton = new ShaderButtonWithLabel(resources.fontShader, "Restart", resources.skin, Constants.UI.CLEAR_BUTTON, Constants.UI.SMALL_FONT,
                    Color.WHITE);
            restartButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    renderResult = "restart";
                }
            });

            ShaderButtonWithLabel mainMenuButton = new ShaderButtonWithLabel(resources.fontShader, "Main Menu", resources.skin, Constants.UI.CLEAR_BUTTON, Constants.UI.SMALL_FONT,
                    Color.WHITE);
            mainMenuButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    renderResult = "mainMenu";
                }
            });

            float bHeight = height * 0.125f;
            float menuWidth = width * 0.25f;

            restartButton.setBounds(-menuWidth, height * 0.4f, menuWidth, bHeight);
            mainMenuButton.setBounds(-menuWidth, height * 0.2f, menuWidth, bHeight);

            ovr.addActor(statusLabel);
            ovr.addActor(restartButton);
            ovr.addActor(mainMenuButton);
            restartButton.addAction(moveTo(width * 0.5f - restartButton.getWidth() * 0.5f, restartButton.getY(), 0.4f, Interpolation.pow3));
            mainMenuButton.addAction(moveTo(width * 0.5f - mainMenuButton.getWidth() * 0.5f, mainMenuButton.getY(), 0.4f, Interpolation.pow3));
        }

        public void render() {
            float delta = Gdx.graphics.getDeltaTime();

            if (!paused) {
                totalTimeInSeconds += delta;
                //timerLabel.setText(formatTime(totalTimeInSeconds));
            }
            starsCollectedLabel.setText("Stars: " + gs.starsCollected);
            distanceTraveledLabel.setText(getDistance());

            stage.act(delta);
            stage.draw();
        }

        private String getDistance() {
            return (-1 * ((int) (gs.getSphereLocation().z - gs.getSphereStartPosition().z)) / 10) + " m";
        }

        private String formatTime(float totalTimeInSeconds) {
            int mins = (int) totalTimeInSeconds / 60;
            int seconds = (int) totalTimeInSeconds % 60;
            int fractionalSeconds = (int) ((totalTimeInSeconds - Math.floor(totalTimeInSeconds)) * 1000.0f);
            String fs = "";
            if (fractionalSeconds < 10) {
                fs = "00";
            } else if (fractionalSeconds < 100) {
                fs = "0";
            }
            fs += fractionalSeconds;

            return (mins < 10 ? "0" : "") + mins + ":" + ((seconds < 10) ? "0" : "") + seconds + "." + fs;
        }

        public void resize(int width, int height) {
            float bWidth = width * 0.08f;
            float bHeight = height * 0.125f;

            upButton.setBounds(width * 0.89f, height * 0.25f, bWidth, bHeight);
            downButton.setBounds(width * 0.89f, height * 0.07f, bWidth, bHeight);
            pauseButton.setBounds(width * 0.03f, height * 0.82f, bWidth, bHeight);

            //timerLabel.setBounds(width * 0.85f, height * 0.94f, width * 0.12f, height * 0.06f);
            distanceTraveledLabel.setBounds(width * 0.85f, height * 0.88f, width * 0.12f, height * 0.06f);

            starsCollectedLabel.setBounds(width * 0.85f, height * 0.94f, width * 0.12f, height * 0.06f);
        }

        private void showMenu() {
            int width = Gdx.graphics.getWidth();
            int height = Gdx.graphics.getHeight();

            final DialogOverlay ovr = new DialogOverlay(resources);
            stage.addActor(ovr);

            ShaderButtonWithLabel restartButton = new ShaderButtonWithLabel(resources.fontShader, "Restart", resources.skin, Constants.UI.CLEAR_BUTTON, Constants.UI.SMALL_FONT,
                    Color.WHITE);
            restartButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    renderResult = "restart";
                }
            });

            ShaderButtonWithLabel mainMenuButton = new ShaderButtonWithLabel(resources.fontShader, "Main Menu", resources.skin, Constants.UI.CLEAR_BUTTON, Constants.UI.SMALL_FONT,
                    Color.WHITE);
            mainMenuButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    renderResult = "mainMenu";
                }
            });

            ShaderButtonWithLabel resumeButton = new ShaderButtonWithLabel(resources.fontShader, "Resume", resources.skin, Constants.UI.CLEAR_BUTTON, Constants.UI.SMALL_FONT,
                    Color.WHITE);
            resumeButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ovr.remove();
                    paused = false;
                    gs.pause = false;
                }
            });

            float bHeight = height * 0.125f;
            float menuWidth = width * 0.25f;

            restartButton.setBounds(-menuWidth, height * 0.7f, menuWidth, bHeight);
            mainMenuButton.setBounds(-menuWidth, height * 0.5f, menuWidth, bHeight);
            resumeButton.setBounds(-menuWidth, height * 0.3f, menuWidth, bHeight);

            ovr.addActor(restartButton);
            ovr.addActor(mainMenuButton);
            ovr.addActor(resumeButton);
            restartButton.addAction(moveTo(width * 0.5f - restartButton.getWidth() * 0.6f, restartButton.getY(), 0.4f, Interpolation.pow3));
            mainMenuButton.addAction(moveTo(width * 0.5f - mainMenuButton.getWidth() * 0.6f, mainMenuButton.getY(), 0.4f, Interpolation.pow3));
            resumeButton.addAction(moveTo(width * 0.5f - resumeButton.getWidth() * 0.6f, resumeButton.getY(), 0.4f, Interpolation.pow3));
        }

        public void dispose() {
            stage.dispose();
            stage = null;
        }
    }
}
