package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;

public final class Agent {
    public int current_travel_time;
    public int time_to_pass_link;

    private long id;
    private Plan plan;
    private static long next_id = 0;
    private static int is_using_auto_id = -1;

    private static long getAutoId() {
        long id = next_id;
        next_id += 1;
        return id;
    }

    @JsonCreator
    public Agent(
        @JsonProperty("id") long id,
        @JsonProperty("plan") Plan plan,
        @JsonProperty("current_travel_time") int current_travel_time,
        @JsonProperty("time_to_pass_link") int time_to_pass_link
    ) {
        if (id >= next_id) {
            next_id = id + 1;
        }
        this.id = id;
        this.plan = plan;
        this.current_travel_time = current_travel_time;
        this.time_to_pass_link = time_to_pass_link;
    }

    public Agent(long id, Plan plan) {
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

    public long getId() {
        return this.id;
    }
    public Plan getPlan() {
        return this.plan;
    }
}
