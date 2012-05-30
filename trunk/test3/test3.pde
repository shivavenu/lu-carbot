/*
  Multicolor Lamp (works with Amarino and the MultiColorLamp Android app)
 
 - based on the Amarino Multicolor Lamp tutorial
 - receives custom events from Amarino changing color accordingly
 
 author: Bonifaz Kaufmann - December 2009
 */

#include <MeetAndroid.h>
#include <Servo.h>

// declare MeetAndroid so that you can call functions with it
MeetAndroid meetAndroid;

Servo servoR;

void setup()  
{
  // use the baud rate your bluetooth module is configured to 
  // not all baud rates are working well, i.e. ATMEGA168 works best with 57600
  Serial.begin(38400); 
  servoR.attach(22);
  meetAndroid.registerFunction(rot, 'r'); 

}

void loop()
{
  meetAndroid.receive(); // you need to keep this in your loop() to receive events
  delay(100);
}

void rot(byte flag, byte numOfValues){
  switch (meetAndroid.getInt()){
  case 0:
    servoR.write(90);
    //servoL.write(90);
    break;
  case 1:
    servoR.write(180);
    //servoL.write(0);
    break;
  case 2:
    servoR.write(0);
    //servoL.write(180);
    break;
  case 3:
    servoR.write(180);
    //servoL.write(180);
    break;
  case 4:
    servoR.write(0);
    //servoL.write(0);
    break;
  case 5:
    //servoL.write(0);
    break;
  case 6:
    servoR.write(180);
    break;

  }
}


