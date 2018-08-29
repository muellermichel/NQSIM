package ch.ethz.systems.nqsim;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;

public class Plan extends ByteArrayList {
    private int peekIdx = 0;

    public Plan(byte ... byte_plan) {
        super();
        for (byte link_idx:byte_plan) {
            this.add(link_idx);
        }
    }

    public Plan() {
        super();
    }

    public byte peek() {
        if (this.peekIdx > super.size() - 1) {
            return (byte) -1;
        }
        return this.get(this.peekIdx);
    }

    public byte poll() {
        if (this.peekIdx > super.size() - 1) {
            return (byte) -1;
        }
        byte result = this.get(this.peekIdx);
        this.peekIdx += 1;
        return result;
    }

    public int size() {
        return super.size() - this.peekIdx;
    }
}
