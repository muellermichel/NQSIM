package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class JSONBenchmark {
    private static Plan generateRandomPlan(int numOfLegs) {
        Plan plan = new Plan();
        for (int idx = 0; idx < numOfLegs; idx++) {
            byte next = (byte)ThreadLocalRandom.current().nextInt(0, 8);
            plan.add(next);
        }
        return plan;
    }

    private static List<Agent> generateRandomAgents(int numOfAgents) {
        List<Agent> result = new ArrayList<>(numOfAgents);
        for (int idx = 0; idx < numOfAgents; idx++) {
            int num_legs = ThreadLocalRandom.current().nextInt(0, 30);
            int time_to_pass_link = ThreadLocalRandom.current().nextInt(10, 200);
            int current_travel_time = ThreadLocalRandom.current().nextInt(0, time_to_pass_link);
            result.add(new Agent(idx, generateRandomPlan(num_legs), current_travel_time, time_to_pass_link));
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        int NUM_AGENTS = 10000000;
        int NUM_AGENTS_PER_COMM = 100;
        List<Agent> agents = generateRandomAgents(NUM_AGENTS);
        ObjectMapper om = new ObjectMapper();
        ObjectWriter agentWriter = om.writerFor(Agent[].class);
        ObjectReader agentReader = om.readerFor(Agent[].class);
        Agent[] tempAgentArray = new Agent[NUM_AGENTS_PER_COMM];

        long start = System.currentTimeMillis();
        for (int idx = 0; idx < agents.size(); idx += NUM_AGENTS_PER_COMM) {
            List<Agent> sub_agents = agents.subList(idx, Math.min(idx + NUM_AGENTS_PER_COMM, agents.size()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            agentWriter.writeValue(baos, sub_agents.toArray(tempAgentArray));
            Agent[] sub_agents_after = agentReader.readValue(baos.toByteArray());
            assert sub_agents.get(0).current_travel_time == sub_agents_after[0].current_travel_time;
            assert sub_agents.get(sub_agents.size() - 1).current_travel_time == sub_agents_after[sub_agents.size() - 1].current_travel_time;
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(String.format(
            "runtime [s]: %s; ser+deser per second: %s",
            String.valueOf(time / (double)1000),
            String.valueOf(NUM_AGENTS / (time / (double)1000))
        ));
    }
}
