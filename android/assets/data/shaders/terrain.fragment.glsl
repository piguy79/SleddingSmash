#ifdef GL_ES
#define LOWP lowp
#define MED mediump
#define HIGH highp
precision mediump float;
#else
#define MED
#define LOWP
#define HIGH
#endif

#ifdef blendedFlag
varying float v_opacity;
#ifdef alphaTestFlag
varying float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#ifdef diffuseTextureFlag
varying MED vec2 v_diffuseUV;
#endif

#ifdef specularTextureFlag
varying MED vec2 v_specularUV;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
varying float v_fog;
#endif // fogFlag

vec4 blendOneMinus(vec4 tex, vec4 color) {
    return vec4(tex.a) * tex + vec4(1.0 - tex.a) * color;
}

void main() {
	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
	    vec4 tex = texture2D(u_diffuseTexture, v_diffuseUV);
		vec4 diffuse = blendOneMinus(tex, u_diffuseColor);
	#elif defined(diffuseTextureFlag)
		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
	#elif defined(diffuseColorFlag)
		vec4 diffuse = u_diffuseColor;
	#else
		vec4 diffuse = vec4(1.0);
	#endif

	#if (!defined(lightingFlag))
		gl_FragColor.rgb = diffuse.rgb;
	#else
		#if defined(ambientFlag) && defined(separateAmbientFlag)
			gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse));
		#else
			gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse);
		#endif
	#endif //lightingFlag

	#ifdef fogFlag
		gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);
	#endif // end fogFlag

	#ifdef blendedFlag
		gl_FragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (gl_FragColor.a <= v_alphaTest)
				discard;
		#endif
	#else
		gl_FragColor.a = 1.0;
	#endif

}
