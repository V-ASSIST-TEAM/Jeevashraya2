import serial
import csv
import time

# =========================
# SETTINGS
# =========================

PORT = "COM4"      
BAUD_RATE = 115200

OUTPUT_FILE = "sensor_data.csv"


# =========================
# CONNECT TO ESP32
# =========================

print("Connecting to ESP32...")

ser = serial.Serial(
    PORT,
    BAUD_RATE,
    timeout=1
)

# Give ESP32 time to reset
time.sleep(2)

print("Connected!")
print("Logging sensor data...")
print("Press Ctrl+C to stop.\n")


# =========================
# CREATE CSV FILE
# =========================

with open(
    OUTPUT_FILE,
    "w",
    newline=""
) as csvfile:

    writer = csv.writer(csvfile)

    # CSV header
    writer.writerow([
        "Time",
        "Tilt",
        "TiltDiff",
        "Pressure",
        "PressureDiff",
        "Status"
    ])

    try:

        while True:

            line = ser.readline().decode(
                "utf-8",
                errors="ignore"
            ).strip()

            if not line:
                continue

            # Split CSV line
            data = line.split(",")

            # We only want lines containing
            # exactly 6 CSV values
            if len(data) != 6:
                continue

            try:

                # Check that first 5 values
                # are actually numbers

                float(data[0])
                float(data[1])
                float(data[2])
                float(data[3])
                float(data[4])

            except ValueError:
                continue

            # Make sure status is valid
            if data[5] not in ["SAFE", "ALERT"]:
                continue

            # Write to CSV
            writer.writerow(data)

            # Make sure data is immediately saved
            csvfile.flush()

            print(
                f"Time: {data[0]} ms | "
                f"Tilt: {data[1]}° | "
                f"Pressure: {data[3]} hPa | "
                f"Status: {data[5]}"
            )

    except KeyboardInterrupt:

        print("\nStopping logger...")

    finally:

        ser.close()

        print(f"Data saved to {OUTPUT_FILE}")