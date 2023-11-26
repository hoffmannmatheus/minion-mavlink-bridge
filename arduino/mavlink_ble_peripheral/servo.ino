/*
  PWM input handling.

  Assuming that the flight controller is connect to this Arduino (FC servo output -> arduino PWM input),
  this file will read changes to that PWM input.
  
  This is meant to act as a "camera shutter trigger", so we notify the main file if the state changed from
  low -> high, but not otherwise.
*/

// Definitions
#define SERVO_INPUT_PIN    A3     // Pin where the FC servo is connected
#define PWM_HIGH_THRESHOLD 1400   // Threshold PWM value considered "servo is active"

// State
bool last_pwm_high_state = false;

void servoSetup() {
  pinMode(SERVO_INPUT_PIN, INPUT);
}

void servoLoop() {
  int pwm = pulseIn(SERVO_INPUT_PIN, HIGH, 1000000);
  bool is_pwm_high = pwm > PWM_HIGH_THRESHOLD;

  if(is_pwm_high != last_pwm_high_state) {
    last_pwm_high_state = is_pwm_high;
    //Serial.println("Is PWM high: " + String(last_pwm_high_state));
    if (last_pwm_high_state) { // only notify if activating
      on_trigger_camera();
    }
  }
}
