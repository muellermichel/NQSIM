package ch.ethz.systems.nqsim;

public final class ChineseCityTest {

    public static void main(String[] args) {
        try {
            ChineseCity chineseCity = new ChineseCity();
            chineseCity.run(args, "chinese_capital_187x187.json", 2);
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
