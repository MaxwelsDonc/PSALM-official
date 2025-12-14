package paper.pss.exp.jackson_project.generation.phase1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import paper.pss.exp.jackson_project.model.TestCase;

public class phase1_art_generator {

    private static final Random random = new Random(System.currentTimeMillis());
    private static final int MAX_RANGE = 999_999_999;
    private final int candidateNum;

    public phase1_art_generator() {
        this.candidateNum = 10;
    }

    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();

        // Step 1: generate the first random test case
        int firstValue = generateRandomInt();
        testCases.add(new TestCase(Integer.toString(firstValue), null));

        // Step 2: generate remaining test cases using ART strategy
        while (testCases.size() < count) {
            String bestCandidate = null;
            int bestMinDist = -1;

            for (int i = 0; i < candidateNum; i++) {
                int candidateValue = generateRandomInt();

                int minDist = Integer.MAX_VALUE;
                for (TestCase existing : testCases) {
                    int existingValue = Integer.parseInt(existing.getInput());
                    int dist = distance(candidateValue, existingValue);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }

                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    bestCandidate = Integer.toString(candidateValue);
                }
            }

            testCases.add(new TestCase(bestCandidate, null));
        }

        return testCases;
    }

    /** 计算两个整数之间的绝对距离 */
    private int distance(int a, int b) {
        return Math.abs(a - b);
    }

    /** 生成随机整数，允许为负值 */
    private int generateRandomInt() {
        boolean isNegative = random.nextBoolean();
        int value = random.nextInt(MAX_RANGE + 1);
        return isNegative ? -value : value;
    }

    public static void main(String[] args) {
        phase1_art_generator generator = new phase1_art_generator(); // candidate pool size = 10
        List<TestCase> testCases = generator.generate(5);

        for (int i = 0; i < testCases.size(); i++) {
            System.out.println("ART Test #" + (i + 1) + ": " + testCases.get(i));
        }
    }
}
