package ch.ethz.systems.nqsim;

public final class Helper {
    public static void intToByteArray(int value, byte[] bytes, int offset) {
        bytes[offset] = (byte)(value >> 24);
        bytes[offset + 1] = (byte)(value >> 16);
        bytes[offset + 2] = (byte)(value >> 8);
        bytes[offset + 3] = (byte) value;
    }

    public static int intFromByteArray(byte[] bytes, int offset) {
        return bytes[offset] << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | (bytes[offset + 3] & 0xFF);
    }
}
