/*
  PWM input handling.

  Assuming that the flight controller is connect to this Arduino (FC servo output -> arduino PWM input),
  this file will read changes to that PWM input.
  
  This is meant to act as a "camera shutter trigger", so we notify the main file if the state changed from
  low (800) -> high (2200), but not otherwise.
*/

// Definitions
#define PWM_READ_WINDOW               400 // Milliseconds interval of PWM window.
#define PWM_HIGH_PERCENTAGE_THRESHOLD 6.9 // Minimum percentage of high reads within the window for "high pwm".
                                          // Note 1: Using "pulseIn" would be much better, but the fact that it 
                                          //   interrupts the loop conclicts with ArduinoBLE, and causes bluetooth
                                          //   to stop working altogether.
                                          // Note 2: this value is arbitrary, empirically set! I'm setting this
                                          //   as the midpoint observed between 800 & 2200 PWM values set from the 
                                          //   flight controller. There is certainly a better way to do this, 
                                          //   please yell at me if you know how.
                                          // TODO: Learn to calculate actual PWM values from ditigalRead.
#define SERVO_INPUT_PIN               A3  // Pin where the FC servo is connected.

// State
unsigned long window_read_count = 0;
unsigned long window_high_count = 0;
unsigned long previous_pwm_read_time = 0;  // will store last time MAVLink was transmitted and listened
bool last_pwm_high_state = false;

void servoSetup() {
  pinMode(SERVO_INPUT_PIN, INPUT);
}

void servoLoop() {
  unsigned long current_time = millis();
  if (current_time - previous_pwm_read_time >= PWM_READ_WINDOW) {
    previous_pwm_read_time = current_time;
    notifyIfHigh();

    window_read_count = 0;
    window_high_count = 0;
  }
  
  window_read_count++;
  // Use digitalRead instead of pulseIn. See comment in definitions.
  if (digitalRead(SERVO_INPUT_PIN) == HIGH) {
    window_high_count++;
  }
}

void notifyIfHigh() {
  float percentage = (float(window_high_count) / float(window_read_count)) * 100.0;
  // Serial print to calculate the PWM_HIGH_PERCENTAGE_THRESHOLD while bench testing:
  //Serial.println("Window read. " + String(percentage) + "%. High/Total: " + String(window_high_count) + "/" + String(window_read_count));
  
  bool is_high = percentage > PWM_HIGH_PERCENTAGE_THRESHOLD;
  if(is_high != last_pwm_high_state) {
    last_pwm_high_state = is_high;
    if (last_pwm_high_state) { // only notify if activating
      onTriggerCamera();
    }
  }
}