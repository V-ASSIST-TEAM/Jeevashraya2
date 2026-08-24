

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

The **J2Rescue** mobile application assists rescuers in locating the buried Scout Node by detecting the BLE beacon and providing proximity information. As the rescuer approaches the buried node, the application indicates increasing proximity. A **"Very Near"** indication corresponds to the rescuer being directly above or extremely close to the buried node, thereby narrowing the search area within the debris.

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

#### Physical Demonstration Model

![Jeevashraya Model](/jeevashraya-model.jpg)

*Physical prototype representing the slope, monitored region, and nearby residential structure.*

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

Jeevashraya separates sensing from household warning through two independent nodes.

**Scout Node**

The Scout Node is deployed on the vulnerable slope. It is responsible for:

* Continuous motion and tilt monitoring.
* Atmospheric-pressure monitoring.
* Sensor calibration and processing.
* Sensor-fusion-based event detection.
* Transmission of warning information through ESP-NOW.
* BLE beacon operation during the post-burial rescue phase.

**Speaker Node**

The Speaker Node is installed inside the nearby house or protected area. It receives alerts from the Scout Node and provides an immediate local warning through:

* Audible buzzer activation.
* OLED-based warning display.

This distributed architecture allows the sensing unit to remain at the hazard location while the warning interface is positioned where occupants can respond to an alert.

### 2. Multi-Sensor Landslide Detection

The Scout Node combines measurements from two sensing modalities:

* **MPU6050** - detects motion and changes in orientation or tilt.
* **BMP180** - measures atmospheric pressure changes.

The combination provides complementary information for identifying environmental conditions associated with a potential landslide event.

Rather than relying on a single sensor reading, Jeevashraya evaluates the combined sensor state before generating a warning.

### 3. Baseline Calibration

At system startup, the Scout Node establishes baseline measurements for the deployment environment.

The baseline provides a reference against which subsequent sensor readings are evaluated. This allows the system to account for the initial orientation of the Scout Node and the prevailing pressure conditions at the deployment location.

Changes in the measured parameters are then evaluated relative to these reference values.

### 4. Sensor Fusion and Dual-Condition Verification

The core warning mechanism is based on a dual-condition decision process.

Conceptually:

**Alert = Motion/Tilt Condition AND Pressure Condition**

A significant change in only one parameter does not independently trigger the final warning condition. The system requires the relevant changes in both monitored parameters before transitioning to the alert state.

This approach is intended to reduce false triggering caused by isolated movement or environmental fluctuations.

The sensor-fusion mechanism is implemented locally on the embedded device, allowing the system to make the warning decision without relying on an external server.

### 5. Sustained Motion Detection

The system considers the persistence of the detected movement rather than treating every instantaneous change as a landslide.

The Scout Node evaluates the measured tilt and movement conditions over successive observations. Sustained changes are therefore distinguished from short-duration disturbances.

This temporal component improves the reliability of the warning mechanism by reducing sensitivity to isolated transient movements.

### 6. Local ESP-NOW Communication

When the sensor-fusion conditions indicate an alert, the Scout Node transmits the warning directly to the Speaker Node using **ESP-NOW**.

ESP-NOW provides direct wireless communication between the two nodes without requiring:

* Internet connectivity.
* A Wi-Fi router.
* Cloud infrastructure.
* Cellular connectivity.

The communication therefore remains local and independent of external network availability.

This characteristic is particularly relevant to disaster scenarios in which conventional communication infrastructure may be unavailable or disrupted.

### 7. Audible and Visual Warning

After receiving an alert from the Scout Node, the Speaker Node activates the local warning mechanism.

The warning subsystem consists of:

* **Buzzer** - provides an audible alert to occupants.
* **OLED display** - displays the corresponding warning information.

The combination of audible and visual feedback provides immediate notification to people inside the monitored residence.

### 8. Two-Phase Disaster Response

A distinguishing feature of Jeevashraya is its operation across two stages of a landslide event.

#### Phase 1 - Early Warning

During normal operation:

**Scout Node → Sensor Monitoring → Sensor Fusion → ESP-NOW → Speaker Node → Buzzer + OLED Alert**

The objective of this phase is to provide occupants with an early local warning when the monitored conditions indicate a potential landslide.

#### Phase 2 - Post-Burial Rescue

If the landslide occurs and the Scout Node becomes buried, the system transitions into its rescue-oriented mode:

**Buried Scout Node → BLE Beacon → J2Rescue → Proximity Detection → Localization**

The BLE beacon allows rescuers to search for the buried Scout Node from the debris surface.

### 9. BLE Rescue Beacon

Following burial, the Scout Node switches to a BLE-based broadcasting mode.

The node continuously advertises a BLE signal that can be detected by a nearby mobile device.

This provides a communication mechanism for the post-disaster phase without depending on cellular or internet connectivity.

The objective is not to provide a conventional data connection but to allow rescuers to determine whether they are approaching the buried node.

### 10. J2Rescue Mobile Application

**J2Rescue** is the mobile application component of Jeevashraya and plays a central role in the post-disaster localization phase.

The application scans for the BLE signal transmitted by the buried Scout Node and provides proximity information to the rescuer.

As the rescuer moves across the debris:

* The BLE beacon is detected when within communication range.
* The application provides an indication of proximity.
* Increasing proximity indicates movement toward the buried node.
* A **"Very Near"** indication identifies a location directly above or extremely close to the buried Scout Node.

This enables rescue personnel to progressively narrow down the search area rather than relying solely on visual inspection of a large debris field.

The J2Rescue application therefore extends the functionality of Jeevashraya from **warning generation to post-disaster localization**.

### 11. Sensor Data Collection and Machine Learning

The project includes a sensor-data collection pipeline using the Scout Node and `logger.py`.

The collected sensor measurements are stored in `sensor_data.csv` and are used for the development and evaluation of a machine-learning-based classification component.

The machine-learning layer complements the existing rule-based sensor-fusion approach by providing a data-driven mechanism for analyzing sensor patterns.

The ML implementation is maintained as part of the project development and will be included in the repository alongside the collected dataset.

### 12. Offline and Infrastructure-Independent Operation

The complete warning mechanism is designed to function without internet access.

The Scout Node performs sensing and local decision-making, while ESP-NOW provides direct communication with the Speaker Node.

During the rescue phase, BLE provides the local beacon mechanism and J2Rescue provides proximity-based localization.

Therefore, the core system does not require:

* Internet connectivity.
* A Wi-Fi router.
* Cloud services.
* Cellular network availability.

This makes the architecture suitable for disaster scenarios where communication infrastructure may be compromised.

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
│   └── Mobile rescue application
├── ScoutNode
├── speaker_node
├── SensorFusion.ino
├── logger.py
├── sensor_data.csv
├── jeevashraya-cover.jpg
├── jeevashraya-architecture.png
├── jeevashraya-model.jpg
├── jeevashraya-oled.jpg
└── jeevashraya-ble.jpg
│── jeevashraya-demo.mp4
└── README.md
```

## License

This project has been developed as part of **MYOSA 6.0 - IEEE International MYOSA Event**, organized under the IEEE Sensors Council, by Team Beyonspire, LBS Institute of Technology for Women, Trivandrum.

The project is intended for educational, research, and prototype-development purposes. Appropriate attribution should be provided when using or extending the project.
