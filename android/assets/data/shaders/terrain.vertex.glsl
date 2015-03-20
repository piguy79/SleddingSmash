#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(fogFlag)
#define cameraPositionFlag
#endif

attribute vec3 a_position;
uniform mat4 u_projViewTrans;

#ifdef normalFlag
attribute vec3 a_normal;
uniform mat3 u_normalMatrix;
varying vec3 v_normal;
#endif // normalFlag

#ifdef textureFlag
attribute vec2 a_texCoord0;
#endif // textureFlag

#ifdef diffuseTextureFlag
uniform vec4 u_diffuseUVTransform;
varying vec2 v_diffuseUV;
#endif

uniform mat4 u_worldTrans;

#ifdef blendedFlag
uniform float u_opacity;
varying float v_opacity;

#ifdef alphaTestFlag
uniform float u_alphaTest;
varying float v_alphaTest;
#endif //alphaTestFlag
#endif // blendedFlag

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#ifdef ambientLightFlag
uniform vec3 u_ambientLight;
#endif // ambientLightFlag

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag

#ifdef cameraPositionFlag
uniform vec4 u_cameraPosition;
#endif // cameraPositionFlag

#ifdef fogFlag
varying float v_fog;
#endif // fogFlag


#if defined(numDirectionalLights) && (numDirectionalLights > 0)
struct DirectionalLight
{
	vec3 color;
	vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif // numDirectionalLights

#if defined(numPointLights) && (numPointLights > 0)
struct PointLight
{
	vec3 color;
	vec3 position;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif // lightingFlag

uniform vec3 u_worldMin;
uniform vec3 u_worldMax;

void main() {
	#ifdef diffuseTextureFlag
	    float z_len = u_worldMax.z - u_worldMin.z;
	    float z_pos = (u_worldMax.z - a_position.z) / z_len;
	    float z_start = 0.94;
	    float z_end = 0.96;
		if(z_pos > z_start && z_pos < z_end) {
		    float x_len = (u_worldMax.x - u_worldMin.x) * 0.1;
		    float u = mod(a_position.x, x_len) / x_len;
		    float v = (z_pos - z_start) / (z_end - z_start);
		    v_diffuseUV = vec2(u, v);
		} else {
		    v_diffuseUV = vec2(1.0);
		}
	#endif //diffuseTextureFlag
		
	#ifdef blendedFlag
		v_opacity = u_opacity;
		#ifdef alphaTestFlag
			v_alphaTest = u_alphaTest;
		#endif //alphaTestFlag
	#endif // blendedFlag

	vec4 pos = u_worldTrans * vec4(a_position, 1.0);
		
	gl_Position = u_projViewTrans * pos;
	
	#if defined(normalFlag)
		vec3 normal = normalize(u_normalMatrix * a_normal);
		v_normal = normal;
	#endif // normalFlag

    #ifdef fogFlag
        vec3 flen = u_cameraPosition.xyz - pos.xyz;
        float fog = dot(flen, flen) * u_cameraPosition.w;
        v_fog = min(fog, 1.0);
    #endif

	#ifdef lightingFlag
		#if	defined(ambientLightFlag)
        	vec3 ambientLight = u_ambientLight;
		#elif defined(ambientFlag)
        	vec3 ambientLight = vec3(0.0);
		#endif

		#ifdef ambientCubemapFlag
			vec3 squaredNormal = normal * normal;
			vec3 isPositive  = step(0.0, normal);
			ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
					squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
					squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
		#endif // ambientCubemapFlag

		#ifdef ambientFlag
			#ifdef separateAmbientFlag
				v_ambientLight = ambientLight;
				v_lightDiffuse = vec3(0.0);
			#else
				v_lightDiffuse = ambientLight;
			#endif //separateAmbientFlag
		#else
	        v_lightDiffuse = vec3(0.0);
		#endif //ambientFlag
			
		#if defined(numDirectionalLights) && (numDirectionalLights > 0) && defined(normalFlag)
			for (int i = 0; i < numDirectionalLights; i++) {
				vec3 lightDir = -u_dirLights[i].direction;
				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
				vec3 value = u_dirLights[i].color * NdotL;
				v_lightDiffuse += value;
			}
		#endif // numDirectionalLights

		#if defined(numPointLights) && (numPointLights > 0) && defined(normalFlag)
			for (int i = 0; i < numPointLights; i++) {
				vec3 lightDir = u_pointLights[i].position - pos.xyz;
				float dist2 = dot(lightDir, lightDir);
				lightDir *= inversesqrt(dist2);
				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
				vec3 value = u_pointLights[i].color * (NdotL / (1.0 + dist2));
				v_lightDiffuse += value;
			}
		#endif // numPointLights
	#endif // lightingFlag
}
