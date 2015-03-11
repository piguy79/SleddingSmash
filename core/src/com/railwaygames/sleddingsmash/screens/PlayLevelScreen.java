package com.railwaygames.sleddingsmash.screens;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
import com.railwaygames.sleddingsmash.levels.obstacles.TreeObstacleGenerator;
import com.railwaygames.sleddingsmash.overlay.DialogOverlay;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;
import com.railwaygames.sleddingsmash.widgets.ShaderButtonWithLabel;
import com.railwaygames.sleddingsmash.widgets.ShaderLabel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.railwaygames.sleddingsmash.SleddingSmashEditor.Level;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.MODIFICATION_TYPE;

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

    private static class GameState {
        private static final float MASS_OF_SLED = 100;
        private static final float MASS_OF_CHARACTER = 200;
        private static final float PHYSICS_SCALE_FACTOR = 1f;
        private static final float sideMove = 10f;
        private static final float forwardMove = 0.4f;
        private static final float rotation = 0.5f;
        public Environment lights;
        public PerspectiveCamera cam;
        public ModelBatch modelBatch;
        public Model model;
        public Map<String, Model> treeModels;
        public CameraInputController camController;
        public Array<GameObject> instances = new Array<GameObject>();
        public Array<ModelInstance> modelInstances = new Array<ModelInstance>();
        private List<GameObject.Constructor> constructors = new ArrayList<GameObject.Constructor>();
        private GameObject sphere;
        private GameObject plane;
        private btCollisionConfiguration collisionConfig;
        private btDispatcher dispatcher;
        private SSContactListener contactListener;
        private btBroadphaseInterface broadphase;
        private btDynamicsWorld dynamicsWorld;
        private btConstraintSolver constraintSolver;
        private Level level;
        private boolean accelerate = false;
        private boolean decelerate = false;
        private DebugDrawer debugDrawer;
        private btCollisionWorld collisionWorld;
        private boolean pushed = false;
        private Vector3 sphereStartPosition;

        public void buildLevel(Level level) {
            this.level = level;
            lights = new Environment();
            lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
            lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

            modelBatch = new ModelBatch();

            createPhysicsWorld();
            setupCamera();
            createPlane(level.width, level.length);
            createTree();

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
                TreeObstacleGenerator treeGenerator = new TreeObstacleGenerator(treeModels.get(obstacle.getModelToUse()));
                List<GameObject> gameObjects = new ArrayList<GameObject>();
                boolean needsPositions = obstacle.generatedPositions == null;

                if (needsPositions) {
                    obstacle.generatedPositions = new ArrayList<Vector3>();
                    gameObjects = treeGenerator.generateObstacles(plane.model, obstacle.params, new Vector3(0, 1, 0), new Vector3(-level.width * 0.5f, 0, 0));
                } else {
                    gameObjects = treeGenerator.generateAt(obstacle.generatedPositions, obstacle.params, new Vector3(-level.width * 0.5f, 0, 0));
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
            //createSled();
        }

        private void createPlane(float width, float length) {
            this.level.width = width;
            this.level.length = length;

            model = LevelBuilder.generate(width, length);
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

            sphere = new GameObject.Constructor(model, GameObject.GameObjectType.CHARACTER, new btSphereShape(2f), 1f).construct();
            constructors.add(sphere.constructor);

            sphere.getBody().setFriction(1);

            sphereStartPosition = findStartPos();
            sphere.transform.setToTranslation(sphereStartPosition);
            sphere.getBody().setWorldTransform(sphere.transform);

            instances.add(sphere);
            dynamicsWorld.addRigidBody(sphere.getBody());
        }

        private void createSled() {

            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            Model model = modelLoader.loadModel(Gdx.files.getFileHandle("data/sled_1.g3db", Files.FileType.Internal));

            btCompoundShape compoundShape = new btCompoundShape();

            btBoxShape box = new btBoxShape(new Vector3(5f, 0.5f, 3f));
            compoundShape.addChildShape(new Matrix4(new Vector3(3, 0, 0), new Quaternion(), new Vector3(1, 1, 1)), box);

            btCylinderShape cylinder = new btCylinderShape(new Vector3(1, 3, 2));
            compoundShape.addChildShape(new Matrix4(new Vector3(-3, 0, 0f), new Quaternion(new Vector3(1, 0, 0), -90), new Vector3(1f, 1.5f, 1f)), cylinder);

            sphere = new GameObject.Constructor(model, GameObject.GameObjectType.CHARACTER, new btSphereShape(2f), MASS_OF_SLED * PHYSICS_SCALE_FACTOR).construct();
            constructors.add(sphere.constructor);

            sphere.getBody().setFriction(1f);
            sphere.getBody().setRestitution(0);

            sphere.getBody().setLinearVelocity(new Vector3(0, 0, -6));

            List<Vector3> locations = ModelUtils.findAreaInModel(plane.model, new ModelUtils.RectangleArea(0.5f, 0.01f, 0.6f, 0.03f), new Vector3(0, 1, 0), 180);
            int randomIndex = (int) MathUtils.randomInRange(0, locations.size());
            Vector3 pos = locations.get(randomIndex);

            sphere.transform.setToTranslation((-level.width * 0.5f) + pos.x, pos.y + 2f, pos.z);
            sphere.transform.rotate(0, 1, 0, -90);

            sphere.getBody().setWorldTransform(sphere.transform);
            collisionWorld.addCollisionObject(sphere.getBody());
            instances.add(sphere);
            dynamicsWorld.addRigidBody(sphere.getBody());
        }

        private Vector3 findStartPos() {
            List<Vector3> locations = ModelUtils.findAreaInModel(plane.model, new ModelUtils.RectangleArea(0.2f, 0.01f, 0.2f, 0.03f), new Vector3(0, 1, 0), 70);
            int randomIndex = (int) MathUtils.randomInRange(0, locations.size());
            return locations.get(randomIndex).add(new Vector3(0, 20, 0));
        }

        private void createTree() {
            this.treeModels = new HashMap<String, Model>();
            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            treeModels.put("tree_1", modelLoader.loadModel(Gdx.files.getFileHandle("data/tree_1.g3db", Files.FileType.Internal)));
            treeModels.put("tree", modelLoader.loadModel(Gdx.files.getFileHandle("data/tree.g3db", Files.FileType.Internal)));
        }

        private void createPhysicsWorld() {
            collisionConfig = new btDefaultCollisionConfiguration();
            dispatcher = new btCollisionDispatcher(collisionConfig);
            broadphase = new btDbvtBroadphase();
            constraintSolver = new btSequentialImpulseConstraintSolver();
            dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
            dynamicsWorld.setGravity(new Vector3(0, -10 * 4, 0));
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


            // Use for seeing the physcis on the plane
            //collisionWorld.addCollisionObject(plane.getBody());

            instances.add(plane);
            dynamicsWorld.addRigidBody(plane.getBody());
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
//            Gdx.input.setInputProcessor(camController);
        }

        public void dispose() {
            for (GameObject.Constructor constructor : constructors)
                constructor.dispose();
            constructors.clear();
        }

        public void render() {
            final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

            dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

            applyForce();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            camController.camera.position.set(sphere.getLocationInWorld()
                    .x, sphere.getLocationInWorld().y + 10f, sphere.getLocationInWorld().z + 20f);
            camController.camera.update();
            camController.update();

            modelBatch.begin(cam);
            modelBatch.render(instances, lights);
            modelBatch.render(modelInstances, lights);
            modelBatch.end();

            debugDrawer.begin(cam);
            collisionWorld.debugDrawWorld();
            debugDrawer.end();
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
                sphere.getBody().applyCentralForce(new Vector3(Gdx.input.getAccelerometerY() * 2, 0, 0));
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
                    sphere.getBody().applyCentralForce(new Vector3(-9f, 0, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
                    sphere.getBody().applyCentralForce(new Vector3(9f, 0, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_UP) || accelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, -5f));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN) || decelerate) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, 2f));
                }
            }
        }

        private void applyForceToSled() {
            // TODO possibly scale based on Linear velocity of the object.
            if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
                sphere.getBody().applyCentralForce(new Vector3(Gdx.input.getAccelerometerY() * 2, 0, 0));
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
                    Vector3 torque = sphere.getLocationInWorld();
                    Vector3 smr = new Vector3(0, rotation, 0).add(torque);
                    sphere.getBody().applyTorqueImpulse(new Vector3(0, rotation * MASS_OF_SLED, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
                    Vector3 torque = sphere.getLocationInWorld();
                    Vector3 smr = new Vector3(0, -rotation, 0).add(torque);
                    sphere.getBody().applyTorqueImpulse(new Vector3(0, -rotation * MASS_OF_SLED, 0));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)) {
                    sledForward(sideMove, forwardMove);
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)) {
                    Vector3 relativeForce = new Vector3(0, 0, 4);
                    sphere.applyForce(relativeForce);
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
            public void onContactStarted(btCollisionObject colObj0, btCollisionObject colObj1) {
                if (collision(GameObject.GameObjectType.TREE, colObj0, colObj1) && collision(GameObject.GameObjectType.CHARACTER, colObj0, colObj1)) {
                    btCollisionObject tree = findObject(GameObject.GameObjectType.TREE, colObj0, colObj1);
                    Vector3 velocity = sphere.getBody().getLinearVelocity();
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, -100000));
                }
            }

            private btCollisionObject findObject(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2) {
                if (((GameObject) obj1.userData).gameObjectType == entity) {
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
        private ShaderLabel timerLabel;
        private ShaderLabel distanceTraveledLabel;
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
                    showMenu();
                }
            });
            stage.addActor(pauseButton);

            timerLabel = new ShaderLabel(resources.fontShader, "", resources.skin, Constants.UI.X_SMALL_FONT, Color.GREEN);
            timerLabel.setAlignment(Align.right);
            stage.addActor(timerLabel);

            distanceTraveledLabel = new ShaderLabel(resources.fontShader, "", resources.skin, Constants.UI.X_SMALL_FONT, Color.GREEN);
            distanceTraveledLabel.setAlignment(Align.right);
            stage.addActor(distanceTraveledLabel);

            Gdx.input.setInputProcessor(stage);
        }

        public void render() {
            float delta = Gdx.graphics.getDeltaTime();

            if (!paused) {
                totalTimeInSeconds += delta;
                timerLabel.setText(formatTime(totalTimeInSeconds));
            }

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

            timerLabel.setBounds(width * 0.85f, height * 0.94f, width * 0.12f, height * 0.06f);
            distanceTraveledLabel.setBounds(width * 0.85f, height * 0.88f, width * 0.12f, height * 0.06f);
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
