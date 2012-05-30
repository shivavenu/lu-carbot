#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <Servo.h>

AndroidAccessory acc ("Manufacturer",
"Model",
"Description",
"1.0",
"http://yoursite.com",
"0000000012345678");

Servo servoR;
Servo servoL;
byte msg[1];
void setup(){
  Serial.begin(115200);
  servoR.attach(8);
  servoL.attach(10);
  acc.powerOn();
}

void loop(){
  if (acc.isConnected()){
    int len = acc.read(msg, sizeof(msg), 1);
    if(len > 0){
      switch (msg[0]){
      case 0x0:
        servoR.write(90);
        servoL.write(90);
        break;
      case 0x1:
        servoR.write(180);
        servoL.write(0);
        break;
      case 0x2:
        servoR.write(0);
        servoL.write(180);
        break;
      case 0x3:
        servoR.write(180);
        servoL.write(180);
        break;
      case 0x4:
        servoR.write(0);
        servoL.write(0);
        break;
      case 0x5:
        servoL.write(0);
        break;
      case 0x6:
        servoR.write(180);
        break;
      /*default:
        servoR.write(90);
        servoL.write(90);
        break;*/

      }
    }
  }

}
void forward(){
}
void reverse(){
}
void cw(){
}
void ccw(){
}
void ptr(){
}
void ptl(){
}





