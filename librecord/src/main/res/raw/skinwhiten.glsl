precision highp float;

uniform sampler2D inputImageTexture;
uniform sampler2D curve;

uniform float texelWidthOffset;
uniform float texelHeightOffset;

varying highp vec2 textureCoordinate;


uniform vec2 singleStepOffset; 
uniform highp vec4 params; 

const highp vec3 W = vec3(0.299,0.587,0.114);
const mat3 saturateMatrix = mat3(
		1.1102,-0.0598,-0.061,
		-0.0774,1.0826,-0.1186,
		-0.0228,-0.0228,1.1772);
		
float hardlight(float color)
{
	if(color <= 0.5)
	{
		color = color * color * 2.0;
	}
	else
	{
		color = 1.0 - ((1.0 - color)*(1.0 - color) * 2.0);
	}
	return color;
}

const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);

vec4 gaussianBlur(sampler2D sampler) {
	lowp float strength = 1.;
	vec4 color = vec4(0.);
	vec2 step  = vec2(0.);

	color += texture2D(sampler,textureCoordinate)* 0.25449 ;

	step.x = 1.37754 * texelWidthOffset  * strength;
	step.y = 1.37754 * texelHeightOffset * strength;
	color += texture2D(sampler,textureCoordinate+step) * 0.24797;
	color += texture2D(sampler,textureCoordinate-step) * 0.24797;

	step.x = 3.37754 * texelWidthOffset  * strength;
	step.y = 3.37754 * texelHeightOffset * strength;
	color += texture2D(sampler,textureCoordinate+step) * 0.09122;
	color += texture2D(sampler,textureCoordinate-step) * 0.09122;

	step.x = 5.37754 * texelWidthOffset  * strength;
	step.y = 5.37754 * texelHeightOffset * strength;

	color += texture2D(sampler,textureCoordinate+step) * 0.03356;
	color += texture2D(sampler,textureCoordinate-step) * 0.03356;

	return color;
}

void main() {
	vec4 blurColor;
	lowp vec4 textureColor;
	lowp float strength = -1.0 / 510.0;

	float xCoordinate = textureCoordinate.x;
	float yCoordinate = textureCoordinate.y;

	lowp float satura = 0.7;
	// naver skin
	textureColor = texture2D(inputImageTexture, textureCoordinate);
	blurColor = gaussianBlur(inputImageTexture);

	//saturation
    lowp float luminance = dot(blurColor.rgb, luminanceWeighting);
	lowp vec3 greyScaleColor = vec3(luminance);

	blurColor = vec4(mix(greyScaleColor, blurColor.rgb, satura), blurColor.w);
    
	lowp float redCurveValue = texture2D(curve, vec2(textureColor.r, 0.0)).r;
	lowp float greenCurveValue = texture2D(curve, vec2(textureColor.g, 0.0)).r;
    lowp float blueCurveValue = texture2D(curve, vec2(textureColor.b, 0.0)).r;

	redCurveValue = min(1.0, redCurveValue + strength);
	greenCurveValue = min(1.0, greenCurveValue + strength);
	blueCurveValue = min(1.0, blueCurveValue + strength);

    mediump vec4 overlay = blurColor;

	mediump vec4 base = vec4(redCurveValue, greenCurveValue, blueCurveValue, 1.0);
    //gl_FragColor = overlay;

    // step4 overlay blending
	mediump float ra;
	if (base.r < 0.5) {
		ra = overlay.r * base.r * 2.0;
	} else {
		ra = 1.0 - ((1.0 - base.r) * (1.0 - overlay.r) * 2.0);
	}

    mediump float ga;
	if (base.g < 0.5) {
		ga = overlay.g * base.g * 2.0;
	} else {
		ga = 1.0 - ((1.0 - base.g) * (1.0 - overlay.g) * 2.0);
	}

	mediump float ba;
	if (base.b < 0.5) {
		ba = overlay.b * base.b * 2.0;
	} else {
		ba = 1.0 - ((1.0 - base.b) * (1.0 - overlay.b) * 2.0);
	}

	textureColor = vec4(ra, ga, ba, 1.0);

    //gl_FragColor = vec4(textureColor.r, textureColor.g, textureColor.b, 1.0);
    
    
    /////////////
    vec2 blurCoordinates[12];
	
	blurCoordinates[0] = textureCoordinate.xy + singleStepOffset * vec2(5.0, -8.0);
	blurCoordinates[1] = textureCoordinate.xy + singleStepOffset * vec2(5.0, 8.0);
	blurCoordinates[2] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, 8.0);
	blurCoordinates[3] = textureCoordinate.xy + singleStepOffset * vec2(-5.0, -8.0);
	
	blurCoordinates[4] = textureCoordinate.xy + singleStepOffset * vec2(8.0, -5.0);
	blurCoordinates[5] = textureCoordinate.xy + singleStepOffset * vec2(8.0, 5.0);
	blurCoordinates[6] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, 5.0);	
	blurCoordinates[7] = textureCoordinate.xy + singleStepOffset * vec2(-8.0, -5.0);
	
	blurCoordinates[8] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, -4.0);
	blurCoordinates[9] = textureCoordinate.xy + singleStepOffset * vec2(-4.0, 4.0);
	blurCoordinates[10] = textureCoordinate.xy + singleStepOffset * vec2(4.0, -4.0);
	blurCoordinates[11] = textureCoordinate.xy + singleStepOffset * vec2(4.0, 4.0);
	
	float sampleColor = texture2D(inputImageTexture, textureCoordinate).g * 22.0;

	sampleColor += texture2D(inputImageTexture, blurCoordinates[0]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[1]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[2]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[3]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[4]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[5]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[6]).g;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[7]).g;
	
	sampleColor += texture2D(inputImageTexture, blurCoordinates[8]).g * 2.0;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[9]).g * 2.0;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[10]).g * 2.0;
	sampleColor += texture2D(inputImageTexture, blurCoordinates[11]).g * 2.0;	
	
	sampleColor = sampleColor / 38.0;
	
	vec3 centralColor = texture2D(inputImageTexture, textureCoordinate).rgb;
	
	float highpass = centralColor.g - sampleColor + 0.5;
	
	for(int i = 0; i < 5;i++)
	{
		highpass = hardlight(highpass);
	}
	float lumance = dot(centralColor, W);
	
	float alpha = pow(lumance, params.r);

	vec3 smoothColor = centralColor + (centralColor-vec3(highpass))*alpha*0.1;
	
	smoothColor.r = clamp(pow(smoothColor.r, params.g),0.0,1.0);
	smoothColor.g = clamp(pow(smoothColor.g, params.g),0.0,1.0);
	smoothColor.b = clamp(pow(smoothColor.b, params.g),0.0,1.0);
	
	vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);
	//vec3 bianliang = max(smoothColor, centralColor);
	vec3 rouguang = 2.0*centralColor*smoothColor + centralColor*centralColor - 2.0*centralColor*centralColor*smoothColor;
	
	
	vec4 tempColor = vec4(mix(centralColor, lvse, alpha), 1.0);
	
	gl_FragColor = mix(tempColor, textureColor, gl_FragColor);
	//gl_FragColor = vec4(mix(centralColor, lvse, alpha), 1.0);
	//gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, alpha);
	gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, params.b);
}
