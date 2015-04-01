package com.railwaygames.sleddingsmash.levels.obstacles;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.railwaygames.sleddingsmash.Constants;
import com.railwaygames.sleddingsmash.entity.GameObject;

import java.util.Map;

/**
 * Created by conormullen on 3/30/15.
 */
public class StarObstacleGenerator extends ObstacleGenerator {

    public StarObstacleGenerator(Model model) {
        super(model);
    }

    @Override
    GameObject placeObstacle(Vector3 vector, Vector3 offset, Map<String, Object> params) {
        btSphereShape sphere = new btSphereShape(5);
        GameObject.Constructor constructor = new GameObject.Constructor(model, GameObject.GameObjectType.STAR, sphere, 0);
        GameObject star = constructor.construct();

        float distanceFromGround = (Float) params.get(HEIGHT_FROM_GROUND) == null ? 5 : (Float) params.get(HEIGHT_FROM_GROUND);

        star.setPosition(vector);
        Vector3 position = new Vector3(offset.x + vector.x, vector.y + distanceFromGround, vector.z);
        star.transform.setToTranslation(position);
        star.transform.rotate(new Vector3(1f, 0, 0), 90);
        star.transform.scale(3,3,3);
        Matrix4 starPosition = star.transform.cpy();
        star.getBody().setWorldTransform(starPosition);
        star.getBody().setContactCallbackFlag(Constants.CollisionsFlag.STAR_FLAG);
        star.getBody().setContactCallbackFilter(Constants.CollisionsFlag.SPHERE_FLAG);

        return star;
    }
}
