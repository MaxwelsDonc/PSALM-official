package paper.pss.exp.jackson_project.generation.phase1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jackson_project.model.TestCase;

/**
 * random_generator.java
 *
 * This class implements a pure random test generation strategy for the
 * parseInt function. It generates string representations of integers,
 * without partitioning logic and without leading zeros.
 */
public class phase1_random_generator {

    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * Generates the specified number of random test cases.
     *
     * @param count The number of test cases to generate
     * @return A list of randomly generated test cases
     */
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String input = generateRandomIntString();
            testCases.add(new TestCase(input, null)); // No partition assigned
        }

        return testCases;
    }

    /**
     * Generates a random integer string without leading zeros.
     *
     * @return A string like "-42", "123456789", "0"
     */
    private String generateRandomIntString() {
        int maxRange = 999_999_999;
        int randomValue;

        boolean isNegative = random.nextBoolean();
        if (isNegative) {
            randomValue = -random.nextInt(maxRange + 1);
        } else {
            randomValue = random.nextInt(maxRange + 1);
        }

        return Integer.toString(randomValue);
    }

    /**
     * Main method for testing the generator
     */
    public static void main(String[] args) {
        phase1_random_generator generator = new phase1_random_generator();
        List<TestCase> testCases = generator.generate(3);

        for (int i = 0; i < testCases.size(); i++) {
            System.out.println("Test #" + (i + 1) + ": " + testCases.get(i) + "\n");
        }
    }
}
