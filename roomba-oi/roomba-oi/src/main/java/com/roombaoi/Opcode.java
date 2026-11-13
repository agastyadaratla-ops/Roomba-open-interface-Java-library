package com.roombaoi;

/**
 * Command opcodes from the iRobot Roomba Open Interface (OI) specification.
 * Every OI command starts with one of these bytes, followed by its data bytes.
 */
public enum Opcode {
    START(128),        // Wake the OI; puts the robot in Passive mode
    SAFE(131),         // Safe mode: full control, but cliff/wheel-drop safety stays on
    FULL(132),         // Full mode: full control, all safety features off
    POWER(133),        // Power the robot down
    SPOT(134),         // Built-in spot clean
    CLEAN(135),        // Built-in normal clean
    MAX(136),          // Built-in max clean
    DRIVE(137),        // [velocity hi][velocity lo][radius hi][radius lo]
    MOTORS(138),       // [motor bits] side brush, vacuum, main brush
    LEDS(139),         // [led bits][power color][power intensity]
    SONG(140),         // [song #][length][note, duration]...
    PLAY(141),         // [song #]
    SENSORS(142),      // [packet id]
    SEEK_DOCK(143),    // Drive back to the home base
    DRIVE_DIRECT(145), // [right vel hi][right vel lo][left vel hi][left vel lo]
    STOP(173);         // Stop the OI; robot returns to Off mode

    private final int value;

    Opcode(int value) {
        this.value = value;
    }

    public byte toByte() {
        return (byte) value;
    }
}
