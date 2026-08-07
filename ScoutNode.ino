#include <Wire.h>
#include <Adafruit_BMP085.h>
#include <WiFi.h>
#include <esp_now.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// =======================
// I2C Pins
// =======================
#define I2C_SDA 21
#define I2C_SCL 22

#define MPU6050_ADDR 0x69

Adafruit_BMP085 bmp;
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

Adafruit_SSD1306 display(
  SCREEN_WIDTH,
  SCREEN_HEIGHT,
  &Wire,
  -1
);

//=======================
// Calibration Variables
//=======================

float baselinePressure = 0;
float baselineTilt = 0;

float currentPressure = 0;
float currentTilt = 0;

// Thresholds
const float TILT_THRESHOLD = 15.0;
const float PRESSURE_THRESHOLD = 2.0;

const int REQUIRED_COUNT = 5;

int sustainCounter = 0;

bool alertTriggered = false;

const float ALPHA = 0.3;

//=======================
// Speaker MAC Address
//=======================

// CHANGE THIS LATER
uint8_t speakerAddress[] =
{
  0xFC,0xF5,0xC4,0xAA,0xD3,0xD3
};

typedef struct
{
  char message[20];
} AlertMessage;

AlertMessage outgoing;

//=======================
// MPU6050 Functions
//=======================

void readMPU6050Raw(int16_t &ax,
                   int16_t &ay,
                   int16_t &az)
{
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(0x3B);
  Wire.endTransmission(false);

  Wire.requestFrom((uint8_t)MPU6050_ADDR,
                   (size_t)6,
                   true);

  ax=(Wire.read()<<8)|Wire.read();
  ay=(Wire.read()<<8)|Wire.read();
  az=(Wire.read()<<8)|Wire.read();
}

float calculateTilt(int16_t ax,
                    int16_t ay,
                    int16_t az)
{
  float fx=ax/16384.0;
  float fy=ay/16384.0;
  float fz=az/16384.0;

  float total=sqrt(fx*fx+fy*fy+fz*fz);

  if(total==0)
    return 0;

  float c=fz/total;

  c=constrain(c,-1,1);

  return acos(c)*180.0/PI;
}

//=======================
// ESP-NOW Callback
//=======================
//=======================
// ESP-NOW Callback (ESP32 Core 3.x)
//=======================

void onDataSent(const wifi_tx_info_t *info,
                esp_now_send_status_t status)
{
  Serial.print("Send Status : ");

  if(status == ESP_NOW_SEND_SUCCESS)
    Serial.println("Success");
  else
    Serial.println("Failed");
}

//=======================
// Setup
//=======================

void setup()
{

  Serial.begin(115200);

  Wire.begin(I2C_SDA,I2C_SCL);
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C))
{
  Serial.println("OLED Failed");
}
else
{
  Serial.println("OLED Ready");

  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(SSD1306_WHITE);

  display.setCursor(0,0);
  display.println("Scout");
  display.println("Node");

  display.display();

  delay(2000);
}

  // Wake MPU6050
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(0x6B);
  Wire.write(0);
  Wire.endTransmission(true);

  if(!bmp.begin())
  {
    Serial.println("BMP180 Error");

    while(1);
  }

  Serial.println("Calibrating...");

  float pressureSum=0;
  float tiltSum=0;

  for(int i=0;i<50;i++)
  {
      int16_t ax,ay,az;

      readMPU6050Raw(ax,ay,az);

      tiltSum+=calculateTilt(ax,ay,az);

      pressureSum+=bmp.readPressure()/100.0;

      delay(100);
  }

  baselineTilt=tiltSum/50.0;

  baselinePressure=pressureSum/50.0;

  currentTilt=baselineTilt;

  currentPressure=baselinePressure;

  Serial.println("Calibration Complete");

  // ESP-NOW initialization
  WiFi.mode(WIFI_STA);
  Serial.print("Channel: ");
  Serial.println(WiFi.channel());

  if(esp_now_init()!=ESP_OK)
  {
      Serial.println("ESP-NOW Error");

      return;
  }

  esp_now_register_send_cb(onDataSent);
    // Add Speaker Node as ESP-NOW peer

  esp_now_peer_info_t peerInfo = {};

  memcpy(peerInfo.peer_addr,
         speakerAddress,
         6);

  peerInfo.channel = 0;
  peerInfo.encrypt = false;


  if(esp_now_add_peer(&peerInfo) != ESP_OK)
  {
      Serial.println("Failed to add Speaker Peer");
      return;
  }
  Serial.println("Speaker Peer Added");

  Serial.println("Scout Node Ready");

}


//=======================
// Send Alert Function
//=======================

void sendAlert(const char *msg)
{
    strcpy(outgoing.message, msg);

    esp_err_t result = esp_now_send(
        speakerAddress,
        (uint8_t *)&outgoing,
        sizeof(outgoing)
    );

    Serial.print("esp_now_send() returned: ");
    Serial.println(result);

    if(result == ESP_OK)
        Serial.println("Alert Sent");
    else
        Serial.println("Send Error");
}



//=======================
// LOOP
//=======================

void loop()
{

  int16_t ax,ay,az;


  // Read MPU6050
  readMPU6050Raw(ax,ay,az);


  float newTilt =
  calculateTilt(ax,ay,az);


  float newPressure =
  bmp.readPressure()/100.0;



  // Low pass filtering

  currentTilt =
  ALPHA * newTilt +
  (1-ALPHA) * currentTilt;


  currentPressure =
  ALPHA * newPressure +
  (1-ALPHA) * currentPressure;



  float tiltDifference =
  abs(currentTilt - baselineTilt);


  float pressureDifference =
  abs(currentPressure - baselinePressure);



  Serial.print("Tilt : ");
  Serial.print(currentTilt);

  Serial.print("  Tilt Diff : ");
  Serial.print(tiltDifference);


  Serial.print("  Pressure : ");
  Serial.print(currentPressure);


  Serial.print("  Pressure Diff : ");
  Serial.println(pressureDifference);
  display.clearDisplay();

display.setTextSize(1);
display.setCursor(0,0);

display.print("Tilt: ");
display.println(currentTilt);

display.print("Diff: ");
display.println(tiltDifference);


display.print("Pressure: ");
display.println(currentPressure);


if(sustainCounter >= REQUIRED_COUNT)
{
  display.println("STATUS: ALERT");
}
else
{
  display.println("STATUS: SAFE");
}


display.display();



  bool danger = false;



  if(tiltDifference > TILT_THRESHOLD &&
     pressureDifference > PRESSURE_THRESHOLD)
  {
      sustainCounter++;

  }
  else
  {
      sustainCounter=0;
      alertTriggered=false;
  }



  // Condition should remain for some samples

  if(sustainCounter >= REQUIRED_COUNT &&
     !alertTriggered)
  {

      Serial.println("DANGER DETECTED");


      sendAlert("FALL ALERT");


      alertTriggered=true;

  }



  delay(200);

}
