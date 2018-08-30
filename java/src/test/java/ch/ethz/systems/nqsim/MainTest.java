package ch.ethz.systems.nqsim;

public final class MainTest {

    public static void main(String[] args) {
        try {
            TwoNodesTest test = new TwoNodesTest();
            test.setUp();
            test.testSingleAgent();
            test.testFullCapacity();

            ThreeNodesTest test2 = new ThreeNodesTest();
            test2.setUp();
            test2.testSingleAgent();

            JSONTest test3 = new JSONTest();
            test3.testAgentSerialization();
            test3.testLoadingFromJSON();

            ProtostuffTest test4 = new ProtostuffTest();
            test4.testAgentSerialization();
        }
        catch (Exception e) {
            System.out.println(String.format(
                "caught %s:%s",
                e.getClass(),
                e.getMessage()
            ));
            e.printStackTrace();
        }
    }
}
