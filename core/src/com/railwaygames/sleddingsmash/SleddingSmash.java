package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.UBJsonReader;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;
import com.railwaygames.sleddingsmash.levels.obstacles.TreeObstacleGenerator;
import com.railwaygames.sleddingsmash.utils.MathUtils;
import com.railwaygames.sleddingsmash.utils.ModelUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class SleddingSmash extends ApplicationAdapter {

    public Environment lights;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public Model model;
    public Array<GameObject> instances;
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
    float width;

    @Override
    public void create() {
        Bullet.init();
        instances = new Array<GameObject>();

        lights = new Environment();
        lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        constructors = new ArrayList<GameObject.Constructor>();
        modelBatch = new ModelBatch();

        createPhysicsWorld();
        setupCamera();


        createPlane();
        createBall();
        createTree();
        //createRock();

    }

    private void createTree() {
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        model = modelLoader.loadModel(Gdx.files.getFileHandle("data/tree.g3db", Files.FileType.Internal));

        TreeObstacleGenerator treeGenerator = new TreeObstacleGenerator(model);

        List<GameObject> gameObjects = treeGenerator.generateObstacles(plane.model,new HashMap<String, Object>(), new Vector3(0,1,0), new Vector3(-width * 0.5f,0,0));


        for (GameObject object : gameObjects) {
            constructors.add(object.constructor);
            instances.add(object);
            dynamicsWorld.addRigidBody(object.getBody());
        }
    }

    private void createRock() {
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        model = modelLoader.loadModel(Gdx.files.getFileHandle("data/rock_2.g3db", Files.FileType.Internal));

        TreeObstacleGenerator treeGenerator = new TreeObstacleGenerator(model);
        List<GameObject> gameObjects = treeGenerator.generateObstacles(plane.model,new HashMap<String, Object>(), cam.up, new Vector3(-width * 0.5f,0,0));


        for (GameObject object : gameObjects) {
            object.transform.rotate(1, 0, 0, MathUtils.randomInRange(0, 360));
            object.getBody().setWorldTransform(object.transform);
            constructors.add(object.constructor);
            instances.add(object);
            dynamicsWorld.addRigidBody(object.getBody());
        }
    }

    private void createBall() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.node().id = "sphere";
        mb.part("sphere", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED)))
                .sphere(1f, 1f, 1f, 10, 10);
        model = mb.end();

        sphere = new GameObject.Constructor(model, new btSphereShape(0.5f), 1f).construct();
        constructors.add(sphere.constructor);

        sphere.getBody().setFriction(100f);
        sphere.transform.setToTranslation(0f, 9f, -9f);
        sphere.getBody().setWorldTransform(sphere.transform);
        sphere.getBody().userData = "Sphere";

        instances.add(sphere);
        dynamicsWorld.addRigidBody(sphere.getBody());
    }

    private void createPlane() {
        ModelBuilder modelBuilder = new ModelBuilder();
        width = 120.0f;
        float length = 2500.0f;
        model = LevelBuilder.generate(width, length);

        SlopeModifier slopeModifier = new SlopeModifier();


        LevelBuilder.calculateNormals(model);

        plane = new GameObject.Constructor(model, new btBvhTriangleMeshShape(model.meshParts), 0f).construct();
        constructors.add(plane.constructor);
        plane.transform.setToTranslation(-width * 0.5f, 0, 0);
        plane.getBody().setWorldTransform(plane.transform);
        plane.getBody().userData = "Plane";

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

        Vector3 up = cam.up;

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
        modelBatch.end();
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
            } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)) {
                sphere.getBody().applyCentralForce(new Vector3(0, 0, -5f));
            } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)) {
                sphere.getBody().applyCentralForce(new Vector3(0, 0, 2f));
            }
        }
    }


    @Override
    public void dispose() {
        for (GameObject obj : instances)
            obj.dispose();
        instances.clear();

        for (GameObject.Constructor constructor : constructors)
            constructor.dispose();
        constructors.clear();

        modelBatch.dispose();
        model.dispose();
        dynamicsWorld.dispose();
        constraintSolver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();

        contactListener.dispose();
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

    class MyContactListener extends ContactListener {
        @Override
        public boolean onContactAdded(int userValue0, int partId0, int index0, int userValue1, int partId1, int index1) {
            return true;
        }

        @Override
        public void onContactStarted(btCollisionObject colObj0, btCollisionObject colObj1) {
            if(collision("Tree", colObj0, colObj1) && collision("Sphere", colObj0, colObj1)){
                //btCollisionObject sphere = findObject("Sphere", colObj0, colObj1);
                btCollisionObject tree = findObject("Tree", colObj0, colObj1);
                Vector3 velocity = sphere.getBody().getLinearVelocity();
                sphere.getBody().applyCentralForce(new Vector3(0,0,-100000));
            }

        }
    }

    private btCollisionObject findObject(String entity, btCollisionObject obj1, btCollisionObject obj2){
        if(((String)obj1.userData).equals(entity)){
            return obj1;
        }
        return obj2;
    }

    private boolean collision(String entity, btCollisionObject obj1, btCollisionObject obj2){
        return ((String)obj1.userData).equals(entity) || ((String)obj2.userData).equals(entity);
    }
}