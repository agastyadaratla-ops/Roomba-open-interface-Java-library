package com.roombaoi;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;

/**
 * Controls an iRobot Roomba over its serial port using the Open Interface (OI).
 *
 * Hardware setup: Raspberry Pi -> USB-to-TTL adapter -> Roomba mini-DIN serial port.
 * Default baud rate for Roomba 500/600 series is 115200.
 *
 * Usage:
 *   try (Roomba roomba = new Roomba("/dev/ttyUSB0")) {
 *       roomba.start();
 *       roomba.safeMode();
 *       roomba.driveDirect(200, 200);
 *   }
 */
public class Roomba implements AutoCloseable {

    public static final int DEFAULT_BAUD = 115200;

    /** Max wheel speed allowed by the OI, in mm/s. */
    public static final int MAX_VELOCITY = 500;

    /** Max turning radius allowed by the OI, in mm. */
    public static final int MAX_RADIUS = 2000;

    /** Special radius values defined by the OI. */
    public static final int RADIUS_STRAIGHT = 0x8000;
    public static final int RADIUS_TURN_CW = -1;
    public static final int RADIUS_TURN_CCW = 1;

    private final SerialPort port;

    public Roomba(String portName) throws IOException {
        this(portName, DEFAULT_BAUD);
    }

    public Roomba(String portName, int baudRate) throws IOException {
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 500, 0);
        if (!port.openPort()) {
            throw new IOException("Could not open serial port " + portName);
        }
    }

    // ---------------------------------------------------------------
    // Modes
    // ---------------------------------------------------------------

    /** Wakes the OI. Must be sent before any other command. */
    public void start() throws IOException {
        send(Opcode.START);
        pause(100);
    }

    /** Safe mode: you control the robot, but it stops at cliffs and wheel drops. */
    public void safeMode() throws IOException {
        send(Opcode.SAFE);
        pause(50);
    }

    /** Full mode: you control everything, including safety. Use with care. */
    public void fullMode() throws IOException {
        send(Opcode.FULL);
        pause(50);
    }

    /** Stops the OI so the robot goes back to its normal, non-controlled state. */
    public void stopOI() throws IOException {
        send(Opcode.STOP);
    }

    public void powerOff() throws IOException {
        send(Opcode.POWER);
    }

    // ---------------------------------------------------------------
    // Driving
    // ---------------------------------------------------------------

    /**
     * Drives with a velocity and a turning radius.
     *
     * @param velocity mm/s, -500 to 500 (negative = backward)
     * @param radius   mm, -2000 to 2000, or RADIUS_STRAIGHT / RADIUS_TURN_CW / RADIUS_TURN_CCW
     */
    public void drive(int velocity, int radius) throws IOException {
        velocity = clamp(velocity, -MAX_VELOCITY, MAX_VELOCITY);
        if (radius != RADIUS_STRAIGHT) {
            radius = clamp(radius, -MAX_RADIUS, MAX_RADIUS);
        }
        send(Opcode.DRIVE, high(velocity), low(velocity), high(radius), low(radius));
    }

    /**
     * Sets each wheel's speed separately. Easiest way to steer.
     *
     * @param rightVelocity mm/s, -500 to 500
     * @param leftVelocity  mm/s, -500 to 500
     */
    public void driveDirect(int rightVelocity, int leftVelocity) throws IOException {
        rightVelocity = clamp(rightVelocity, -MAX_VELOCITY, MAX_VELOCITY);
        leftVelocity = clamp(leftVelocity, -MAX_VELOCITY, MAX_VELOCITY);
        send(Opcode.DRIVE_DIRECT,
                high(rightVelocity), low(rightVelocity),
                high(leftVelocity), low(leftVelocity));
    }

    public void forward(int velocity) throws IOException {
        drive(velocity, RADIUS_STRAIGHT);
    }

    public void spinLeft(int velocity) throws IOException {
        drive(velocity, RADIUS_TURN_CCW);
    }

    public void spinRight(int velocity) throws IOException {
        drive(velocity, RADIUS_TURN_CW);
    }

    public void stop() throws IOException {
        driveDirect(0, 0);
    }

    // ---------------------------------------------------------------
    // Built-in behaviors
    // ---------------------------------------------------------------

    public void clean() throws IOException {
        send(Opcode.CLEAN);
    }

    public void spotClean() throws IOException {
        send(Opcode.SPOT);
    }

    public void seekDock() throws IOException {
        send(Opcode.SEEK_DOCK);
    }

    /**
     * Turns cleaning motors on or off.
     */
    public void setMotors(boolean sideBrush, boolean vacuum, boolean mainBrush) throws IOException {
        int bits = (sideBrush ? 0x01 : 0) | (vacuum ? 0x02 : 0) | (mainBrush ? 0x04 : 0);
        send(Opcode.MOTORS, (byte) bits);
    }

    // ---------------------------------------------------------------
    // LEDs and sound
    // ---------------------------------------------------------------

    /**
     * @param checkRobot  "check robot" LED
     * @param dock        dock LED
     * @param spot        spot LED
     * @param debris      debris LED
     * @param powerColor  0 = green, 255 = red
     * @param powerLevel  0 = off, 255 = full brightness
     */
    public void setLeds(boolean checkRobot, boolean dock, boolean spot, boolean debris,
                        int powerColor, int powerLevel) throws IOException {
        int bits = (debris ? 0x01 : 0) | (spot ? 0x02 : 0) | (dock ? 0x04 : 0) | (checkRobot ? 0x08 : 0);
        send(Opcode.LEDS, (byte) bits, (byte) clamp(powerColor, 0, 255), (byte) clamp(powerLevel, 0, 255));
    }

    /**
     * Stores a song (up to 16 notes) in one of the robot's 4 song slots.
     *
     * @param songNumber 0-3
     * @param notes      MIDI note numbers (31-127)
     * @param durations  length of each note in 1/64ths of a second
     */
    public void defineSong(int songNumber, int[] notes, int[] durations) throws IOException {
        if (notes.length != durations.length || notes.length == 0 || notes.length > 16) {
            throw new IllegalArgumentException("Need 1-16 notes, with one duration per note");
        }
        byte[] data = new byte[2 + notes.length * 2];
        data[0] = (byte) clamp(songNumber, 0, 3);
        data[1] = (byte) notes.length;
        for (int i = 0; i < notes.length; i++) {
            data[2 + i * 2] = (byte) clamp(notes[i], 31, 127);
            data[3 + i * 2] = (byte) clamp(durations[i], 0, 255);
        }
        send(Opcode.SONG, data);
    }

    public void playSong(int songNumber) throws IOException {
        send(Opcode.PLAY, (byte) clamp(songNumber, 0, 3));
    }

    // ---------------------------------------------------------------
    // Sensors
    // ---------------------------------------------------------------

    /** Packet 7: true if the left or right bumper is pressed. */
    public boolean isBumped() throws IOException {
        int value = readSensor(SensorPacket.BUMPS_AND_WHEEL_DROPS);
        return (value & 0x03) != 0;
    }

    /** Packet 7: true if either wheel has dropped (robot lifted). */
    public boolean isWheelDropped() throws IOException {
        int value = readSensor(SensorPacket.BUMPS_AND_WHEEL_DROPS);
        return (value & 0x0C) != 0;
    }

    /** Battery charge as a percentage (packets 25 and 26). */
    public double batteryPercent() throws IOException {
        int charge = readSensor(SensorPacket.BATTERY_CHARGE);
        int capacity = readSensor(SensorPacket.BATTERY_CAPACITY);
        return capacity == 0 ? 0 : 100.0 * charge / capacity;
    }

    /** Battery voltage in millivolts (packet 22). */
    public int voltage() throws IOException {
        return readSensor(SensorPacket.VOLTAGE);
    }

    /**
     * Requests one sensor packet and returns its value.
     */
    public int readSensor(SensorPacket packet) throws IOException {
        port.flushIOBuffers();
        send(Opcode.SENSORS, (byte) packet.id());

        byte[] buffer = new byte[packet.size()];
        int read = 0;
        while (read < buffer.length) {
            int n = port.readBytes(buffer, buffer.length - read, read);
            if (n <= 0) {
                throw new IOException("Timed out reading sensor packet " + packet.id());
            }
            read += n;
        }
        return packet.decode(buffer);
    }

    // ---------------------------------------------------------------
    // Low-level serial helpers
    // ---------------------------------------------------------------

    /** Sends an opcode followed by its data bytes. */
    public void send(Opcode opcode, byte... data) throws IOException {
        byte[] packet = new byte[1 + data.length];
        packet[0] = opcode.toByte();
        System.arraycopy(data, 0, packet, 1, data.length);
        int written = port.writeBytes(packet, packet.length);
        if (written != packet.length) {
            throw new IOException("Serial write failed for " + opcode);
        }
    }

    @Override
    public void close() {
        try {
            stop();
            stopOI();
        } catch (IOException ignored) {
            // Port may already be gone; still try to close it below.
        }
        port.closePort();
    }

    private static byte high(int value) {
        return (byte) ((value >> 8) & 0xFF);
    }

    private static byte low(int value) {
        return (byte) (value & 0xFF);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
