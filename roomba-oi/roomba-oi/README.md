# roomba-oi

A Java library for controlling an iRobot Roomba through its serial port, using the Roomba Open Interface (OI).

## How it works

```
Java program  ->  Raspberry Pi  ->  USB-to-TTL adapter  ->  Roomba serial port (mini-DIN)
```

The Roomba exposes a serial port that accepts Open Interface commands. Each command is an opcode byte followed by data bytes. This library wraps those bytes in readable Java methods. Serial communication uses [jSerialComm](https://fazecast.github.io/jSerialComm/).

## Hardware

- Roomba 500 or 600 series (OI baud rate 115200)
- Raspberry Pi with Java 11 or newer
- USB-to-TTL serial adapter (3.3V or 5V logic; the Roomba's TX line is 5V)
- Wiring: adapter TX to Roomba RXD (pin 3), adapter RX to Roomba TXD (pin 4), GND to Roomba GND (pin 6 or 7)

## Build

```
mvn package
```

This creates `target/roomba-oi-1.0.0.jar` with jSerialComm bundled in.

## Run the demo

Copy the jar to the Pi, then:

```
java -jar roomba-oi-1.0.0.jar /dev/ttyUSB0
```

The demo plays a song, then drives forward and turns away whenever it hits something, for 30 seconds.

## Example

```java
try (Roomba roomba = new Roomba("/dev/ttyUSB0")) {
    roomba.start();      // wake the Open Interface
    roomba.safeMode();   // take control, keep cliff safety on

    roomba.driveDirect(200, 200);   // both wheels at 200 mm/s
    Thread.sleep(2000);
    roomba.spinLeft(150);
    Thread.sleep(1000);
    roomba.stop();

    System.out.println("Battery: " + roomba.batteryPercent() + "%");
}
```

## What's supported

| Area | Methods |
| --- | --- |
| Modes | `start`, `safeMode`, `fullMode`, `stopOI`, `powerOff` |
| Driving | `drive`, `driveDirect`, `forward`, `spinLeft`, `spinRight`, `stop` |
| Built-in behaviors | `clean`, `spotClean`, `seekDock`, `setMotors` |
| LEDs and sound | `setLeds`, `defineSong`, `playSong` |
| Sensors | `isBumped`, `isWheelDropped`, `batteryPercent`, `voltage`, `readSensor(SensorPacket)` |
| Raw commands | `send(Opcode, byte...)` |

## Reference

iRobot Roomba Open Interface Specification (500/600 series).
