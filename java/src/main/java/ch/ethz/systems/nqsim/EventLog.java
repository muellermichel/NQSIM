package ch.ethz.systems.nqsim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;
import java.util.*;

public class EventLog {
    private Map<Integer, Map<String, List<String>>> entries_by_agent_id_by_time;
    private Map<String, List<String>> current_entries_by_agent_id;
    private int time;
    private static final EventLog instance = new EventLog();

    private EventLog() {
        this.resetInstance();
    }

    public static void clear() {
        instance.resetInstance();
    }

    public void resetInstance() {
        this.entries_by_agent_id_by_time = new TreeMap<>();
        this.time = -1;
    }

    public static void setTime(int time) {
        instance.time = time;
        instance.current_entries_by_agent_id = instance.entries_by_agent_id_by_time
            .computeIfAbsent(time, k -> new TreeMap<>());
    }

    public static Map<Integer, Map<String, List<String>>> getDataCopy() {
        Map<Integer, Map<String, List<String>>> copy = new TreeMap<>();
        for (Map.Entry<Integer, Map<String, List<String>>> e : instance.entries_by_agent_id_by_time.entrySet()) {
            int time = e.getKey();
            Map<String, List<String>> entries_by_agent_id = e.getValue();
            Map<String, List<String>> copied_entries_by_agent_id = copy.computeIfAbsent(time, k -> new TreeMap<>());
            for (Map.Entry<String, List<String>> f : entries_by_agent_id.entrySet()) {
                String agent_id = f.getKey();
                List<String> log_entries = f.getValue();
                List<String> copied_entries = copied_entries_by_agent_id.computeIfAbsent(agent_id, k -> new LinkedList<>());
                for (String log_entry : log_entries) {
                    copied_entries.add(log_entry);
                }
            }
        }
        return copy;
    }

    public static void log(String agent_id, String entry) {
        List<String> entries = instance.current_entries_by_agent_id.computeIfAbsent(agent_id, k -> new LinkedList<>());
        entries.add(entry);
    }

    public static String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(instance.entries_by_agent_id_by_time);
    }

    public static void mergeJson(String json) throws IOException, WorldException {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, Map<String, List<String>>> new_entries_by_agent_id_by_time;
        try {
            new_entries_by_agent_id_by_time = mapper.readValue(
                    json,
                    new TypeReference<TreeMap<Integer, TreeMap<String, LinkedList<String>>>>() {
                    }
            );
        }
        catch (MismatchedInputException e) {
            throw new WorldException(e.getMessage() + "; received json: " + json);
        }
        for (Map.Entry<Integer, Map<String, List<String>>> e : new_entries_by_agent_id_by_time.entrySet()) {
            int time = e.getKey();
            Map<String, List<String>> entries_by_agent_id = instance.entries_by_agent_id_by_time.get(time);
            if (entries_by_agent_id == null) {
                throw new WorldException("time cannot be merged");
            }
            Map<String, List<String>> new_entries_by_agent_id = e.getValue();
            for (Map.Entry<String, List<String>> f : new_entries_by_agent_id.entrySet()) {
                String agent_id = f.getKey();
                List<String> log_entries = f.getValue();
                if (entries_by_agent_id.get(agent_id) != null) {
                    throw new WorldException("merge conflict");
                }
                entries_by_agent_id.put(agent_id, log_entries);
            }
        }

    }

    public static void print_all() {
        for (Map.Entry<Integer, Map<String, List<String>>> e : instance.entries_by_agent_id_by_time.entrySet()) {
            int time = e.getKey();
            Map<String, List<String>> entries_by_agent_id = e.getValue();
            for (Map.Entry<String, List<String>> f : entries_by_agent_id.entrySet()) {
                String agent_id = f.getKey();
                List<String> log_entries = f.getValue();
                for (String log_entry : log_entries) {
                    System.out.println(String.format("t%d, %s: %s", time, agent_id, log_entry));
                }
            }
        }
    }
}
