package ch.ethz.systems.nqsim;

import java.util.LinkedList;
import java.util.Queue;

public class Plan extends LinkedList<Byte> implements Queue<Byte> {
    public Plan(byte ... byte_plan) {
        super();
        for (byte link_idx:byte_plan) {
            this.add(link_idx);
        }
    }

    public Plan() {
        super();
    }
}
