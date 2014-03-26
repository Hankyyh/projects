#pragma strict

function Start () {

}

var walkAcceleration : float = 5;
var walkAccelAirRacio : float = 0.1;
var walkDeacceleration : float = 5;
@HideInInspector
var walkDeaccelerationVolx : float;
@HideInInspector
var walkDeaccelerationVolz : float;

var cameraObject : GameObject;
var maxWalkSpeed : float = 20;
@HideInInspector
var horizontalMovement : Vector2;

var jumpVelocity : float =20;
@HideInInspector
var grounded : boolean = false;
var maxSlope : float = 60;

function Update () {
	
	horizontalMovement = Vector2(rigidbody.velocity.x,rigidbody.velocity.z);
	if(horizontalMovement.magnitude > maxWalkSpeed){
		horizontalMovement = horizontalMovement.normalized;
		horizontalMovement *= maxWalkSpeed;
	}
	rigidbody.velocity.x = horizontalMovement.x;
	rigidbody.velocity.z = horizontalMovement.y;
	
	if(grounded){
		rigidbody.velocity.x = Mathf.SmoothDamp(rigidbody.velocity.x,0,walkDeaccelerationVolx,walkDeacceleration);
		rigidbody.velocity.z = Mathf.SmoothDamp(rigidbody.velocity.z,0,walkDeaccelerationVolz,walkDeacceleration);
	}
	
	transform.rotation = Quaternion.Euler(0,cameraObject.GetComponent(MouseLookScript).currentYRotation,0);
	
	if(grounded){
		rigidbody.AddRelativeForce(Input.GetAxis("Horizontal") * walkAcceleration * Time.deltaTime,0,Input.GetAxis("Vertical") * walkAcceleration * Time.deltaTime);
	}else{
		rigidbody.AddRelativeForce(Input.GetAxis("Horizontal") * walkAcceleration * walkAccelAirRacio * Time.deltaTime,0,Input.GetAxis("Vertical") * walkAcceleration * walkAccelAirRacio * Time.deltaTime);
	}
	
	if(Input.GetButtonDown("Jump") && grounded){
		rigidbody.AddForce(0,jumpVelocity,0);
	}
}

function OnCollisionStay(collision : Collision){
	
	for(var contact: ContactPoint in collision.contacts){
		if(Vector3.Angle(contact.normal,Vector3.up) < maxSlope)
			grounded = true;
	}

}

function OnCollisionExit(){
	grounded = false;
}