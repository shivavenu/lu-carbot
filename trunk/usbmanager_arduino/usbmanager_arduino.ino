#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <Servo.h>

AndroidAccessory acc ("Lehigh University",
"Carbot V1",
"Carbot with Wood Chassis",
"1.0",
"http://code.google.com/p/lu-carbot",
"0000000012345678");

/**
 * Variables representing the two servos
 */
Servo servoR;
Servo servoL;

/**
 * A buffer for holding the data received from the android phone
 */
byte msg[1];

/**
 * To configure, turn on the serial port, attach servos to ports 8 and 10, and turn on the accessory
 */
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

