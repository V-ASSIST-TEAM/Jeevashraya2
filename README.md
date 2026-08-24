---
publishDate: 2026-08-24
title: Jeevashraya - Offline Landslide Warning and Rescue System
excerpt: An offline landslide early-warning and post-disaster rescue system combining multi-sensor fusion, ESP-NOW communication, BLE-based localization, and a dedicated mobile application.
image: jeevashraya-cover.jpg
tags:
- landslide
- disaster-management
- sensor-fusion
---

> **When conventional communication fails, Jeevashraya provides a local path from warning to rescue.**

## Acknowledgements

Jeevashraya was developed by **Team Beyonspire**, comprising undergraduate students from the Computer Science Engineering and Electronics and Computer Engineering programs at **LBS Institute of Technology for Women, Trivandrum**, as part of **MYOSA 6.0 - IEEE International MYOSA Event**, organized under the IEEE Sensors Council.

The team gratefully acknowledges the guidance and support provided by **Rensi Sam Mathew, Assistant Professor, Department of ECE, LBSITW**, throughout the development and evaluation of the project.

**Team Beyonspire**

* **Diya N S** - 3rd Year, Computer Science Engineering
* **Iris Mariah Kurien** - 4th Year, Computer Science Engineering
* **Rahaf Ayesha Rehas** - 3rd Year, Electronics and Computer Engineering
* **Sruthi S** - 3rd Year, Electronics and Computer Engineering

## Overview

Landslides pose a significant risk to communities located in hilly and geologically vulnerable regions. During severe rainfall and disaster events, conventional communication infrastructure may become unreliable, limiting the effectiveness of systems that depend on internet connectivity, routers, cellular networks, or cloud services.

**Jeevashraya** addresses this limitation through a two-phase, infrastructure-independent architecture designed for both **pre-disaster warning and post-disaster rescue support**.

The system consists of two primary hardware nodes:

* **Scout Node** - positioned on the vulnerable slope to continuously monitor environmental and physical changes.
* **Speaker Node** - positioned inside a nearby house or protected location to receive warnings and provide immediate local alerts.

The Scout Node uses an **MPU6050** to monitor motion and tilt changes and a **BMP180** to monitor atmospheric pressure. These measurements are processed locally using sensor-fusion logic. A landslide condition is considered significant when the required changes in both parameters are observed, reducing the possibility of triggering an alert from an isolated sensor disturbance.

When the combined conditions indicate a potential landslide event, the Scout Node transmits an alert to the Speaker Node using **ESP-NOW**. ESP-NOW enables direct device-to-device communication without requiring an internet connection, Wi-Fi router, or cloud infrastructure.

Upon receiving the alert, the Speaker Node activates an audible buzzer and displays a warning message on its OLED display, providing an immediate local evacuation indication.

Jeevashraya also addresses the scenario in which the Scout Node is buried during a landslide. Following burial, the system enters a **BLE-based rescue beacon mode**, allowing the buried node to continuously broadcast a Bluetooth Low Energy signal.

The **J2Rescue** mobile application assists rescuers in locating the buried Scout Node by detecting the BLE beacon and providing proximity information. As the rescuer approaches the buried node, the application indicates increasing proximity. A **"Very Close"** indication corresponds to the rescuer being directly above or extremely close to the buried node, thereby narrowing the search area within the debris.

The project additionally incorporates a machine-learning component based on the sensor data collected during system testing. The collected dataset provides a foundation for developing and evaluating intelligent event classification alongside the rule-based sensor-fusion mechanism.

## Demo / Examples

The prototype demonstration presents the operation of the sensing, communication, alert, and rescue-support subsystems.

### Images

#### System Prototype

![Jeevashraya Prototype](/jeevashraya-cover.jpg)

*Jeevashraya prototype demonstrating the integrated sensing, communication, and warning architecture.*

#### System Architecture

![Jeevashraya Architecture](/jeevashraya-architecture.png)

