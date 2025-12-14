package paper.pss.exp.math2_project.metamorphicRelation;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * MR4_relation.java - Idempotence Relation
 * 
 * This metamorphic relation verifies the idempotence property of the copySign operation:
 * Verifies that copySign(copySign(magnitude, sign), sign) == copySign(magnitude, sign)
 * Applying copySign twice with the same sign should yield the same result as applying it once.
 */
public class MR4_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR4";
    }

    @Override
    public String getDescription() {
        return "Idempotence Relation - verifies that copySign(copySign(magnitude, sign), sign) == copySign(magnitude, sign)";
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
            long magnitude = sourceTest.getMagnitude();
            long sign = sourceTest.getSign();

            // Calculate the result of the first copySign operation
            long firstResult;
            if ((magnitude < 0 && sign < 0) || (magnitude >= 0 && sign >= 0)) {
                // If magnitude and sign have the same sign, result is magnitude
                firstResult = magnitude;
            } else {
                // If magnitude and sign have different signs, result is -magnitude
                firstResult = -magnitude;
            }

            // Create follow-up test case: use the first calculation result as new magnitude
            TestCase followupTest = new TestCase(
                    firstResult, // Result of first copySign as new magnitude
                    sign, // Keep sign unchanged
                    sourceTest.getPartitionId() // Keep partition ID unchanged
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // If exception occurs, log and return empty list
            System.err.println("Error generating MR4 follow-up test: " + e.getMessage());
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

        // Idempotence test: the result of applying copySign twice should be the same as applying it once
        return followupResult == sourceResult;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // This relation applies to most copySign test cases, but excludes special cases
        if (testCase == null) {
            return false;
        }

        // Exclude cases that would cause overflow: magnitude is Long.MIN_VALUE and sign >= 0
        long magnitude = testCase.getMagnitude();
        long sign = testCase.getSign();
        return !(magnitude == Long.MIN_VALUE && sign >= 0);
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