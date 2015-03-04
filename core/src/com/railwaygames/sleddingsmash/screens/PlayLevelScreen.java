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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBox2dShape;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.badlogic.gdx.physics.bullet.collision.btConeShape;
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.UBJsonReader;
import com.railwaygames.sleddingsmash.Resources;
import com.railwaygames.sleddingsmash.SleddingSmashEditor;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.BumpyTerrainModifier;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;
import com.railwaygames.sleddingsmash.levels.obstacles.TreeObstacleGenerator;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;

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
        FileHandle fileHandle = Gdx.files.internal(levelToLoad);

        Json json = new Json();
        Level level = json.fromJson(Level.class, fileHandle);

        gs = new GameState();
        gs.buildLevel(level);
    }

    @Override
    public void render(float delta) {
        gs.render();
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

    @Override
    public void hide() {
        gs.dispose();
        gs = null;
    }

    @Override
    public void dispose() {

    }

    private static class GameState {
        public Environment lights;
        public PerspectiveCamera cam;
        public ModelBatch modelBatch;
        public Model model;
        public Map<String, Model> treeModels;
        public CameraInputController camController;
        public Array<GameObject> instances = new Array<GameObject>();
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

        private DebugDrawer debugDrawer;
        private btCollisionWorld collisionWorld;

        private static final float PHYSICS_SCALE_FACTOR = 3f;

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

            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            Model model = modelLoader.loadModel(Gdx.files.getFileHandle("data/sled_1.g3db", Files.FileType.Internal));

            btCompoundShape compoundShape = new btCompoundShape();

            btBoxShape box = new btBoxShape(new Vector3(4f,0.2f,2f));
            compoundShape.addChildShape(new Matrix4(new Vector3(0,0,0), new Quaternion(), new Vector3(1,1,1)), box);

            btCylinderShape cylinder = new btCylinderShape(new Vector3(1,1,1));
            compoundShape.addChildShape(new Matrix4(new Vector3(-3,0,0f), new Quaternion(new Vector3(1,0,0), -90), new Vector3(1f,1.5f,1f)), cylinder);

            sphere = new GameObject.Constructor(model, GameObject.GameObjectType.CHARACTER, compoundShape, 50f * PHYSICS_SCALE_FACTOR).construct();
            constructors.add(sphere.constructor);

            collisionWorld.addCollisionObject(sphere.getBody());
            sphere.getBody().setFriction(0.1f);

            List<Vector3> locations = ModelUtils.findAreaInModel(plane.model, new ModelUtils.RectangleArea(0.5f,0.01f, 0.6f, 0.03f),new Vector3(0, 1, 0), 180);
            int randomIndex = (int)MathUtils.randomInRange(0, locations.size());
            Vector3 pos = locations.get(randomIndex);

            sphere.transform.setToTranslation((-level.width * 0.5f) + pos.x, pos.y+2f, pos.z);
            sphere.transform.rotate(0,1,0,-90);
            sphere.getBody().setWorldTransform(sphere.transform);


            instances.add(sphere);
            dynamicsWorld.addRigidBody(sphere.getBody());
        }

        private void createTree() {
            this.treeModels = new HashMap<String, Model>();
            UBJsonReader jsonReader = new UBJsonReader();
            G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            treeModels.put("tree",modelLoader.loadModel(Gdx.files.getFileHandle("data/tree.g3db", Files.FileType.Internal)));
        }

        private void createPhysicsWorld() {
            collisionConfig = new btDefaultCollisionConfiguration();
            dispatcher = new btCollisionDispatcher(collisionConfig);
            broadphase = new btDbvtBroadphase();
            constraintSolver = new btSequentialImpulseConstraintSolver();
            dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
            dynamicsWorld.setGravity(new Vector3(0, -10 * PHYSICS_SCALE_FACTOR, 0));
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
            //plane.getBody().setFriction(100f * PHYSICS_SCALE_FACTOR);
            //plane.getBody().setRestitution(100f * PHYSICS_SCALE_FACTOR);

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
            cam.far = 1500f;
            cam.update();

            camController = new CameraInputController(cam);
            Gdx.input.setInputProcessor(camController);
        }

        public void dispose() {
            for (GameObject.Constructor constructor : constructors)
                constructor.dispose();
            constructors.clear();
        }

        public void render() {
            final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

            dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);

            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            applyForce();

            for (GameObject obj : instances) {
                obj.getBody().getWorldTransform(obj.transform);
            }

            camController.camera.position.set(sphere.getLocationInWorld()
                    .x, sphere.getLocationInWorld().y + 10f, sphere.getLocationInWorld().z + 20f);
            camController.camera.update();
            camController.update();

            modelBatch.begin(cam);
            modelBatch.render(instances, lights);
            modelBatch.end();

            debugDrawer.begin(cam);
            collisionWorld.debugDrawWorld();
            debugDrawer.end();
        }

        private void applyForce() {
            // TODO possibly scale based on Linear velocity of the object.
            if (Gdx.input.isPeripheralAvailable(Input.Peripheral.Accelerometer)) {
                sphere.getBody().applyCentralForce(new Vector3(Gdx.input.getAccelerometerY() * 2, 0, 0));
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
                    sphere.getBody().applyCentralForce(new Vector3(-15f * PHYSICS_SCALE_FACTOR, 0, 0));

                    //Vector3 force = new Vector3(15f * PHYSICS_SCALE_FACTOR, 0, 0);
                    //Vector3 position = new Vector3(sphere.width(), 0, sphere.height());
                    //sphere.getBody().applyForce(force, position);
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
                    sphere.getBody().applyCentralForce(new Vector3(15f * PHYSICS_SCALE_FACTOR, 0, 0));

                    //Vector3 force = new Vector3(-15f * PHYSICS_SCALE_FACTOR, 0, 0);
                    //Vector3 position = new Vector3(sphere.width(), 0, sphere.height());
                    //sphere.getBody().applyForce(force, position);
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, -20f * PHYSICS_SCALE_FACTOR));
                } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)) {
                    sphere.getBody().applyCentralForce(new Vector3(0, 0, 4f *PHYSICS_SCALE_FACTOR));
                }
            }
        }

        class SSContactListener extends ContactListener {
            @Override
            public void onContactStarted(btCollisionObject colObj0, btCollisionObject colObj1) {
                if(collision(GameObject.GameObjectType.TREE, colObj0, colObj1) && collision(GameObject.GameObjectType.CHARACTER, colObj0, colObj1)){
                    btCollisionObject tree = findObject(GameObject.GameObjectType.TREE, colObj0, colObj1);
                    Vector3 velocity = sphere.getBody().getLinearVelocity();
                    sphere.getBody().applyCentralForce(new Vector3(0,0,-100000));
                }
            }

            private btCollisionObject findObject(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2){
                if(((GameObject)obj1.userData).gameObjectType == entity){
                    return obj1;
                }
                return obj2;
            }

            private boolean collision(GameObject.GameObjectType entity, btCollisionObject obj1, btCollisionObject obj2){
                return ((GameObject)obj1.userData).gameObjectType == entity || ((GameObject)obj2.userData).gameObjectType ==entity;
            }
        }
    }
}
