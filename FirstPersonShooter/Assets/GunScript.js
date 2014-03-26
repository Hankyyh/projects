#pragma strict

function Start () {

}

var cameraObject : GameObject;
@HideInInspector
var targetXRotation : float;
@HideInInspector
var targetYRotation : float;
@HideInInspector
var targetXRotationV : float;
@HideInInspector
var targetYRotationV : float;

var rotateSpeed : float = 0.3;

var holdHeight : float = -0.5;
var holdSide : float = 0.5;
var racioHipHold : float = 1;
var hipToAimSpeed : float = 0.1;
@HideInInspector
var racioHipHoldV : float;

var aimRacio : float = 0.4;

var zoomAngle : float = 30;

var fireSpeed : float = 15;
@HideInInspector
var waitTilNextFire : float = 0;
var bullet : GameObject;
var bulletSpawn : GameObject;


function Update () {

	if(Input.GetButton("Fire1")){
		if(waitTilNextFire <= 0){
			if(bullet){
				Instantiate(bullet, bulletSpawn.transform.position, bulletSpawn.transform.rotation);
				}
			waitTilNextFire = 1;
		}
	}
	waitTilNextFire -= Time.deltaTime * fireSpeed;

	cameraObject.GetComponent(MouseLookScript).currentTargetCameraAngle = zoomAngle;

	if(Input.GetButton("Fire2")){
		cameraObject.GetComponent(MouseLookScript).currentAimRacio = aimRacio;
		racioHipHold = Mathf.SmoothDamp(racioHipHold,0,racioHipHoldV,hipToAimSpeed);
	}else{
		cameraObject.GetComponent(MouseLookScript).currentAimRacio = 1;
		racioHipHold = Mathf.SmoothDamp(racioHipHold,1,racioHipHoldV,hipToAimSpeed);
	}
	
	transform.position = cameraObject.transform.position + (Quaternion.Euler(0,targetYRotation,0) * Vector3(holdSide * racioHipHold,holdHeight * racioHipHold, 0));
	targetXRotation = Mathf.SmoothDamp(targetXRotation, cameraObject.GetComponent(MouseLookScript).xRotation, targetXRotationV,rotateSpeed);
	targetYRotation = Mathf.SmoothDamp(targetYRotation, cameraObject.GetComponent(MouseLookScript).yRotation, targetYRotationV,rotateSpeed);
	
	transform.rotation = Quaternion.Euler(targetXRotation, targetYRotation, 0);
}