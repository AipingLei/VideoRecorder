precision highp float;

varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;

uniform highp vec2 controlPointA;
uniform highp vec2 controlPointB;
uniform highp float radius;
uniform highp float angle;
uniform highp float aspectRatio;


highp vec2 thinFacePositionToUse(highp vec2 currentPoint, highp vec2 curControlPointA,  highp vec2 curControlPointB, highp float curRadius, highp float delta, highp float curAspectRatio)
{
     highp vec2 positionToUse = currentPoint;
     
     highp vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * curAspectRatio + 0.5 - 0.5 * curAspectRatio);
     highp vec2 contourPointAToUse = vec2(curControlPointA.x, curControlPointA.y * curAspectRatio + 0.5 - 0.5 * curAspectRatio);
     
     highp float r = distance(currentPointToUse, contourPointAToUse);
     if(r < curRadius)
     {
         highp vec2 dir = normalize(curControlPointB - curControlPointA);
         highp float dist = curRadius * curRadius - r * r;
         highp float alpha = dist / (dist + (r-delta) * (r-delta));
         alpha = alpha * alpha;
         
         positionToUse = positionToUse - alpha * delta * dir;
     }
     
     return positionToUse;
}

void main()
{
	highp vec2 positionToUse = textureCoordinate;
    highp vec2 controlPointATemp = vec2(0.58, 0.8);
	highp vec2 controlPointBTemp = vec2(0.42, 0.8);
	highp float radiusTemp = 1.0;
	highp float angleTemp = 0.06;
	highp float aspectRatioTemp = 2.0;
	
    positionToUse = thinFacePositionToUse(positionToUse, controlPointATemp, controlPointBTemp, radiusTemp, angleTemp, aspectRatioTemp);
    positionToUse = thinFacePositionToUse(positionToUse, controlPointBTemp, controlPointATemp, radiusTemp, angleTemp, aspectRatioTemp);
	gl_FragColor = texture2D(inputImageTexture, positionToUse);
	
}