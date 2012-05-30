/*
  Sends sensor data to Arduino
  (needs SensorGraph and Amarino app installed and running on Android)
*/
 
#include <MeetAndroid.h>
#include <Servo.h>

MeetAndroid meetAndroid;
int sensor = 5;
Servo servoR;

void setup()  
{
  // use the baud rate your bluetooth module is configured to 
  // not all baud rates are working well, i.e. ATMEGA168 works best with 57600
  Serial.begin(38400); 
  servoR.attach(22);
  // we initialize analog pin 5 as an input pin
  pinMode(sensor, INPUT);
}

void loop()
{
  meetAndroid.receive(); // you need to keep this in your loop() to receive events
  
  int msg = meetAndroid.getInt();
  
  switch (msg){
      case 0x0:
        servoR.write(90);
        break;
      }
  // read input pin and send result to Android
  //meetAndroid.send(analogRead(sensor));
  
  // add a little delay otherwise the phone is pretty busy
  delay(100);
}