*System architecture illustrating the Scout Node, ESP-NOW communication link, Speaker Node, BLE rescue beacon, and J2Rescue application.*

#### Alert Display

![Jeevashraya OLED](/jeevashraya-oled.jpg)

*OLED display indicating the transition from normal monitoring to an alert condition.*

#### J2Rescue Mobile Application

![J2Rescue Mobile Application](/jeevashraya-ble.jpg)

*J2Rescue application detecting the BLE rescue beacon and providing proximity information for locating the buried Scout Node.*

### Videos

The complete prototype demonstration is provided below.

https://github.com/user-attachments/assets/2fa5d9d6-f9fd-4afb-af13-d3d1fc530205

## Features (Detailed)

### 1. Two-Node Distributed Architecture

Jeevashraya uses a distributed architecture consisting of two primary hardware nodes:

**Scout Node**

The Scout Node is deployed on the vulnerable slope and continuously monitors conditions using:

* **MPU6050** for motion and tilt detection.
* **BMP180** for atmospheric-pressure monitoring.
* **ESP32** for local processing and sensor-fusion-based decision making.

**Speaker Node**

The Speaker Node is positioned inside a nearby house or protected location. It receives alerts from the Scout Node and provides an immediate local warning through:

* An audible buzzer.
* An OLED display.

This separation allows sensing to take place directly at the hazard location while the warning interface remains accessible to occupants.

### 2. Simultaneous Sensor Fusion and False-Alarm Prevention

Jeevashraya uses a dual-condition sensor-fusion mechanism to reduce false alarms.

The system continuously evaluates two conditions:

1. A significant and sustained **motion/tilt change** detected by the MPU6050.
2. A corresponding **pressure change** detected by the BMP180.

The warning condition is expressed as:

**Alert = Motion/Tilt Condition AND Pressure Condition**

Importantly, **both conditions must be detected simultaneously within the defined detection window**. A change in only one parameter does not trigger the final alert.

For example:

* **Motion/Tilt only → SAFE**
* **Pressure change only → SAFE**
* **Motion/Tilt + Pressure change simultaneously → ALERT**

The Scout Node establishes baseline sensor values during initialization and evaluates subsequent measurements against these reference values. Sustained movement is considered rather than a single instantaneous change, helping distinguish potential slope movement from short-duration disturbances.

### 3. Offline ESP-NOW Alert Communication

Once the simultaneous sensor conditions are confirmed, the Scout Node transmits an alert to the Speaker Node using **ESP-NOW**.

ESP-NOW provides direct device-to-device communication without requiring:

* Internet connectivity.
* A Wi-Fi router.
* Cloud infrastructure.
* Cellular connectivity.

The warning pathway therefore remains operational independently of external communication infrastructure, which is particularly relevant during severe weather and disaster conditions.

The alert pathway is:

**Scout Node → ESP-NOW → Speaker Node**

### 4. Immediate Audible and Visual Warning

After receiving the alert, the Speaker Node provides immediate feedback to occupants.

The warning subsystem consists of:

* **Buzzer** - generates an audible warning.
* **OLED display** - displays the corresponding landslide alert.

This combination provides both audible and visual notification, enabling occupants to recognize the warning even when environmental noise or visibility may be an issue.

The prototype demonstrates the transition from the normal monitoring state to the alert state through the Speaker Node.

### 5. Two-Phase Disaster Response

Jeevashraya is designed to address both the **warning phase** and the **post-disaster rescue phase** of a landslide event.

#### Phase 1 - Early Warning

The first phase follows:

**Slope Monitoring → Sensor Fusion → ESP-NOW → Speaker Node → Buzzer + OLED Alert**

The Scout Node detects the simultaneous sensor conditions and communicates the warning locally to the Speaker Node, allowing occupants to respond before or during the onset of a hazardous event.

#### Phase 2 - Post-Burial Rescue

If the landslide occurs and the Scout Node becomes buried, the system transitions to its rescue-oriented mode:

