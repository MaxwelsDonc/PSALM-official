package paper.pss.exp.math2_project.metamorphicRelation;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * MR3_relation.java - Sign Inversion Relation
 * 
 * This metamorphic relation verifies the sign inversion property of the copySign operation:
 * Verifies that copySign(magnitude, -sign) == -copySign(magnitude, sign)
 * When the sign is inverted, the result should also be inverted.
 */
public class MR3_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR3";
    }

    @Override
    public String getDescription() {
        return "Sign Inversion Relation - verifies that copySign(magnitude, -sign) == -copySign(magnitude, sign)";
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

            // Invert the sign
            long invertedSign = -sign;

            // Create follow-up test case
            TestCase followupTest = new TestCase(
                    magnitude, // Keep magnitude unchanged
                    invertedSign, // Use inverted sign
                    sourceTest.getPartitionId() // Keep partition ID unchanged
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // If exception occurs, log and return empty list
            System.err.println("Error generating MR3 follow-up test: " + e.getMessage());
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

        // Normal case: verify copySign(magnitude, -sign) == -copySign(magnitude, sign)
        return followupResult == -sourceResult;
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