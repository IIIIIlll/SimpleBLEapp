# SimpleBLEapp
Android application with a simple GUI that can be able to connect to a Bluetooth Low Energy device. The goal is to receive data from an ECG sensor, apply a filter and visualize the readings into a graph which measures the reading for the user also having it update in real time for the user


///////////////////////////////////////////

latest changes - 
/*
Test Arduino Sensor:
- Model: PCA10040
- Firmware: 2.1.0
- Date: 2021.10
- Serial: 6823550797

Issue:
When starting the scanning stage, no devices are popping up.

Notes:
- You need BLE permission and the BLE toggle switched ON.
- Once both are set, you might start seeing BLE callbacks.
- If not, check permission handling and device advertisement.
*/
