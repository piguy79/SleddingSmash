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
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.UBJsonReader;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;

import java.util.HashMap;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class SleddingSmash extends ApplicationAdapter {

    public Environment lights;
    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public Model model;
    public Array<GameObject> instances;
    public CameraInputController camController;
    GameObject sphere;
    btCollisionConfiguration collisionConfig;
    btDispatcher dispatcher;
    MyContactListener contactListener;
    btBroadphaseInterface broadphase;
    btDynamicsWorld dynamicsWorld;
    btConstraintSolver constraintSolver;
    ArrayMap<String, GameObject.Constructor> constructors;

    @Override
    public void create() {
        Bullet.init();
        instances = new Array<GameObject>();

        lights = new Environment();
        lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        constructors = new ArrayMap<String, GameObject.Constructor>(String.class, GameObject.Constructor.class);
        modelBatch = new ModelBatch();

        createPhysicsWorld();

        createPlane();
        createBall();
        createTree();

        setupCamera();
    }

    private void createTree(){
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        model = modelLoader.loadModel(Gdx.files.getFileHandle("data/tree.g3db", Files.FileType.Internal));
        // Now create an instance.  Instance holds the positioning data, etc of an instance of your model
        ModelInstance modelInstance = new ModelInstance(model);

        constructors.put("tree", new GameObject.Constructor(model, new btBoxShape(new Vector3(2f, 2f, 2f)), 0));
        GameObject obj = constructors.get("tree").construct();

        obj.transform.rotate(1, 0, 0, -90);
        obj.transform.setToTranslation(10f, -39f, -40f);
        obj.getBody().setWorldTransform(obj.transform);

        instances.add(obj);
        dynamicsWorld.addRigidBody(obj.getBody());

    }

    private void createBall() {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.node().id = "sphere";
        mb.part("sphere", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED)))
                .sphere(1f, 1f, 1f, 10, 10);
        model = mb.end();

        constructors.put("sphere", new GameObject.Constructor(model, new btSphereShape(0.5f), 1f));

        sphere = constructors.get("sphere").construct();
        sphere.getBody().setFriction(100f);
        sphere.transform.setToTranslation(0f, 9f, -9f);
        sphere.getBody().setWorldTransform(sphere.transform);

        instances.add(sphere);
        dynamicsWorld.addRigidBody(sphere.getBody());
    }

    private void createPlane() {
        ModelBuilder modelBuilder = new ModelBuilder();
        float width = 120.0f;
        float length = 2500.0f;
        model = LevelBuilder.generate(width, length);

        SlopeModifier slopeModifier = new SlopeModifier();
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "s");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "x");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.1f);
            put(SlopeModifier.IMPACT_AMOUNT, 0.5f);
            put(SlopeModifier.INTERPOLATION, Interpolation.circleOut);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.00f);
            put(SlopeModifier.IMPACT_AMOUNT, 375.0f);
            put(SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION, 0.7f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.3f);
            put(SlopeModifier.IMPACT_AMOUNT, 90.0f);
            put(SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION, 0.4f);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "s");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "x");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.4f);
            put(SlopeModifier.IMPACT_AMOUNT, 2.5f);
            put(SlopeModifier.INTERPOLATION, Interpolation.circleOut);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.1f);
            put(SlopeModifier.IMPACT_AMOUNT, 20.0f);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "x");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.0f);
            put(SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION, 0.1f);
            put(SlopeModifier.IMPACT_AMOUNT, -40.0f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "x");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.9f);
            put(SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION, 0.1f);
            put(SlopeModifier.IMPACT_AMOUNT, 40.0f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "x");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.8f);
            put(SlopeModifier.IMPACT_AMOUNT, -120.0f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "z");
            put(SlopeModifier.IMPACT_AXIS, "x");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.2f);
            put(SlopeModifier.IMPACT_AMOUNT, 200.0f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});
        slopeModifier.modify(model, new HashMap<String, Object>() {{
            put(SlopeModifier.MODIFICATION_TYPE, "t");
            put(SlopeModifier.EVAL_AXIS, "x");
            put(SlopeModifier.IMPACT_AXIS, "y");
            put(SlopeModifier.EVAL_AXIS_START_RATIO, 0.5f);
            put(SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION, 0.1f);
            put(SlopeModifier.IMPACT_AMOUNT, 40.0f);
            put(SlopeModifier.INTERPOLATION, Interpolation.linear);
        }});

        LevelBuilder.calculateNormals(model);

        constructors.put("plane", new GameObject.Constructor(model, new btBvhTriangleMeshShape(model.meshParts), 0f));
        GameObject plane = constructors.get("plane").construct();
        plane.transform.setToTranslation(-width * 0.5f, 0, 0);
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
        Gdx.input.setInputProcessor(camController);
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

        camController.camera.position.set(sphere.getPosition().x, sphere.getPosition().y + 10f, sphere.getPosition().z + 10f);
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
        }else{
            if (Gdx.input.isKeyPressed(Input.Keys.DPAD_LEFT)) {
                sphere.getBody().applyCentralForce(new Vector3(-9f, 0, 0));
            } else if (Gdx.input.isKeyPressed(Input.Keys.DPAD_RIGHT)) {
                sphere.getBody().applyCentralForce(new Vector3(9f, 0, 0));
            } else if(Gdx.input.isKeyPressed(Input.Keys.DPAD_UP)){
                sphere.getBody().applyCentralForce(new Vector3(0, 0, -5f));
            } else if(Gdx.input.isKeyPressed(Input.Keys.DPAD_DOWN)){
                sphere.getBody().applyCentralForce(new Vector3(0, 0, 2f));
            }
        }
    }


    @Override
    public void dispose() {
        for (GameObject obj : instances)
            obj.dispose();
        instances.clear();

        for (GameObject.Constructor constructor : constructors.values())
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
    }
}