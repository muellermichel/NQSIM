package ch.ethz.systems.matsimtooling;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

class EventValidator {
    private Map<String, Map<String, Map<String, StringlyEvent>>> eventMap;

    EventValidator(StringlyEvents events) {
        this.eventMap = new HashMap<>();
        for (StringlyEvent event:events.events) {
            Map<String, Map<String, StringlyEvent>> eventMapForTime = eventMap.computeIfAbsent(
                event.time,
                k -> new HashMap<>()
            );
            Map<String, StringlyEvent> eventMapForTimeAndPerson = eventMapForTime.computeIfAbsent(
                event.person,
                k -> new HashMap<>()
            );
            if (eventMapForTimeAndPerson.containsKey(event.type)) {
                throw new RuntimeException(String.format(
                    "event with time %s x person %s x type %s is not unique",
                    event.time,
                    event.person,
                    event.type
                ));
            }
            eventMapForTimeAndPerson.put(event.type, event);
        }
    }

    void validate(EventValidator reference) {
        Set<String> remainingTimeSet = new HashSet<>();
        remainingTimeSet.addAll(reference.eventMap.keySet());
        for (Map.Entry<String, Map<String, Map<String, StringlyEvent>>> e:this.eventMap.entrySet()) {
            String time = e.getKey();
            if (!remainingTimeSet.remove(time)) {
                throw new RuntimeException("no events found during time " + time + " in reference");
            }
            Map<String, Map<String, StringlyEvent>> eventMapForTime = e.getValue();
            Map<String, Map<String, StringlyEvent>> refEventMapForTime = reference.eventMap.get(time);
            Set<String> remainingPersonSet = new HashSet<>();
            remainingPersonSet.addAll(refEventMapForTime.keySet());
            for (Map.Entry<String, Map<String, StringlyEvent>> personEntry:eventMapForTime.entrySet()) {
                String person = personEntry.getKey();
                if (!remainingPersonSet.remove(person)) {
                    throw new RuntimeException(String.format(
                        "no events found in reference during time %s for person %s",
                        time,
                        person
                    ));
                }
                Map<String, StringlyEvent> eventMapForTimeAndPerson = personEntry.getValue();
                Map<String, StringlyEvent> refEventMapForTimeAndPerson = refEventMapForTime.get(person);
                Set<String> remainingTypeSet = new HashSet<>();
                remainingTypeSet.addAll(refEventMapForTimeAndPerson.keySet());
                for (Map.Entry<String, StringlyEvent> typeEntry:eventMapForTimeAndPerson.entrySet()) {
                    String type = typeEntry.getKey();
                    if (!remainingTypeSet.remove(type)) {
                        throw new RuntimeException(String.format(
                            "no events found in reference during time %s for person %s and type %s",
                            time,
                            person,
                            type
                        ));
                    }
                    StringlyEvent event = typeEntry.getValue();
                    StringlyEvent refEvent = refEventMapForTimeAndPerson.get(type);
                    if (!event.equals(refEvent)) {
                        throw new RuntimeException(event.toString() + " does not match " + refEvent.toString());
                    }
                }
                if (!remainingTypeSet.isEmpty()) {
                    throw new RuntimeException(String.format(
                        "reference at time %s for person %s contains additional events of type %s",
                        time,
                        person,
                        String.join(",", remainingTypeSet)
                    ));
                }
            }
            if (!remainingPersonSet.isEmpty()) {
                throw new RuntimeException(String.format(
                    "reference at time %s contains additional events for persons %s",
                    time,
                    String.join(",", remainingPersonSet)
                ));
            }
        }
        if (!remainingTimeSet.isEmpty()) {
            throw new RuntimeException(
                "reference contains additional events in times " + String.join(",", remainingTimeSet)
            );
        }
    }
}

public final class StringlyEventlogTool {

    public static byte[] decompress(byte[] contentBytes){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try{
            IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        } catch(IOException e){
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    public static StringlyEvents readBinary(ByteBuffer buf) {
        //TODO
        return null;
    }

    public static StringlyEvents readXML(byte[] input) {
        StringlyEvents events;
        try {
            events = new XmlMapper().readValue(input, StringlyEvents.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return events;
    }

    public static StringlyEvents readGzipXMLFile(String filePath) {
        try {
            return readXML(decompress(IOUtils.toByteArray(new FileInputStream(new File(filePath)))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StringlyEvents readXMLFile(String filePath) {
        try {
            return readXML(IOUtils.toByteArray(new FileInputStream(new File(filePath))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeXMLFile(String filePath, StringlyEvents events) {
        try {
            XmlMapper mapper = new XmlMapper();
            mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            mapper.writeValue(new File(filePath), events);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   public static void validate(StringlyEvents events, StringlyEvents refEvents) {
        EventValidator eventValidator = new EventValidator(events);
        eventValidator.validate(new EventValidator(refEvents));
   }
}
