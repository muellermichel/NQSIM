package ch.ethz.systems.nqsim;

import java.util.Arrays;

public final class ChineseCityTest {

    public static void main(String[] args) {
        System.out.println("process started with args: " + Arrays.toString(args));
        try {
            ChineseCity chineseCity = new ChineseCity(args, "chinese_capital_187x187.json");
            chineseCity.initializeRandomAgents(4000);

//            chineseCity.world.getNodes();

            chineseCity.run();
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
