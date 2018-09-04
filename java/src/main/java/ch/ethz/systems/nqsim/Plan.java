package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public final class Plan {
    private byte[] bytes;
    private int peekIdx;

    public Plan(byte[] bytes, int peekIdx) {
        this.bytes = bytes;
        this.peekIdx = peekIdx;
    }

    @JsonCreator
    public Plan(byte[] bytes) {
        this(bytes, 0);
    }

    public byte peek() {
        if (this.peekIdx > this.bytes.length - 1) {
            return (byte) -1;
        }
        return this.bytes[this.peekIdx];
    }

    public byte poll() {
        if (this.peekIdx > this.bytes.length - 1) {
            return (byte) -1;
        }
        byte result = this.bytes[this.peekIdx];
        this.peekIdx += 1;
        return result;
    }

    public int size() {
        return this.bytes.length - this.peekIdx;
    }

    @JsonValue
    public byte[] getBytes() {
        return this.bytes;
    }

    public void serializeToBytes(byte[] bytes, int offset) {
        Helper.intToByteArray(this.bytes.length - this.peekIdx, bytes, offset);
        for (int idx = 0; idx < this.bytes.length - this.peekIdx; idx++) {
            bytes[offset + idx + 4] = this.bytes[this.peekIdx + idx];
        }
    }

    public static Plan deserializeFromBytes(byte[] bytes, int offset) {
        int planLength = Helper.intFromByteArray(bytes, offset);
        byte[] planBytes = new byte[planLength];
        for (int idx = 0; idx < planLength; idx++) {
            planBytes[idx] = bytes[offset + idx + 4];
        }
        return new Plan(planBytes);
    }

    public int byteLength() {
        return this.size() + 4;
    }
}
