#include <Servo.h>

/**
 * Variables representing the two servos
 */
Servo servoR;
Servo servoL;
int state;
void setup() {
  Serial.begin(115200);
  servoR.attach(8);
  servoL.attach(10);

  pinMode(13, OUTPUT);
  state = 0;
}

void loop() {
  byte a = (byte)Serial.read();
  switch (a) {
  case 0x2:
    servoR.write(90);
    servoL.write(90);
    state = !state;
    break;
  case 0x1:
    servoR.write(180);
    servoL.write(0);
    state = !state;
    break;
  case 0x3:
    servoR.write(180);
    servoL.write(180);
    state = !state;
    break;
  case 0x4:
    servoR.write(0);
    servoL.write(0);
    state = !state;
    break;
  case 0x5:
    servoL.write(0);
    state = !state;
    break;
  case 0x6:
    servoR.write(180);
    state = !state;
    break;
    /*default:
     servoR.write(90);
     servoL.write(90);
     break;*/
  }

  if (state == 1)
    digitalWrite(13, HIGH);
  else
    digitalWrite(13, LOW);
}
