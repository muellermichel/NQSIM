package ch.ethz.systems.nqsim2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mpi.MPI;
import mpi.Request;
import mpi.Status;

public class Communicator {

    // Communication buffers. One buffer per neighbor realm.
    private final Map<Integer, ByteBuffer> rcvBufsByRealmId;
    private final Map<Integer, ByteBuffer> sndBufsByRealmId;
    private final Set<Request> sndRequests;
    // Current realm.
    private final Realm realm;
    // All the agents in the world.
    private final Agent[] agents;
    // Buffer capacity for each neighbor realm.
    private final int bufferCapacity = 1024*1024;

    public Communicator(Realm realm, Agent[] agents) throws Exception {
        MPI.COMM_WORLD.setErrhandler(MPI.ERRORS_ARE_FATAL);
        this.rcvBufsByRealmId = new HashMap<>();
        this.sndBufsByRealmId = new HashMap<>();
        this.sndRequests = new HashSet<>();
        this.realm = realm;
        this.agents = agents;

        for (LinkBoundary inlink : realm.inLinks()) {
            int realmid = inlink.fromrealm();
            if (!rcvBufsByRealmId.containsKey(realmid)) {
                rcvBufsByRealmId.put(realmid, ByteBuffer.allocateDirect(bufferCapacity));
            }
        }
        for (LinkBoundary outlink : realm.outLinks()) {
            int realmid = outlink.torealm();
            if (!sndBufsByRealmId.containsKey(realmid)) {
                sndBufsByRealmId.put(realmid, ByteBuffer.allocateDirect(bufferCapacity));
            }
        }
    }

    public void waitSends() throws Exception {
        assert(!sndRequests.isEmpty());
        System.out.println(String.format("%d wait ISends...", realm.id()));
        Status[] statuses = Request.waitAllStatus(sndRequests.toArray(new Request[sndBufsByRealmId.size()]));
        for (Status status : statuses) {
            System.out.println(String.format("wait for status %d", status.getError()));

        }
        System.out.println(String.format("%d wait ISends...done!", realm.id()));
        sndRequests.clear();
    }

    private static void copyBB(ByteBuffer from, ByteBuffer to) {
       from.rewind();//copy from the beginning
       to.put(from);
       from.rewind();
       to.flip();
    }

    private void sendBuffers(int tag) throws Exception {
        for (Integer toid : sndBufsByRealmId.keySet()) {
            ByteBuffer bb = sndBufsByRealmId.get(toid);
            ByteBuffer mpibb = MPI.newByteBuffer(bufferCapacity);
            int bytes = bb.position();
            copyBB(bb, mpibb);
            System.out.println(String.format("%d sendBuffers to %d, %d bytes, time %d...", realm.id(), toid, bytes, tag));
            sndRequests.add(MPI.COMM_WORLD.iSend(mpibb, bytes, MPI.BYTE, toid, tag));
            System.out.println(String.format("%d sendBuffers to %d, %d bytes, time %d...done!", realm.id(), toid, bytes, tag));
            bb.clear();
        }
    }

    public void recvBuffers(int tag) throws Exception {
        for (Integer fromid : rcvBufsByRealmId.keySet()) {
            System.out.println(String.format("%d recvBuffers from %d, time %d...", realm.id(), fromid, tag));
            ByteBuffer bb = rcvBufsByRealmId.get(fromid);
            ByteBuffer mpibb = MPI.newByteBuffer(bufferCapacity);
            bb.clear();
            Status status = MPI.COMM_WORLD.recv(mpibb, bufferCapacity, MPI.BYTE, fromid, tag);
            copyBB(mpibb, bb);
            bb.limit(status.getCount(MPI.BYTE));
            System.out.println(String.format("%d recvBuffers from %d, %d bytes, time %d...done!", realm.id(), fromid, bb.remaining(), tag));
        }

    }

    public void sendAgents(Map<LinkBoundary, ArrayList<Agent>> outAgentsByLinkId) throws Exception {
        for (Map.Entry<LinkBoundary,ArrayList<Agent>> entry : outAgentsByLinkId.entrySet()) {
            LinkBoundary blink = entry.getKey();
            ArrayList<Agent> outgoing = entry.getValue();
            ByteBuffer bb = sndBufsByRealmId.get(blink.torealm());
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
        sendBuffers(realm.time()); // TODO - hack!!
    }

    public Map<Integer, ArrayList<Agent>> receiveAgents() throws Exception {
        recvBuffers(realm.time()); // TODO - hack, other is here!
        Map<Integer, ArrayList<Agent>> inAgentsByLinkId = new HashMap<>();
        for (ByteBuffer bb : rcvBufsByRealmId.values()) {
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
        for (LinkBoundary link : realm.inLinks()) {
            ByteBuffer bb = sndBufsByRealmId.get(link.fromrealm());
            bb.putInt(link.id());
            bb.putInt(routedAgentsByLinkId.get(link.id()));
        }
        sendBuffers(realm.time() + 1);
    }
    
    public Map<Integer, Integer> receiveRoutedCounters() throws Exception {
        Map<Integer, Integer> routedAgentsByLinkId = new HashMap<>();
        recvBuffers(realm.time() + 1); // TODO - one is here!
        for (ByteBuffer bb : rcvBufsByRealmId.values()) {
            while (bb.remaining() > 0) {
                int linkid = bb.getInt();
                int counter = bb.getInt();
                routedAgentsByLinkId.put(linkid, counter);
            }
        }
        return routedAgentsByLinkId;
    }
}