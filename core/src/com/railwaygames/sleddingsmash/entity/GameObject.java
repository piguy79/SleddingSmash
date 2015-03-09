package com.railwaygames.sleddingsmash.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;


/**
 * Created by conormullen on 2/13/15.
 */
public class GameObject extends ModelInstance implements Disposable {

    private btRigidBody body;
    public GameObjectType gameObjectType;
    public Constructor constructor;
    public Vector3 position;
    public GameObject.MotionState motionState;

    public GameObject(Model model, GameObjectType gameObjectType, btRigidBody.btRigidBodyConstructionInfo constructionInfo, Constructor constructor) {
        super(model);
        this.constructor = constructor;
        this.body = new btRigidBody(constructionInfo);
        this.body.userData = this;
        this.motionState = new MotionState(this.transform);
        ((btRigidBody)this.body).setMotionState(motionState);
    }

    public void setPosition(Vector3 newPosition) {
        this.position = newPosition;
    }

    public Vector3 getLocationInWorld() {
        Vector3 output = new Vector3();
        transform.getTranslation(output);
        return output;
    }

    public float height(){
        BoundingBox box = new BoundingBox();
        calculateBoundingBox(box);
        return box.getHeight();
    }

    public float width(){
        BoundingBox box = new BoundingBox();
        calculateBoundingBox(box);
        return box.getWidth();
    }

    public void applyForce(Vector3 force){
        Vector3 pos = getLocationInWorld();
        Vector3 correctedForce = (pos.add(force));
        getBody().applyCentralForce(correctedForce);
    }



    public btRigidBody getBody() {
        return body;
    }

    @Override
    public void dispose() {
        this.body.dispose();
    }

    public static class Constructor implements Disposable {
        public final Model model;
        public final GameObjectType gameObjectType;
        public final btCollisionShape shape;
        public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
        private static Vector3 localInertia = new Vector3();

        public Constructor(Model model, GameObjectType gameObjectType, btCollisionShape shape, float mass) {
            this.model = model;
            this.gameObjectType = gameObjectType;
            this.shape = shape;
            if (mass > 0f)
                shape.calculateLocalInertia(mass, localInertia);
            else
                localInertia.set(0, 0, 0);
            this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
        }

        public GameObject construct() {
            return new GameObject(model, gameObjectType, constructionInfo, this);
        }

        @Override
        public void dispose() {
            shape.dispose();
            constructionInfo.dispose();
        }
    }

    public enum GameObjectType{
        CHARACTER, TREE, ROCK, PLANE
    }

    static class MotionState extends btMotionState {
        private final Matrix4 transform;

        public MotionState (final Matrix4 transform) {
            this.transform = transform;
        }

        /** For dynamic and static bodies this method is called by bullet once to get the initial state of the body. For kinematic
         * bodies this method is called on every update, unless the body is deactivated. */
        @Override
        public void getWorldTransform (final Matrix4 worldTrans) {
            worldTrans.set(transform);
        }

        /** For dynamic bodies this method is called by bullet every update to inform about the new position and rotation. */
        @Override
        public void setWorldTransform (final Matrix4 worldTrans) {
            transform.set(worldTrans);
        }
    }
}
