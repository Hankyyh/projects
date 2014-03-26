#pragma strict

function Start () {

}

var defaultCameraAngle : float=60;
@HideInInspector
var currentTargetCameraAngle : float = 60;
@HideInInspector
var racioZoom : float = 1;
@HideInInspector
var racioZoomV : float;
var racioZoomSpeed : float = 0.2;

var lookSensitivity:float=5;
@HideInInspector
var yRotation:float;
@HideInInspector
var xRotation:float;
@HideInInspector
var currentYRotation:float;
@HideInInspector
var currentXRotation:float;
@HideInInspector
var yRotationV:float;
@HideInInspector
var xRotationV:float;
var lookSmoothDamp:float=0.1;
@HideInInspector
var currentAimRacio : float = 1;

function Update () {

	if(currentAimRacio == 1)
		racioZoom = Mathf.SmoothDamp(racioZoom, 1, racioZoomV,racioZoomSpeed);
	else
		racioZoom = Mathf.SmoothDamp(racioZoom, 0, racioZoomV,racioZoomSpeed);
	
	camera.fieldOfView = Mathf.Lerp(currentTargetCameraAngle, defaultCameraAngle,racioZoom);

	yRotation += Input.GetAxis("Mouse X") * lookSensitivity * currentAimRacio;
	xRotation -= Input.GetAxis("Mouse Y") * lookSensitivity * currentAimRacio;
	
	xRotation = Mathf.Clamp(xRotation,-90,90);
	
	currentXRotation = Mathf.SmoothDamp(currentXRotation,xRotation,xRotationV,lookSmoothDamp);
	currentYRotation = Mathf.SmoothDamp(currentYRotation,yRotation,yRotationV,lookSmoothDamp);
	
	transform.rotation = Quaternion.Euler(currentXRotation,currentYRotation,0);
}