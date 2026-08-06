#include <Wire.h>
#include <Adafruit_BMP085.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// I2C Pin Definitions for ESP32 MYOSA
#define I2C_SDA 21
#define I2C_SCL 22

// I2C Addresses
#define MPU6050_ADDR 0x69
#define OLED_ADDR    0x3C  // Standard I2C address for 0.96" OLED

// OLED Screen Setup
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// Buzzer Pin Definition
#define BUZZER_PIN 25

Adafruit_BMP085 bmp;

// Calibration & Baseline Variables
float baseline_pressure = 0.0;
float baseline_tilt = 0.0;
float current_tilt = 0.0;
float current_pressure = 0.0;

// ================================================================
// THRESHOLD CONFIGURATION (PHYSICAL LANDSLIDE SCALING)
// ================================================================

// DEMO MODE (For desktop presentation via air blowing & hand tilting)
const float TILT_THRESHOLD_DEG     = 6.0;   // Scaled down from 10.0° to 6.0°
const float PRESSURE_THRESHOLD_HPA = 0.4;   // Scaled down from 1.5 hPa storm drop to 0.4 hPa
const int   SUSTAIN_COUNT_REQUIRED = 5;     // 5 frames (~1.0s of continuous tilt)

/* 
// FIELD MODE (For actual Wayanad/Munnar deployment parameters):
const float TILT_THRESHOLD_DEG     = 10.0;  // Real physical shear slope creep (10.0°)
const float PRESSURE_THRESHOLD_HPA = 1.5;   // Real monsoon cyclonic pressure drop (1.5 hPa)
const int   SUSTAIN_COUNT_REQUIRED = 25;    // 25 frames (~5.0s sustain to filter trucks)
*/

int tilt_sustain_counter = 0;
bool alert_triggered = false;
const float ALPHA = 0.3; // Low-pass filter coefficient for exponential smoothing

void readMPU6050Raw(int16_t &ax, int16_t &ay, int16_t &az) {
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(0x3B);
  Wire.endTransmission(false);
  Wire.requestFrom((uint8_t)MPU6050_ADDR, (size_t)6, true);
  
  ax = (Wire.read() << 8) | Wire.read();
  ay = (Wire.read() << 8) | Wire.read();
  az = (Wire.read() << 8) | Wire.read();
}

float calculateTiltAngle(int16_t ax, int16_t ay, int16_t az) {
  float fx = ax / 16384.0;
  float fy = ay / 16384.0;
  float fz = az / 16384.0;
  
  float total_g = sqrt(fx * fx + fy * fy + fz * fz);
  if (total_g == 0) return 0;
  
  float cos_theta = fz / total_g;
  cos_theta = constrain(cos_theta, -1.0, 1.0);
  return acos(cos_theta) * (180.0 / PI);
}

void setup() {
  Serial.begin(115200);
  Wire.begin(I2C_SDA, I2C_SCL);

  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW); // Force buzzer OFF initially

  // Initialize OLED Display
  if(!display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR)) {
    Serial.println(F("OLED allocation failed! Check wiring."));
  } else {
    display.clearDisplay();
    display.setTextColor(SSD1306_WHITE);
    display.setTextSize(1);
    display.setCursor(0, 10);
    display.println("SentryMesh Active");
    display.println("Calibrating...");
    display.display();
  }

  // Wake up MPU6050
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(0x6B);
  Wire.write(0);
  Wire.endTransmission(true);

  if (!bmp.begin()) {
    Serial.println("CRITICAL: BMP180 sensor not detected!");
    while (1);
  }

  // Baseline Calibration
  Serial.println("--- STARTING BASELINE CALIBRATION ---");
  Serial.println("Keep board completely flat and still...");
  
  float p_sum = 0.0;
  float t_sum = 0.0;
  int samples = 50;

  for (int i = 0; i < samples; i++) {
    int16_t ax, ay, az;
    readMPU6050Raw(ax, ay, az);
    t_sum += calculateTiltAngle(ax, ay, az);
    p_sum += (bmp.readPressure() / 100.0F);
    delay(100);
  }

  baseline_tilt = t_sum / samples;
  baseline_pressure = p_sum / samples;

  // Seed filter variables to avoid startup spike
  current_tilt = baseline_tilt;
  current_pressure = baseline_pressure;

  Serial.print("Baseline Tilt Locked: "); Serial.print(baseline_tilt, 1); Serial.println(" deg");
  Serial.print("Baseline Pressure Locked: "); Serial.print(baseline_pressure, 1); Serial.println(" hPa");
  Serial.println("--- CALIBRATION COMPLETE ---");
}

void loop() {
  int16_t ax, ay, az;
  readMPU6050Raw(ax, ay, az);
  
  float raw_tilt = calculateTiltAngle(ax, ay, az);
  float raw_pressure = bmp.readPressure() / 100.0F;

  // Exponential Smoothing
  current_tilt = (ALPHA * raw_tilt) + ((1.0 - ALPHA) * current_tilt);
  current_pressure = (ALPHA * raw_pressure) + ((1.0 - ALPHA) * current_pressure);

  float delta_tilt = abs(current_tilt - baseline_tilt);
  float delta_pressure = abs(current_pressure - baseline_pressure);

  // Check Tilt Condition
  if (delta_tilt >= TILT_THRESHOLD_DEG) {
    tilt_sustain_counter++;
  } else {
    tilt_sustain_counter = max(0, tilt_sustain_counter - 1);
  }

  bool sustained_tilt_detected = (tilt_sustain_counter >= SUSTAIN_COUNT_REQUIRED);
  bool pressure_shift_detected = (delta_pressure >= PRESSURE_THRESHOLD_HPA);

  // Dual Confirmation Alert Logic
  alert_triggered = (sustained_tilt_detected && pressure_shift_detected);

  // Update OLED Display & Buzzer State
  display.clearDisplay();
  
  if (alert_triggered) {
    // Alert State
    digitalWrite(BUZZER_PIN, HIGH);
    
    display.setTextSize(2);
    display.setCursor(10, 5);
    display.println("WARNING!");
    display.setTextSize(1);
    display.setCursor(0, 30);
    display.println("LANDSLIDE DETECTED");
    display.setCursor(0, 45);
    display.println("EVACUATE IMMEDIATELY");
    
    Serial.println("🚨 LANDSLIDE ALERT! 🚨");
  } else {
    // Normal Monitoring State
    digitalWrite(BUZZER_PIN, LOW);
    
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.println("STATUS: NORMAL");
    display.drawLine(0, 10, 128, 10, SSD1306_WHITE);
    
    display.setCursor(0, 20);
    display.print("Tilt Delta: ");
    display.print(delta_tilt, 1);
    display.println(" deg");
    
    display.setCursor(0, 35);
    display.print("Pres Delta: ");
    display.print(delta_pressure, 2);
    display.println(" hPa");
    
    display.setCursor(0, 50);
    display.print("Sustain: ");
    display.print(tilt_sustain_counter);
    display.print("/");
    display.println(SUSTAIN_COUNT_REQUIRED);

    // Serial debug output
    Serial.print("dTilt: "); Serial.print(delta_tilt, 1);
    Serial.print("° | dPres: "); Serial.print(delta_pressure, 2);
    Serial.print(" hPa | Sustain: "); Serial.print(tilt_sustain_counter);
    Serial.println(" | Status: NORMAL");
  }

  display.display();
  delay(150);
}