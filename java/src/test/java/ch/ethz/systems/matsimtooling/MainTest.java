package ch.ethz.systems.matsimtooling;

public class MainTest {

    public static void main(String[] args) {
        EventValidationTest.testEventLogReadWrite();
        EventValidationTest.testEventLogTimingError();
        EventValidationTest.testEventLogError();
        EventValidationTest.testEventLogReadWriteBerlin();
    }
}
