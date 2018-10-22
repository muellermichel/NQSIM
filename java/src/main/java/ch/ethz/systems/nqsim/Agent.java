package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;

public final class Agent {
    public int current_travel_time;
    public int time_to_pass_link;

    private String id;
    private byte[] id_bytes;
    private long numeric_id = -1;
    private Plan plan;
    private static long next_id = 0;
    private static int is_using_auto_id = -1;

    private static String getAutoId() {
        long id = next_id;
        return String.valueOf(id);
    }

    private static long getNumericId(String id) {
        long numeric_id = 0;
        try {
            numeric_id = Long.parseLong(id);
        }
        catch (NumberFormatException e) {
            //ignore
        }
        return numeric_id;
    }

    public static void resetAutoIds() {
        next_id = 0;
    }

    @JsonCreator
    public Agent(
        @JsonProperty("id") String id,
        @JsonProperty("plan") Plan plan,
        @JsonProperty("current_travel_time") int current_travel_time,
        @JsonProperty("time_to_pass_link") int time_to_pass_link
    ) {
        this(getNumericId(id), plan, current_travel_time, time_to_pass_link);
    }

    public Agent(long id, Plan plan, int current_travel_time, int time_to_pass_link) {
        this.numeric_id = id;
        if (this.numeric_id >= next_id) {
            next_id = this.numeric_id + 1;
        }
        this.plan = plan;
        this.current_travel_time = current_travel_time;
        this.time_to_pass_link = time_to_pass_link;
    }

    public Agent(String id, Plan plan) {
        this(id, plan, 0, 0);
    }

    public Agent(Plan plan) {
        this(getAutoId(), plan, 0, 0);
    }

    public static Agent fromJson(byte[] jsonData, ObjectReader or) throws IOException {
        return or.readValue(jsonData);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id=" + this.getId() +
                ", current_travel_time=" + current_travel_time +
                ", time_to_pass_link=" + time_to_pass_link +
                ", plan=" + this.plan.toString() +
                '}';
    }

    public void streamAsJson(OutputStream os, ObjectWriter ow) throws IOException {
        ow.writeValue(os, this);
    }

    public void serializeToBytes(byte[] bytes, int offset) {
        Helper.intToByteArray(this.current_travel_time, bytes, offset);
        offset += 4;
        Helper.intToByteArray(this.time_to_pass_link, bytes, offset);
        offset += 4;
        Helper.intToByteArray(this.getIdBytes().length, bytes, offset);
        offset += 4;
        for (int idx = 0; idx < this.getIdBytes().length; idx++) {
            bytes[offset + idx] = this.getIdBytes()[idx];
        }
        offset += this.getIdBytes().length;
        this.plan.serializeToBytes(bytes, offset);
    }

    public static Agent deserializeFromBytes(byte[] bytes, int offset) {
        int current_travel_time = Helper.intFromByteArray(bytes, offset);
        offset += 4;
        int time_to_pass_link = Helper.intFromByteArray(bytes, offset);
        offset += 4;
        int num_id_bytes = Helper.intFromByteArray(bytes, offset);
        offset += 4;
        String agent_id = new String(bytes, offset, num_id_bytes);
        offset += num_id_bytes;
        Plan plan = Plan.deserializeFromBytes(bytes, offset);
        return new Agent(agent_id, plan, current_travel_time, time_to_pass_link);
    }

    public int byteLength() {
        return 12 + this.getIdBytes().length + this.plan.byteLength();
    }

    public void tick(int delta_t) {
        current_travel_time += delta_t;
    }

    public byte peekPlan() {
        return this.plan.peek();
    }

    public byte pollPlan() {
        return this.plan.poll();
    }

    public String getId() {
        if (this.id == null) {
            this.id = String.valueOf(this.numeric_id);
        }
        return this.id;
    }

    public byte[] getIdBytes() {
        if (this.id_bytes == null) {
            this.id_bytes = this.getId().getBytes();
        }
        return this.id_bytes;
    }

    public Plan getPlan() {
        return this.plan;
    }
}
