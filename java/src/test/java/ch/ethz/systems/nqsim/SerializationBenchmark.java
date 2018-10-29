package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SerializationBenchmark {
    static int NUM_AGENTS_PER_COMM = 100;
    static int NUM_AGENTS = 10000000;

    private static Plan generateRandomPlan(int numOfLegs) {
        byte[] bytes = new byte[numOfLegs];
        for (int idx = 0; idx < numOfLegs; idx++) {
            byte next = (byte)ThreadLocalRandom.current().nextInt(0, 8);
            bytes[idx] = next;
        }
        return new Plan(bytes);
    }

    private static List<Agent> generateRandomAgents(int numOfAgents) {
        List<Agent> result = new ArrayList<>(numOfAgents);
        for (int idx = 0; idx < numOfAgents; idx++) {
            int num_legs = ThreadLocalRandom.current().nextInt(0, 30);
            int time_to_pass_link = ThreadLocalRandom.current().nextInt(10, 200);
            int current_travel_time = ThreadLocalRandom.current().nextInt(0, time_to_pass_link);
            result.add(new Agent(String.valueOf(idx), generateRandomPlan(num_legs), current_travel_time, time_to_pass_link));
        }
        return result;
    }

    private static void runJSONBenchmark(List<Agent> agents) throws IOException {
//        ObjectMapper om = new ObjectMapper();
//        ObjectWriter agentWriter = om.writerFor(Agent.class);
//        ObjectReader agentReader = om.readerFor(Agent.class);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        byte[][] byteBuffer = new byte[NUM_AGENTS_PER_COMM][];
//        Agent[] readBuffer = new Agent[NUM_AGENTS_PER_COMM];
//
//        long start = System.currentTimeMillis();
//        for (int comm_idx = 0; comm_idx < agents.size(); comm_idx += NUM_AGENTS_PER_COMM) {
//            for (int idx = 0; idx < NUM_AGENTS_PER_COMM; idx++) {
//                agentWriter.writeValue(baos, agents.get(comm_idx + idx));
//                byteBuffer[idx] = baos.toByteArray();
//                baos.reset();
//            }
//            for (int idx = 0; idx < NUM_AGENTS_PER_COMM; idx++) {
//                readBuffer[idx] = agentReader.readValue(byteBuffer[idx]);
//            }
//            assert agents.get(comm_idx).current_travel_time == readBuffer[0].current_travel_time;
//            assert agents.get(comm_idx + NUM_AGENTS_PER_COMM - 1).current_travel_time == readBuffer[NUM_AGENTS_PER_COMM - 1].current_travel_time;
//            assert agents.get(comm_idx).peekPlan() == readBuffer[0].peekPlan();
//            assert agents.get(comm_idx + NUM_AGENTS_PER_COMM - 1).peekPlan() == readBuffer[NUM_AGENTS_PER_COMM - 1].peekPlan();
//        }
//        long time = System.currentTimeMillis() - start;
//        System.out.println(String.format(
//                "jackson runtime [s]: %s; ser+deser per second: %s",
//                String.valueOf(time / (double)1000),
//                String.valueOf(NUM_AGENTS / (time / (double)1000))
//        ));
    }

    private static void runProtostuffBenchmark(List<Agent> agents) {
//        Agent[] tempAgentArray = new Agent[NUM_AGENTS_PER_COMM];
//        Schema<AgentArrayBox> schema = RuntimeSchema.getSchema(AgentArrayBox.class);
//        LinkedBuffer buffer = LinkedBuffer.allocate(1024);
//
//        long start = System.currentTimeMillis();
//        for (int idx = 0; idx < agents.size(); idx += NUM_AGENTS_PER_COMM) {
//            List<Agent> sub_agents = agents.subList(idx, Math.min(idx + NUM_AGENTS_PER_COMM, agents.size()));
//            AgentArrayBox box = new AgentArrayBox(sub_agents.toArray(tempAgentArray));
//            final byte[] protostuff;
//            try {
//                protostuff = ProtostuffIOUtil.toByteArray(
//                    box,
//                    schema,
//                    buffer
//                );
//            }
//            finally {
//                buffer.clear();
//            }
//            AgentArrayBox box_after = schema.newMessage();
//            ProtostuffIOUtil.mergeFrom(protostuff, box_after, schema);
//            Agent[] sub_agents_after = box_after.agents;
//            assert sub_agents.get(0).current_travel_time == sub_agents_after[0].current_travel_time;
//            assert sub_agents.get(sub_agents.size() - 1).current_travel_time == sub_agents_after[sub_agents.size() - 1].current_travel_time;
//            assert sub_agents.get(0).peekPlan() == sub_agents_after[0].peekPlan();
//            assert sub_agents.get(sub_agents.size() - 1).peekPlan() ==  sub_agents_after[sub_agents.size() - 1].peekPlan();
//        }
//        long time = System.currentTimeMillis() - start;
//        System.out.println(String.format(
//                "protostuff runtime [s]: %s; ser+deser per second: %s",
//                String.valueOf(time / (double)1000),
//                String.valueOf(NUM_AGENTS / (time / (double)1000))
//        ));
    }

    private static void runBinaryBenchmark(List<Agent> agents) throws IOException, ClassNotFoundException {
//        long start = System.currentTimeMillis();
//        Agent[] readBuffer = new Agent[NUM_AGENTS_PER_COMM];
//        for (int comm_idx = 0; comm_idx < agents.size(); comm_idx += NUM_AGENTS_PER_COMM) {
//            int byteLength = 0;
//            for (int idx = 0; idx < NUM_AGENTS_PER_COMM; idx++) {
//                byteLength += agents.get(comm_idx + idx).byteLength();
//            }
//            byte[] byteBuffer = new byte[byteLength];
//            int offset = 0;
//            for (int idx = 0; idx < NUM_AGENTS_PER_COMM; idx++) {
//                agents.get(comm_idx + idx).serializeToBytes(byteBuffer, offset);
//                offset += agents.get(comm_idx + idx).byteLength();
//            }
//            offset = 0;
//            for (int idx = 0; idx < NUM_AGENTS_PER_COMM; idx++) {
//                Agent currentAgent = Agent.deserializeFromBytes(byteBuffer, offset);
//                readBuffer[idx] = currentAgent;
//                offset += currentAgent.byteLength();
//            }
//            assert agents.get(comm_idx).current_travel_time == readBuffer[0].current_travel_time;
//            assert agents.get(comm_idx + NUM_AGENTS_PER_COMM - 1).current_travel_time == readBuffer[NUM_AGENTS_PER_COMM - 1].current_travel_time;
//            assert agents.get(comm_idx).peekPlan() == readBuffer[0].peekPlan();
//            assert agents.get(comm_idx + NUM_AGENTS_PER_COMM - 1).peekPlan() == readBuffer[NUM_AGENTS_PER_COMM - 1].peekPlan();
//        }
//        long time = System.currentTimeMillis() - start;
//        System.out.println(String.format(
//                "binary runtime [s]: %s; ser+deser per second: %s",
//                String.valueOf(time / (double)1000),
//                String.valueOf(NUM_AGENTS / (time / (double)1000))
//        ));
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        List<Agent> agents = generateRandomAgents(NUM_AGENTS);

//        System.out.println("agents generated. press enter to continue with benchmark");
//        System.in.read();
        runJSONBenchmark(agents);
        runProtostuffBenchmark(agents);
        runBinaryBenchmark(agents);
    }
}
