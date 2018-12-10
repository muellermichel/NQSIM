package ch.ethz.systems.matsimtooling;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.GZIPInputStream;

class EventTypeExtractor {
    static Map<String, StringlyEvent> extractEventExamplesByHeader(StringlyEvents events) {
        Map<String, StringlyEvent> result = new TreeMap<>();
        for (StringlyEvent event:events.events) {
            result.putIfAbsent(event.getStringHeader(), event);
        }
        return result;
    }
}

class EventValidator {
    private Map<String, List<StringlyEvent>> eventsByPerson;

    EventValidator(StringlyEvents events) throws ValidationException {
        this.eventsByPerson = new HashMap<>();
        for (StringlyEvent event:events.events) {
            String agentIdentifier = event.person;
            if (agentIdentifier == null) {
                agentIdentifier = event.vehicle;
            }
            if (agentIdentifier == null) {
                agentIdentifier = event.driverId;
            }
            if (agentIdentifier == null) {
                agentIdentifier = event.agent;
            }
            if (agentIdentifier == null) {
                throw new ValidationException("event cannot be represented as there is no agent identifier: " + event.toString());
            }
            List<StringlyEvent> eventsForPerson = eventsByPerson.computeIfAbsent(
                agentIdentifier,
                k -> new ArrayList<>()
            );
            eventsForPerson.add(event);
        }
    }

    void validate(EventValidator reference, boolean exactTimingRequired) throws ValidationException {
        for (Map.Entry<String, List<StringlyEvent>> e:this.eventsByPerson.entrySet()) {
            String agentIdentifier = e.getKey();
            List<StringlyEvent> events = e.getValue();
            List<StringlyEvent> referenceEvents = reference.eventsByPerson.get(agentIdentifier);
            if (referenceEvents == null) {
                throw new ValidationException("no events found in reference for agent " + agentIdentifier);
            }
            if (referenceEvents.size() != events.size()) {
                throw new ValidationException(String.format(
                        "event list size (%d) does not match reference (%d) for agent %s",
                        events.size(),
                        referenceEvents.size(),
                        agentIdentifier
                ));
            }
            for (int event_idx = 0; event_idx < events.size(); event_idx++) {
                if (!events.get(event_idx).equals(referenceEvents.get(event_idx), exactTimingRequired)) {
                    throw new ValidationException(String.format(
                        "event %d for agent %s does not match reference:\n%s\nvs\n%s",
                        event_idx,
                        agentIdentifier,
                        events.get(event_idx).toString(),
                        referenceEvents.get(event_idx).toString()
                    ));
                }
            }
        }
    }
}

public final class StringlyEventlogTool {

    public static byte[] decompress(byte[] contentBytes){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try{
            IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);
        }
        catch(IOException e){
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
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return events;
    }

    public static StringlyEvents readGzipXMLFile(String filePath) {
        try {
            return readXML(decompress(IOUtils.toByteArray(new FileInputStream(new File(filePath)))));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StringlyEvents readXMLFile(String filePath) {
        try {
            return readXML(IOUtils.toByteArray(new FileInputStream(new File(filePath))));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeXMLFile(String filePath, StringlyEvents events) {
        try {
            XmlMapper mapper = new XmlMapper();
            mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
            mapper.writeValue(new File(filePath), events);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void printEventTypesAndExamples(StringlyEvents events) {
        Map<String,StringlyEvent> sampleEventsByHeader = EventTypeExtractor.extractEventExamplesByHeader(events);
        int typeIdx = 0;
        for (Map.Entry<String, StringlyEvent> e:sampleEventsByHeader.entrySet()) {
            System.out.println(String.format(
                "%d: %s  ->  %s",
                typeIdx,
                e.getKey(),
                e.getValue().toString()
            ));
            typeIdx++;
        }
    }

    public static void validate(StringlyEvents events, StringlyEvents refEvents, boolean exactTimingRequired) throws ValidationException {
        EventValidator eventValidator = new EventValidator(events);
        eventValidator.validate(new EventValidator(refEvents), exactTimingRequired);
    }
}