**Buried Scout Node → BLE Beacon → J2Rescue → Proximity Detection**

This allows the system to remain useful even after the Scout Node is no longer accessible or visible.

### 6. BLE-Based Rescue Beacon

Following burial, the Scout Node switches to a **Bluetooth Low Energy (BLE) beacon mode** and continuously broadcasts a BLE signal.

The beacon can be detected by a nearby mobile device without requiring internet or cellular connectivity.

The purpose of the beacon is to provide a proximity reference for rescuers searching across the debris surface, enabling them to progressively narrow down the location of the buried Scout Node.

### 7. J2Rescue Mobile Application

**J2Rescue** is the Kotlin-based Android application developed as the mobile rescue component of Jeevashraya.

The application scans for the BLE beacon transmitted by the buried Scout Node and provides proximity information to the rescuer.

As the rescuer moves toward the beacon:

* The application detects the Scout Node's BLE signal.
* Proximity information changes as the rescuer moves closer or farther away.
* A **"Very Close"** indication signifies that the rescuer is directly above or extremely close to the buried Scout Node.

This provides a practical method for narrowing the search location within a debris field and extends Jeevashraya from an early-warning system into a post-disaster localization system.

### 8. Machine Learning-Based Event Classification

Jeevashraya analyzes recorded tilt angles and barometric-pressure fluctuations using a time-series ML pipeline. Causal feature engineering with EWMA and dynamic kinetic interaction terms captures temporal patterns while preventing data leakage.

An ExtraTrees classifier ("landslide_model.pkl") achieves 92.42% accuracy and 93.33% alert recall on the evaluated data, distinguishing benign transient creep from catastrophic shear-failure patterns. The model also generates live probability scores for three risk states: SAFE, FILTERED ANOMALY, and CRITICAL COLLAPSE.

The model and real-time dashboard ("app.py") are contained in the "ml_dashboard/" directory

## Usage Instructions

### Scout Node Setup

1. Connect the MPU6050 and BMP180 sensors to the Scout Node.
2. Connect the required hardware according to the firmware configuration.
3. Open the Scout Node firmware in the Arduino IDE.
4. Select the appropriate ESP32 board and serial port.
5. Compile and upload the firmware.
6. Place the Scout Node in its intended orientation on the slope.
7. Power the node while keeping it stationary during calibration.
8. Allow the baseline calibration process to complete.
9. The Scout Node then enters monitoring mode.

### Speaker Node Setup

1. Connect the OLED display and buzzer to the Speaker Node.
2. Open the Speaker Node firmware in the Arduino IDE.
3. Select the appropriate ESP8266 board configuration.
4. Compile and upload the firmware.
5. Power the Speaker Node.
6. Verify that the node is ready to receive ESP-NOW communication from the Scout Node.

### Normal Monitoring

During operation, the Scout Node:

1. Reads motion and tilt information from the MPU6050.
2. Reads atmospheric pressure from the BMP180.
3. Processes the sensor readings.
4. Compares current measurements against the calibrated baseline.
5. Evaluates the motion/tilt condition.
6. Evaluates the pressure condition.
7. Determines whether both conditions are satisfied.
8. Maintains the normal monitoring state when the combined condition is not met.

### Alert Demonstration

The warning mechanism can be demonstrated by introducing controlled changes to the monitored parameters.

**Motion/Tilt condition only**

Introduce a change in Scout Node orientation without satisfying the required pressure condition.

**Expected result:** The system does not enter the final alert state.

**Pressure condition only**

Introduce a pressure change without satisfying the required motion/tilt condition.

**Expected result:** The system does not enter the final alert state.

**Combined condition**

Introduce the required motion/tilt change together with the corresponding pressure condition.

**Expected result:**

1. The Scout Node identifies the combined condition.
2. An alert is transmitted through ESP-NOW.
3. The Speaker Node receives the alert.
4. The buzzer is activated.
5. The OLED displays the landslide warning.

### Burial and Rescue Demonstration

