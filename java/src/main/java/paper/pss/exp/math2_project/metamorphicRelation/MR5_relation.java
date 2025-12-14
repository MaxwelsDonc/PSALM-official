package paper.pss.exp.math2_project.metamorphicRelation;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * MR5_relation.java - Summation Property Relation
 * 
 * This metamorphic relation verifies the summation property of the copySign operation:
 * Verifies that copySign(magnitude1 + magnitude2, sign) == copySign(magnitude1, sign) + copySign(magnitude2, sign)
 * The copySign operation should be distributive over addition.
 */
public class MR5_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR5";
    }

    @Override
    public String getDescription() {
        return "Summation Property Relation - verifies that copySign(magnitude1 + magnitude2, sign) == copySign(magnitude1, sign) + copySign(magnitude2, sign)";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();

        // Return empty list if source test case is not available
        if (sourceTest == null) {
            return followupTests;
        }

        try {
            // Get input parameters from source test
            long magnitude1 = sourceTest.getMagnitude();
            long sign = sourceTest.getSign();

            // Generate a random second magnitude
            long magnitude2 = sourceTest.getMagnitude() * (long) 1.1;

            // Prevent overflow
            try {
                Math.addExact(magnitude1, magnitude2);
            } catch (ArithmeticException e) {
                // If addition overflows, use a small value
                magnitude2 = (magnitude1 > 0) ? -1 : 1;
            }

            // Calculate the sum of two magnitudes
            long sumMagnitude = magnitude1 + magnitude2;

            // Create follow-up test case: use the sum of magnitudes
            TestCase followupTest = new TestCase(
                    sumMagnitude, // Sum of two magnitudes
                    sign, // Keep sign unchanged
                    sourceTest.getPartitionId() // Keep partition ID unchanged
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // If exception occurs, log and return empty list
            System.err.println("Error generating MR5 follow-up test: " + e.getMessage());
        }

        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest,
                                long sourceResult, long followupResult,
                                String sourceExecution, String followupExecution) {

        // Check for any execution errors
        if (!sourceExecution.isEmpty() || !followupExecution.isEmpty()) {
            // If both tests have the same type of error, the relation is still considered satisfied
            return sourceExecution.equals(followupExecution);
        }

        // Get magnitude values from test cases
        long magnitude1 = sourceTest.getMagnitude();
        long magnitude2 = followupTest.getMagnitude() - magnitude1;
        long sign = sourceTest.getSign();

        // Calculate expected result of copySign(magnitude2, sign)
        long expectedMagnitude2Result;

        // Simulate copySign result
        if ((magnitude2 < 0 && sign < 0) || (magnitude2 >= 0 && sign >= 0)) {
            // If magnitude2 and sign have the same sign, result is magnitude2
            expectedMagnitude2Result = magnitude2;
        } else {
            // If magnitude2 and sign have different signs, result is -magnitude2
            // Special case: when magnitude2 is Long.MIN_VALUE and sign >= 0, overflow exception occurs
            if (magnitude2 == Long.MIN_VALUE && sign >= 0) {
                // In this case, the relation is not applicable
                return true; // or return false, depending on test strategy
            }
            expectedMagnitude2Result = -magnitude2;
        }

        // Verify relation: copySign(magnitude1 + magnitude2, sign) == copySign(magnitude1, sign) + copySign(magnitude2, sign)
        long expectedSum;
        try {
            expectedSum = Math.addExact(sourceResult, expectedMagnitude2Result);
        } catch (ArithmeticException e) {
            // If sum overflows, the relation is not applicable
            return true; // or return false, depending on test strategy
        }

        return followupResult == expectedSum;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // This relation applies to all valid copySign test cases
        return testCase != null;
    }

    @Override
    public List<MetamorphicGroup> createGroups(TestCase sourceTest) {
        List<MetamorphicGroup> groups = new ArrayList<>();

        // Generate follow-up test cases
        List<TestCase> followupTests = generateFollowupTests(sourceTest);

        // Create a metamorphic group for each follow-up test
        for (TestCase followupTest : followupTests) {
            groups.add(new MetamorphicGroup(
                    getId(),
                    getDescription(),
                    sourceTest,
                    followupTest));
        }

        return groups;
    }
}