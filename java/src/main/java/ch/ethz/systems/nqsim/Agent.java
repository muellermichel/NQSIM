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
    private Plan plan;
    private static long next_id = 0;
    private static int is_using_auto_id = -1;

    private static String getAutoId() {
        long id = next_id;
        return String.valueOf(id);
    }

    @JsonCreator
    public Agent(
        @JsonProperty("id") String id,
        @JsonProperty("plan") Plan plan,
        @JsonProperty("current_travel_time") int current_travel_time,
        @JsonProperty("time_to_pass_link") int time_to_pass_link
    ) {
        long numeric_id = 0;
        try {
            numeric_id = Long.parseLong(id);
        }
        catch (NumberFormatException e) {
            //ignore
        }
        if (numeric_id >= next_id) {
            next_id = numeric_id + 1;
        }
        this.id = id;
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

    public Agent() {
        this(getAutoId(), new Plan(), 0, 0);
    }

    public static Agent fromJson(byte[] jsonData, ObjectReader or) throws IOException {
        return or.readValue(jsonData);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id=" + id +
                ", current_travel_time=" + current_travel_time +
                ", time_to_pass_link=" + time_to_pass_link +
                ", plan=" + plan +
                '}';
    }

    public void streamAsJson(OutputStream os, ObjectWriter ow) throws IOException {
        ow.writeValue(os, this);
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
        return this.id;
    }
    public Plan getPlan() {
        return this.plan;
    }
}
