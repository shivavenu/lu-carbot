#include <Servo.h>

// recall that 0 is 48, 1 is 49, etc...

/**
 * Variables representing the two servos
 */
Servo servoR;
Servo servoL;
int state;
void setup() {
  Serial.begin(9600);
  servoR.attach(8);
  servoL.attach(10);

  pinMode(13, OUTPUT);
  state = 0;
}

void loop() {
  byte a = (byte)Serial.read();
  switch (a) {
  case 48: // 0 in other system
    servoR.write(90);
    servoL.write(90);
    state = !state;
    break;
  case 49: // 1 in other system
    servoR.write(180);
    servoL.write(0);
    state = !state;
    break;
  case 50: // 2 in other system
    servoR.write(0);
    servoL.write(180);
    state = !state;
    break;    
  case 51: // 3 in other system
    servoR.write(180);
    servoL.write(180);
    state = !state;
    break;
  case 52: // 4 in other system
    servoR.write(0);
    servoL.write(0);
    state = !state;
    break;
  case 53: // 5 in other system
    servoL.write(0);
    state = !state;
    break;
  case 54: // 6 in other system
    servoR.write(180);
    state = !state;
    break;
  }

  if (state == 1)
    digitalWrite(13, HIGH);
  else
    digitalWrite(13, LOW);
}
