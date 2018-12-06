package ch.ethz.systems.matsimtooling;

public class EventValidationTest {
    public static void testEventLogReadWrite() {
        StringlyEvents events = StringlyEventlogTool.readXMLFile("sample-events.xml");
        StringlyEventlogTool.writeXMLFile("sample-events-output.xml", events);
        StringlyEvents newEvents = StringlyEventlogTool.readXMLFile("sample-events-output.xml");
        StringlyEventlogTool.validate(newEvents, events);
        System.out.println("sample-events.xml can be read and represented correctly");
    }

    public static void testEventLogReadWriteBerlin() {
        System.out.println("parsing berlin")
        StringlyEvents events = StringlyEventlogTool.readGzipXMLFile("berlin-v5.1-1pct.0.events.xml.gz");
        System.out.println("writing out berlin")
        StringlyEventlogTool.writeXMLFile("berlin-events-output.xml", events);
        System.out.println("parsing berlin again")
        StringlyEvents newEvents = StringlyEventlogTool.readXMLFile("berlin-events-output.xml");
        System.out.println("validating the two versions")
        StringlyEventlogTool.validate(newEvents, events);
        System.out.println("berlin-v5.1-1pct.0.events.xml.gz can be read and represented correctly");
    }
}
