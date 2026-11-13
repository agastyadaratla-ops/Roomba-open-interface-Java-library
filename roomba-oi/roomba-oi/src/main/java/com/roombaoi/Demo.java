package com.roombaoi;

/**
 * Example program: drives forward until the bumper is hit,
 * backs up, turns, and repeats. Plays a short song at the start.
 *
 * Run on the Raspberry Pi:
 *   java -jar roomba-oi-1.0.0.jar /dev/ttyUSB0
 */
public class Demo {

    public static void main(String[] args) throws Exception {
        String portName = args.length > 0 ? args[0] : "/dev/ttyUSB0";

        try (Roomba roomba = new Roomba(portName)) {
            roomba.start();
            roomba.safeMode();

            System.out.printf("Battery: %.0f%%%n", roomba.batteryPercent());

            roomba.defineSong(0, new int[]{60, 64, 67, 72}, new int[]{16, 16, 16, 32});
            roomba.playSong(0);
            Thread.sleep(1500);

            long end = System.currentTimeMillis() + 30_000; // run for 30 seconds
            while (System.currentTimeMillis() < end) {
                if (roomba.isWheelDropped()) {
                    System.out.println("Robot lifted, stopping.");
                    break;
                }
                if (roomba.isBumped()) {
                    roomba.driveDirect(-150, -150); // back up
                    Thread.sleep(500);
                    roomba.spinLeft(150);           // turn away
                    Thread.sleep(700);
                } else {
                    roomba.forward(200);
                }
                Thread.sleep(50);
            }
        }
    }
}