The post-disaster rescue mechanism can be demonstrated as follows:

1. Simulate burial of the Scout Node.
2. Allow the Scout Node to enter BLE beacon mode.
3. Open the J2Rescue mobile application.
4. Scan for the Scout Node's BLE signal.
5. Move the mobile device toward the beacon source.
6. Observe the changing proximity indication.
7. When the application indicates **"Very Near,"** the rescuer is directly above or extremely close to the buried Scout Node.

### ML Dashboard

The machine-learning dashboard provides a real-time interface for evaluating sensor readings and model-generated risk levels.

1. Open the "ml_dashboard" directory in the terminal:

```cd ml_dashboard```

2. Install the required Python dependencies:

```pip install streamlit pandas numpy scikit-learn joblib```

3. Start the dashboard:

```streamlit run app.py```

4. Open the dashboard at "http://localhost:8501".

5. Use the Tilt and Pressure controls in the sidebar to simulate different sensor conditions.

6. Observe the real-time sensor charts, hazard-risk percentage, and alert status generated by the machine-learning model   

## Tech Stack

### Embedded Hardware

* **ESP32** - Scout Node processing and sensing
* **ESP8266** - Speaker Node communication and alerting
* **MPU6050** - motion and tilt sensing
* **BMP180** - atmospheric-pressure sensing
* **SSD1306 OLED** - visual warning interface
* **Buzzer** - audible warning mechanism

### Communication

* **ESP-NOW** - offline communication between Scout Node and Speaker Node
* **Bluetooth Low Energy (BLE)** - post-burial rescue beacon
* **J2Rescue** - mobile-based proximity detection and localization

### Software

* **Arduino / C++** - embedded firmware
* **Python** - sensor-data logging and supporting analysis
* **CSV** - sensor-data storage
* **Machine Learning** - sensor-pattern analysis and classification
- **Kotlin / Android** — rescue-support mobile application

## Requirements / Installation

### Hardware Requirements

The prototype requires:

* ESP32 development board
* ESP8266 development board
* MPU6050 sensor
* BMP180 sensor
* SSD1306 OLED display
* Buzzer
* Required wiring and power components

### Software Requirements

* Arduino IDE
* ESP32 board support package
* ESP8266 board support package
* Required Arduino libraries for:

  * MPU6050 / I2C communication
  * BMP180
  * SSD1306 OLED
* Python environment for sensor-data logging and analysis
* Android development environment for J2Rescue

### Arduino Installation

1. Install the Arduino IDE.
2. Install the ESP32 board support package through the Arduino Board Manager.
3. Install the ESP8266 board support package.
4. Install the libraries required by the respective firmware.
5. Open the required `.ino` file from the repository.
6. Select the appropriate board and serial port.
7. Compile and upload the firmware.
8. Use the Serial Monitor to observe sensor and system status during testing.

### Python Data Logging

The repository contains `logger.py` for collecting sensor data and `sensor_data.csv` for storing the recorded measurements.

The collected dataset can be used for sensor analysis and machine-learning model development and evaluation.

### J2Rescue Installation

The J2Rescue mobile application is located in the `J2Rescue` directory.

The application can be built and deployed using the Android development environment and project configuration contained within that directory.

## File Structure

```text
Jeevashraya2/
├── J2Rescue/
├── ScoutNode/
├── speaker_node/
├── SensorFusion.ino
├── logger.py
├── sensor_data.csv
├── jeevashraya-cover.jpg
├── jeevashraya-architecture.png
├── jeevashraya-oled.jpg
├── jeevashraya-ble.jpg
├── jeevashraya-demo.mp4
└── README.md
```

## License

This project has been developed as part of **MYOSA 6.0 - IEEE International MYOSA Event**, organized under the IEEE Sensors Council, by Team Beyonspire, LBS Institute of Technology for Women, Trivandrum.

The project is intended for educational, research, and prototype-development purposes. Appropriate attribution should be provided when using or extending the project.
