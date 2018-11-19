package ch.ethz.systems.nqsim2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mpi.MPI;
import mpi.Status;

public class Communicator {

    // Communication buffers. One buffer per neighbor realm.
    private final Map<Integer, ByteBuffer> bufsByRealmId;
    // Current realm.
    private final Realm realm;
    // All the agents in the world.
    private final Agent[] agents;
    // Buffer capacity for each neighbor realm.
    private final int bufferCapacity = 256*1024*1024;

    public Communicator(Realm realm, Agent[] agents) throws Exception {
        this.bufsByRealmId = new HashMap<>();
        this.realm = realm;
        this.agents = agents;

        for (LinkBoundary inlink : realm.inLinks()) {
            int realmid = inlink.fromrealm();
            if (!bufsByRealmId.containsKey(realmid)) {
                bufsByRealmId.put(realmid, ByteBuffer.allocateDirect(bufferCapacity));
            }
        }
        for (LinkBoundary outlink : realm.outLinks()) {
            int realmid = outlink.torealm();
            if (!bufsByRealmId.containsKey(realmid)) {
                bufsByRealmId.put(realmid, ByteBuffer.allocateDirect(bufferCapacity));
            }
        }
    }

    private void sendBuffers() throws Exception {
        for (Map.Entry<Integer, ByteBuffer> entry : bufsByRealmId.entrySet()) {
            MPI.COMM_WORLD.iSend(
                entry.getValue(), entry.getValue().position(), 
                MPI.BYTE, entry.getKey(), realm.time());
        }
    }

    public void recvBuffers() throws Exception {
        for (Map.Entry<Integer, ByteBuffer> entry : bufsByRealmId.entrySet()) {
            Status status = MPI.COMM_WORLD.recv(
                entry.getValue(), entry.getValue().capacity(), 
                MPI.BYTE, entry.getKey(), realm.time());
            entry.getValue().limit(status.getCount(MPI.BYTE));
        }
    }

    public void clearBuffers() {
        for (ByteBuffer bb : bufsByRealmId.values()) {
            bb.clear();
        }
    }

    public void sendAgents(Map<LinkBoundary, ArrayList<Agent>> outAgentsByLinkId) throws Exception {
        clearBuffers();
        for (Map.Entry<LinkBoundary,ArrayList<Agent>> entry : outAgentsByLinkId.entrySet()) {
            LinkBoundary blink = entry.getKey();
            ArrayList<Agent> outgoing = entry.getValue();
            ByteBuffer bb = bufsByRealmId.get(blink.torealm());
            // Id of the link
            bb.putInt(blink.id());
            // Number of agents being sent for this link.
            bb.putInt(outgoing.size());
            for (Agent a : outgoing) {
                // Id of the agent.
                bb.putInt(a.id);
                // Plan index of the agent.
                bb.putInt(a.planIndex);
            }
        }
        sendBuffers();
    }

    public Map<Integer, ArrayList<Agent>> receiveAgents() throws Exception {
        clearBuffers();
        recvBuffers();
        Map<Integer, ArrayList<Agent>> inAgentsByLinkId = new HashMap<>();
        for (ByteBuffer bb : bufsByRealmId.values()) {
            while (bb.remaining() > 0) {
                int linkid = bb.getInt();
                int nagents = bb.getInt();
                ArrayList<Agent> recvAgents = new ArrayList<>(nagents);
                for (int j = 0; j < nagents; j++) {
                    int agentid = bb.getInt();
                    int planindex = bb.getInt();
                    agents[agentid].planIndex(planindex);
                    recvAgents.add(agents[agentid]);
                }
                inAgentsByLinkId.put(linkid, recvAgents);
            }
        }
        return inAgentsByLinkId;
    }

    public void sendRoutedCounters(Map<Integer, Integer> routedAgentsByLinkId) throws Exception {
        clearBuffers();
        for (LinkBoundary link : realm.inLinks()) {
            ByteBuffer bb = bufsByRealmId.get(link.fromrealm());
            bb.putInt(link.id());
            bb.putInt(routedAgentsByLinkId.get(link.id()));
        }
        sendBuffers();
    }
    
    public Map<Integer, Integer> receiveRoutedCounters() throws Exception {
        clearBuffers();
        Map<Integer, Integer> routedAgentsByLinkId = new HashMap<>();
        recvBuffers();
        for (ByteBuffer bb : bufsByRealmId.values()) {
            while (bb.remaining() > 0) {
                int linkid = bb.getInt();
                int counter = bb.getInt();
                routedAgentsByLinkId.put(linkid, counter);
            }
        }
        return routedAgentsByLinkId;
    }
}