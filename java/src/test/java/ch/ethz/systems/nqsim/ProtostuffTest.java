package ch.ethz.systems.nqsim;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProtostuffTest {
    void testAgentSerialization() throws IOException {
//        Plan plan = new Plan(new byte[]{(byte) 0, (byte) 0});
//        Agent agentBefore = new Agent(String.valueOf(0), plan, 1, 2);
//
//        Schema<Agent> schema = RuntimeSchema.getSchema(Agent.class);
//        LinkedBuffer buffer = LinkedBuffer.allocate(512);
//
//        final byte[] protostuff;
//        try {
//            protostuff = ProtostuffIOUtil.toByteArray(agentBefore, schema, buffer);
//        }
//        finally {
//            buffer.clear();
//        }
//        Agent agentAfter = schema.newMessage();
//        ProtostuffIOUtil.mergeFrom(protostuff, agentAfter, schema);
//        assert agentAfter.current_travel_time == 1 : agentAfter.current_travel_time;
//        assert agentAfter.time_to_pass_link == 2 : agentAfter.time_to_pass_link;
//        assert agentAfter.pollPlan() == (byte) 0;
//        assert agentAfter.getPlan().size() == 1;
    }
}
