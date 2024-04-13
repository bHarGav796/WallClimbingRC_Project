//BLDC motor code
#include<Servo.h>

  Servo esc; //declare motor as esc

void setup() {
  esc.attach(10); //motor signal pin attached to pin 10
  esc.write(30); //initial throttle value of motor
  delay(3000); //delay for 3 sec
 }

void loop() {
    esc.write(100); // Speed of Motor varies from 50-130
}
