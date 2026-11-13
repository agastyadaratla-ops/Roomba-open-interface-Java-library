package com.roombaoi;

/**
 * Sensor packets from the Roomba Open Interface, with their size in bytes
 * and whether the value is signed. Multi-byte values are sent high byte first.
 */
public enum SensorPacket {
    BUMPS_AND_WHEEL_DROPS(7, 1, false),
    WALL(8, 1, false),
    CLIFF_LEFT(9, 1, false),
    CLIFF_FRONT_LEFT(10, 1, false),
    CLIFF_FRONT_RIGHT(11, 1, false),
    CLIFF_RIGHT(12, 1, false),
    BUTTONS(18, 1, false),
    DISTANCE(19, 2, true),          // mm since last request
    ANGLE(20, 2, true),             // degrees since last request
    CHARGING_STATE(21, 1, false),
    VOLTAGE(22, 2, false),          // mV
    CURRENT(23, 2, true),           // mA
    TEMPERATURE(24, 1, true),       // degrees C
    BATTERY_CHARGE(25, 2, false),   // mAh
    BATTERY_CAPACITY(26, 2, false), // mAh
    OI_MODE(35, 1, false);          // 0 off, 1 passive, 2 safe, 3 full

    private final int id;
    private final int size;
    private final boolean signed;

    SensorPacket(int id, int size, boolean signed) {
        this.id = id;
        this.size = size;
        this.signed = signed;
    }

    public int id() {
        return id;
    }

    public int size() {
        return size;
    }

    /** Turns the raw bytes from the robot into a number. */
    int decode(byte[] data) {
        if (size == 1) {
            return signed ? data[0] : data[0] & 0xFF;
        }
        int value = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        return signed ? (short) value : value;
    }
}
