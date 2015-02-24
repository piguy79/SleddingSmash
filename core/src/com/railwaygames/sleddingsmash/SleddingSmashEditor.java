package com.railwaygames.sleddingsmash;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.railwaygames.sleddingsmash.entity.GameObject;
import com.railwaygames.sleddingsmash.levels.LevelBuilder;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier;
import com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.InterpolationChoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS_INTERPOLATION_DURATION;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.EVAL_AXIS_START_RATIO;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.IMPACT_AMOUNT;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.IMPACT_AXIS;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.INTERPOLATION;
import static com.railwaygames.sleddingsmash.levels.modifiers.SlopeModifier.MODIFICATION_TYPE;

public class SleddingSmashEditor extends ApplicationAdapter {

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
    private BitmapFont font;
    private Skin skin;
    private Stage stage;
    private Group leftMenus;
    private Group rightMenus;
    private Map<String, Runnable> menuHandlerMap = new HashMap<String, Runnable>();
    private List<Modifier> modifiers = new ArrayList<Modifier>();
    private float width;
    private float length;

    @Override
    public void create() {
        Bullet.init();
        instances = new Array<GameObject>();

        lights = new Environment();
        lights.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.2f, 0.2f, 0.2f, 1f));
        lights.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        constructors = new ArrayMap<String, GameObject.Constructor>(String.class, GameObject.Constructor.class);
        modelBatch = new ModelBatch();

        font = new BitmapFont(Gdx.files.internal("data/fonts/font16.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        stage = new Stage();
        skin = new Skin();
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        AssetManager assetManager = new AssetManager();
        assetManager.load("data/images/menus.atlas", TextureAtlas.class);
        assetManager.finishLoading();

        TextureAtlas menusAtlas = assetManager.get("data/images/menus.atlas", TextureAtlas.class);

        TextureRegionDrawable trd = new TextureRegionDrawable(menusAtlas.findRegion("textFieldBg"));
        trd.setLeftWidth(20);
        trd.setRightWidth(20);
        TextureRegionDrawable cursor = new TextureRegionDrawable(menusAtlas.findRegion("cursor"));
        TextField.TextFieldStyle style = new TextField.TextFieldStyle(font, Color.BLACK, cursor, null, trd);
        skin.add("default", style);
        skin.add("default", new SelectBox.SelectBoxStyle(font, Color.BLACK, trd, new ScrollPane.ScrollPaneStyle(), new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(font, Color.BLUE, Color.WHITE, cursor)));

        createPhysicsWorld();
        setupCamera();

        menuHandlerMap.put("New", createNewRunnable());
        menuHandlerMap.put("Add", createAddRunnable());
        menuHandlerMap.put("Reset", createResetRunnable());
        menuHandlerMap.put("Transform", createSlopeModifierRunnable(ModifierType.TRANSFORM, null));
        menuHandlerMap.put("Scale", createSlopeModifierRunnable(ModifierType.SCALE, null));

        Gdx.input.setInputProcessor(stage);

        float height = Gdx.graphics.getHeight();
        float width = Gdx.graphics.getWidth();

        leftMenus = new Group();
        leftMenus.setBounds(0.01f * width, 0, width * 0.45f, height);
        stage.addActor(leftMenus);

        rightMenus = new Group();
        rightMenus.setBounds(0.75f * width, 0, width * 0.25f, height);
        stage.addActor(rightMenus);

        showMenus(true, "New");
    }

    private void showMenus(boolean right, String... menus) {
        float height = Gdx.graphics.getHeight();

        if (right) {
            rightMenus.clear();
        } else {
            leftMenus.clear();
        }

        float y = height * 0.95f;
        for (final String menu : menus) {
            Label label = new Label(menu, skin, "default");
            label.setColor(Color.WHITE);

            BitmapFont.TextBounds bounds = label.getTextBounds();
            label.setBounds(0, y, bounds.width, bounds.height);
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

            y -= height * 0.07f;
            if (right) {
                rightMenus.addActor(label);
            } else {
                leftMenus.addActor(label);
            }
        }
    }

    private void edit(String menu) {
        String[] split = menu.split(":");
        Modifier modifier = modifiers.get(Integer.valueOf(split[0]));

        createSlopeModifierRunnable(modifier.type, modifier).run();
    }

    private void createPlane(float width, float length) {
        this.width = width;
        this.length = length;

        model = LevelBuilder.generate(width, length);
    }

    private void finalizePlane() {
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
        cam.position.set(0f, 50f, 50f);
        cam.lookAt(0, 0, -50);
        cam.near = 1f;
        cam.far = 1500f;
        cam.update();

        camController = new CameraInputController(cam);
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

        for (GameObject obj : instances) {
            obj.getBody().getWorldTransform(obj.transform);
        }

        modelBatch.begin(cam);
        modelBatch.render(instances, lights);
        modelBatch.end();

        stage.act(delta);
        stage.draw();
    }

    private void reset() {
        model.dispose();
        model = null;

        for (GameObject obj : instances)
            obj.dispose();
        instances.clear();

        for (GameObject.Constructor constructor : constructors.values())
            constructor.dispose();
        constructors.clear();

        createPhysicsWorld();
        createPlane(width, length);
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

    private Runnable createAddRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                showMenus(true, "Transform", "Scale");
                showMenus(false);
            }
        };
    }

    private TextField.TextFieldListener createTextListener(final Modifier modifier, final String field, final Class clazz) {
        return new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                try {
                    Object val = null;
                    if (clazz == String.class) {
                        val = new String(textField.getText());
                    } else if (clazz == Float.class) {
                        val = Float.valueOf(textField.getText());
                    }
                    modifier.params.put(field, val);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
    }

    private boolean applyModifiers(Group group) {
        reset();
        for (Modifier existingModifier : modifiers) {
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

        return true;
    }

    private void showError(Group group, String message) {
        Label errorLabel = new Label(message, skin, "default");
        errorLabel.setColor(Color.WHITE);
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

    private Runnable createSlopeModifierRunnable(final ModifierType type, final Modifier modToEdit) {
        return new Runnable() {
            @Override
            public void run() {
                final Modifier modifier;
                if (modToEdit != null) {
                    modifier = modToEdit;
                } else {
                    modifier = new Modifier(type);
                    modifiers.add(modifier);
                }
                float height = Gdx.graphics.getHeight();
                float width = Gdx.graphics.getWidth();
                final Group group = new Group();
                group.setBounds(0, 0, width, height);
                stage.addActor(group);

                float y = height * 0.9f;
                final TextField evalAxisTextField = createLabelWithTextField(group, "eval axis", y, width);
                evalAxisTextField.setTextFieldListener(createTextListener(modifier, EVAL_AXIS, String.class));
                setText(evalAxisTextField, (String) modifier.params.get(EVAL_AXIS));

                y -= height * 0.07f;
                final TextField impactAxisTextField = createLabelWithTextField(group, "impact axis", y, width);
                impactAxisTextField.setTextFieldListener(createTextListener(modifier, IMPACT_AXIS, String.class));
                setText(impactAxisTextField, (String) modifier.params.get(IMPACT_AXIS));

                y -= height * 0.07f;
                final TextField startTextField = createLabelWithTextField(group, "start (0.0 - 1.0)", y, width);
                startTextField.setTextFieldListener(createTextListener(modifier, EVAL_AXIS_START_RATIO, Float.class));
                setText(startTextField, (Float) modifier.params.get(EVAL_AXIS_START_RATIO));

                y -= height * 0.07f;
                final TextField durationTextField = createLabelWithTextField(group, "duration (0.0 - 1.0)", y, width);
                durationTextField.setTextFieldListener(createTextListener(modifier, EVAL_AXIS_INTERPOLATION_DURATION, Float.class));
                setText(durationTextField, (Float) modifier.params.get(EVAL_AXIS_INTERPOLATION_DURATION));

                y -= height * 0.07f;
                final TextField impactAmountTextField = createLabelWithTextField(group, "impact amount", y, width);
                impactAmountTextField.setTextFieldListener(createTextListener(modifier, IMPACT_AMOUNT, Float.class));
                setText(impactAmountTextField, (Float) modifier.params.get(IMPACT_AMOUNT));

                y -= height * 0.07f;
                Array<InterpolationChoice> choices = new Array<InterpolationChoice>();
                for (InterpolationChoice choice : InterpolationChoice.values()) {
                    choices.add(choice);
                }

                modifier.params.put(INTERPOLATION, choices.get(0));
                final SelectBox<InterpolationChoice> interpolationSelectBox = createLabelWithSelectBox(group, "interpolation", y, width, choices);
                interpolationSelectBox.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        modifier.params.put(INTERPOLATION, interpolationSelectBox.getSelected().getValue());
                    }
                });
                if (modifier.params.containsKey(INTERPOLATION)) {
                    interpolationSelectBox.setSelected((InterpolationChoice) modifier.params.get(INTERPOLATION));
                }

                y -= height * 0.07f;

                {
                    Label label = new Label("save", skin, "default");
                    label.setColor(Color.WHITE);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.4f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (applyModifiers(group)) {
                                group.remove();
                                showMenus(true, "Add", "Reset");
                                showLeftMenus();
                            }
                        }
                    });
                }

                {
                    Label label = new Label("cancel", skin, "default");
                    label.setColor(Color.WHITE);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, "Add", "Reset");
                            showLeftMenus();
                        }
                    });
                }

                y -= height * 0.07f;

                if (modToEdit != null) {
                    {
                        Label label = new Label("delete", skin, "default");
                        label.setColor(Color.WHITE);
                        BitmapFont.TextBounds bounds = label.getTextBounds();
                        label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                        group.addActor(label);

                        label.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                group.remove();
                                modifiers.remove(modToEdit);
                                applyModifiers(group);
                                showMenus(true, "Add", "Reset");
                                showLeftMenus();
                            }
                        });
                    }
                }
            }
        };
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
                    Label label = new Label("Are you sure?", skin, "default");
                    label.setColor(Color.WHITE);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);
                }
                y -= height * 0.1f;
                {
                    Label label = new Label("ok", skin, "default");
                    label.setColor(Color.WHITE);
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
                            modifiers.clear();

                            group.remove();
                            showMenus(true, "New");
                        }
                    });
                }
                {
                    Label label = new Label("cancel", skin, "default");
                    label.setColor(Color.WHITE);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.6f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            group.remove();
                            showMenus(true, "Add", "Reset");
                            showLeftMenus();
                        }
                    });
                }
            }
        };
    }

    private void showLeftMenus() {
        String[] strings = new String[modifiers.size()];
        for (int i = 0; i < modifiers.size(); ++i) {
            strings[i] = i + ": " + modifiers.get(i).toString();
        }
        showMenus(false, strings);
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

                y -= height * 0.1f;
                final TextField lengthTextField = createLabelWithTextField(group, "length", y, width);

                y -= height * 0.1f;

                {
                    Label label = new Label("ok", skin, "default");
                    label.setColor(Color.WHITE);
                    BitmapFont.TextBounds bounds = label.getTextBounds();
                    label.setBounds(width * 0.5f - bounds.width * 0.5f, y, bounds.width, bounds.height);
                    group.addActor(label);

                    label.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            createPlane(Integer.valueOf(widthTextField.getText()), Integer.valueOf(lengthTextField.getText()));
                            finalizePlane();
                            group.remove();
                            showMenus(true, "Add", "Reset");
                        }
                    });
                }
            }
        };
    }

    private TextField createLabelWithTextField(Group group, String labelText, float y, float width) {
        Label label = new Label(labelText, skin, "default");
        label.setColor(Color.WHITE);
        BitmapFont.TextBounds bounds = label.getTextBounds();
        label.setBounds(width * 0.48f - bounds.width, y, bounds.width, bounds.height);
        group.addActor(label);

        TextField textField = new TextField("", skin, "default");
        textField.setBounds(width * 0.5f, y - 5, width * 0.2f, bounds.height + 10);
        group.addActor(textField);

        return textField;
    }

    private SelectBox<InterpolationChoice> createLabelWithSelectBox(Group group, String labelText, float y, float width, Array<InterpolationChoice> items) {
        Label label = new Label(labelText, skin, "default");
        label.setColor(Color.WHITE);
        BitmapFont.TextBounds bounds = label.getTextBounds();
        label.setBounds(width * 0.48f - bounds.width, y, bounds.width, bounds.height);
        group.addActor(label);

        SelectBox<InterpolationChoice> selectBox = new SelectBox<InterpolationChoice>(skin, "default");
        selectBox.setItems(items);
        selectBox.setBounds(width * 0.5f, y - 5, width * 0.2f, bounds.height + 10);
        group.addActor(selectBox);

        return selectBox;
    }

    public enum ModifierType {
        TRANSFORM, SCALE
    }

    public static class Modifier {
        public ModifierType type;
        public Map<String, Object> params = new HashMap<String, Object>();

        public Modifier(ModifierType type) {
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