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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.railwaygames.sleddingsmash.SleddingSmashEditor.Level;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.MODIFICATION_TYPE;

public class PlayLevelScreen implements ScreenFeedback {

    private final Resources resources;
    private String levelToLoad;
    private String renderResult = null;
    private GameState gs;
    private HudButtons hudButtons;

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

        hudButtons = new HudButtons(gs, resources);
    }

    @Override
    public void render(float delta) {
        gs.render();
        hudButtons.render();
    }

    @Override
    public void resize(int width, int height) {
        hudButtons.resize(width, height);

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

        hudButtons.dispose();
        hudButtons = null;
    }

    @Override
    public void dispose() {

    }

    private static class HudButtons {

        private Stage stage;
        private Button upButton;
        private Button downButton;
        private GameState gs;

        public HudButtons(GameState gs, Resources resources1) {
            this.gs = gs;
            stage = new Stage();

            upButton = new Button(resources1.skin, Constants.UI.UP_BUTTON);
            stage.addActor(upButton);

            downButton = new Button(resources1.skin, Constants.UI.DOWN_BUTTON);
            stage.addActor(downButton);

            Gdx.input.setInputProcessor(stage);
        }

        public void render() {
            float delta = Gdx.graphics.getDeltaTime();

            stage.act(delta);
            stage.draw();
        }

        public void resize(int width, int height) {
            upButton.setBounds(width * 0.89f, height * 0.25f, width * 0.08f, height * 0.12f);
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
            downButton.setBounds(width * 0.89f, height * 0.07f, width * 0.08f, height * 0.12f);
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
        }

        public void dispose() {
            stage.dispose();
            stage = null;
        }
    }

    private static class GameState {
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

        public void buildLevel(Level level) {
            this.level = level;
            lights = new Environment();
            lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
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

            finalizePlane();
            createBall();
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
                    .sphere(1f, 1f, 1f, 10, 10);
            Model model = mb.end();

            sphere = new GameObject.Constructor(model, GameObject.GameObjectType.CHARACTER, new btSphereShape(0.5f), 1f).construct();
            constructors.add(sphere.constructor);

            sphere.getBody().setFriction(100f);
            sphere.transform.setToTranslation(0f, 0f, -9f);
            sphere.getBody().setWorldTransform(sphere.transform);

            instances.add(sphere);
            dynamicsWorld.addRigidBody(sphere.getBody());
        }

        private void createTree() {
            this.treeModels = new HashMap<String, Model>();
            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            treeModels.put("tree", modelLoader.loadModel(Gdx.files.getFileHandle("data/tree.g3db", Files.FileType.Internal)));
        }

        private void createPhysicsWorld() {
            collisionConfig = new btDefaultCollisionConfiguration();
            dispatcher = new btCollisionDispatcher(collisionConfig);
            broadphase = new btDbvtBroadphase();
            constraintSolver = new btSequentialImpulseConstraintSolver();
            dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
            dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
            contactListener = new SSContactListener();
        }

        private void finalizePlane() {
            LevelBuilder.calculateNormals(model);

            plane = new GameObject.Constructor(model, GameObject.GameObjectType.PLANE, new btBvhTriangleMeshShape(model.meshParts), 0f).construct();
            constructors.add(plane.constructor);

            plane.transform.setToTranslation(-level.width * 0.5f, 0, 0);
            plane.getBody().setWorldTransform(plane.transform);

            instances.add(plane);
            dynamicsWorld.addRigidBody(plane.getBody());
        }

        private void setupCamera() {
            cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            cam.position.set(0f, 10f, 10f);
            cam.lookAt(0, 0, -20);
            cam.near = 1f;
            cam.far = 1500f;
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

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            applyForce();

            for (GameObject obj : instances) {
                obj.getBody().getWorldTransform(obj.transform);
            }

            camController.camera.position.set(sphere.getLocationInWorld()
                    .x, sphere.getLocationInWorld().y + 10f, sphere.getLocationInWorld().z + 10f);
            camController.camera.update();
            camController.update();

            modelBatch.begin(cam);
            modelBatch.render(instances, lights);
            modelBatch.render(modelInstances, lights);
            modelBatch.end();
        }

        private boolean accelerate = false;
        private boolean decelerate = false;
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
}
