package ch.ethz.systems.nqsim;

import mpi.MPIException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ChineseCityTest {

    public static Map<Integer, Map<String, List<String>>> getEventLogFromExperiment(
            int num_agents,
            int num_ranks,
            Communicator communicator
    ) throws CommunicatorException, NodeException, WorldException, InterruptedException, ExceedingBufferException, MPIException, LinkException, IOException, ChineseCityException {
        EventLog.clear();
        communicator.resetData();
        Agent.resetAutoIds();
        Link.resetAutoIds();
        communicator.num_ranks_override = num_ranks;
        if (communicator.getMyRank() == 0) {
            System.out.println(String.format(
                    "==================== Chinese City @ %d ranks, %d agents ==========================",
                    num_ranks,
                    num_agents
            ));
        }
        ChineseCity chineseCity = new ChineseCity("chinese_capital_187x187.json", communicator);
        if (chineseCity.world == null) {
            return null;
        }
        chineseCity.initializeRandomAgents(num_agents);
        chineseCity.run();
        return EventLog.getDataCopy();
    }

    public static void validateEvents (
        Map<Integer, Map<String, List<String>>> event_log,
        Map<Integer, Map<String, List<String>>> reference_event_log
    ) throws WorldException {
        for (Map.Entry<Integer, Map<String, List<String>>> e : event_log.entrySet()) {
            int time = e.getKey();
            Map<String, List<String>> entries_by_agent_id = e.getValue();
            Map<String, List<String>> ref_entries_by_agent_id = reference_event_log.get(time);
            if (ref_entries_by_agent_id == null) {
                throw new WorldException("time " + time + " not found in reference");
            }
            for (Map.Entry<String, List<String>> f : entries_by_agent_id.entrySet()) {
                String agent_id = f.getKey();
                List<String> log_entries = f.getValue();
                List<String> ref_entries = ref_entries_by_agent_id.get(agent_id);
                if (ref_entries == null) {
                    throw new WorldException(String.format("time %d: no reference data for agent %s", time, agent_id));
                }
                if (log_entries.size() != ref_entries.size()) {
                    throw new WorldException(String.format(
                        "time %d: %d instead of %d events for agent %s",
                        time,
                        log_entries.size(),
                        ref_entries.size(),
                        agent_id
                    ));
                }
            }
            for (Map.Entry<String, List<String>> f : ref_entries_by_agent_id.entrySet()) {
                String agent_id = f.getKey();
                List<String> log_entries = ref_entries_by_agent_id.get(agent_id);
                if (log_entries == null) {
                    throw new WorldException(String.format("time %d: no data for agent %s", time, agent_id));
                }
            }
        }
    }

    public static void runReferenceTests(int num_agents, Communicator communicator) throws MPIException {
        try {
            Map<Integer, Map<String, List<String>>> event_log = getEventLogFromExperiment(
                    num_agents,
                    communicator.getNumberOfRanks(),
                    communicator
            );
//            Map<Integer, Map<String, List<String>>> reference_event_log = getEventLogFromExperiment(
//                num_agents,
//                1,
//                communicator
//            );
//            int[] potential_rank_numbers = {2, 4, 16, 25, 36};
//            List<Integer> rank_numbers_to_test = new LinkedList<>();
//            for (int r:potential_rank_numbers) {
//                if (communicator.getNumberOfRanks(false) >= r) {
//                    rank_numbers_to_test.add(r);
//                }
//                else {
//                    break;
//                }
//            }
//            for (int num_ranks : rank_numbers_to_test) {
//                Map<Integer, Map<String, List<String>>> event_log = getEventLogFromExperiment(
//                        num_agents,
//                        num_ranks,
//                        communicator
//                );
////                if (communicator.getMyRank() == 0) {
////                    System.out.println(String.format(
////                        "validating %d agents on %d ranks",
////                        num_agents,
////                        num_ranks
////                    ));
////                    validateEvents(event_log, reference_event_log);
////                    System.out.println("validation OK");
////                }
//            }
        }
        catch (Exception e) {
            System.out.println(String.format(
                    "%d caught %s:%s; last num ranks: %d; last num agents: %d",
                    communicator.getMyRank(),
                    e.getClass(),
                    e.getMessage(),
                    communicator.getNumberOfRanks(),
                    num_agents
            ));
            System.out.println("------------stack trace--------------");
            e.printStackTrace();
//            System.out.println("------------event log showing error--------------");
//            EventLog.print_all();
            System.exit(2);
        }
    }

    public static void main(String[] args) throws MPIException {
        System.out.println("process started with args: " + Arrays.toString(args));
        int final_num_agents = 4000;
        if (args.length > 0) {
            final_num_agents = Integer.valueOf(args[0]);
        }
        Communicator communicator = new Communicator(args);
//        if (2 <= final_num_agents) runReferenceTests(2, communicator);
//        if (100 <= final_num_agents) runReferenceTests(100, communicator);
//        if (final_num_agents != 2 && final_num_agents != 100) runReferenceTests(final_num_agents, communicator);
        runReferenceTests(final_num_agents, communicator);
        communicator.shutDown();
    }
}
