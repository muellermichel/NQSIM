package ch.ethz.systems.nqsim;

import java.util.Arrays;

public final class ChineseCityTest {

    public static void main(String[] args) {
        System.out.println("process started with args: " + Arrays.toString(args));
        int num_agents = 4000;
        if (args.length > 0) {
            num_agents = Integer.valueOf(args[0]);
        }
        try {
            ChineseCity chineseCity = new ChineseCity(args, "chinese_capital_187x187.json");
            chineseCity.initializeRandomAgents(num_agents);
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
