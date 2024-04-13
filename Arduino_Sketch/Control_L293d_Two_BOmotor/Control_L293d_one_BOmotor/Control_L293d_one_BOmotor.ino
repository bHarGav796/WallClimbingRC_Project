// Motor control pins
const int urmw1 = 2; // Motor driver input 1 (e.g., L298N IN1)
const int urmw2 = 3; // Motor driver input 2 (e.g., L298N IN2)
const int dlmw1 = 8; // Motor driver input 3 
const int dlmw2 = 9; // Motor driver input 4 

void setup() {
  // Set the motor control pins as outputs
  pinMode(urmw1, OUTPUT);
  pinMode(urmw2, OUTPUT);
  pinMode(dlmw1, OUTPUT);
  pinMode(dlmw2, OUTPUT);
  
  // Initialize the serial communication for Bluetooth
  Serial.begin(9600);
}

void loop() {
  if (Serial.available()) {
    char command = Serial.read();
    switch (command) {
      case 'F': // Forward
        digitalWrite(urmw1, HIGH);
        digitalWrite(urmw2, LOW);
        digitalWrite(dlmw1, HIGH);
        digitalWrite(dlmw2, LOW);
        break;
                  
      case 'B': // Backward
        digitalWrite(urmw1, LOW);
        digitalWrite(urmw2, HIGH);
        digitalWrite(dlmw1, LOW);
        digitalWrite(dlmw2, HIGH);
        break;
      case 'S': // Stop
        digitalWrite(urmw1, LOW);
        digitalWrite(urmw2, LOW);
        digitalWrite(dlmw1, LOW);
        digitalWrite(dlmw2, LOW);
        break;
  
       case 'L': // Left
        digitalWrite(urmw1, HIGH);
        digitalWrite(urmw2, LOW);
        digitalWrite(dlmw1, LOW);
        digitalWrite(dlmw2, LOW);
        break;
    
       case 'R': // Left
        digitalWrite(urmw1, LOW);
        digitalWrite(urmw2, LOW);
        digitalWrite(dlmw1, HIGH);
        digitalWrite(dlmw2, LOW);
        break;
    }
  }
}
