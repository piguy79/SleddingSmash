package com.railwaygames.sleddingsmash.entity;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;

/**
 * Created by conormullen on 2/13/15.
 */
public class GameObject extends ModelInstance implements Disposable {

    private btRigidBody body;

    public GameObject(Model model, btRigidBody.btRigidBodyConstructionInfo constructionInfo) {
        super(model);
        body = new btRigidBody(constructionInfo);
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
        public final btCollisionShape shape;
        public final btRigidBody.btRigidBodyConstructionInfo constructionInfo;
        private static Vector3 localInertia = new Vector3();

        public Constructor (Model model, btCollisionShape shape, float mass) {
            this.model = model;
            this.shape = shape;
            if (mass > 0f)
                shape.calculateLocalInertia(mass, localInertia);
            else
                localInertia.set(0, 0, 0);
            this.constructionInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, null, shape, localInertia);
        }

        public GameObject construct () {
            return new GameObject(model, constructionInfo);
        }

        @Override
        public void dispose () {
            shape.dispose();
            constructionInfo.dispose();
        }
    }
}
