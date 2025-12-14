package paper.pss.exp.math2_project.metamorphicRelation;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * MR2_relation.java - Magnitude Scaling Relation
 * 
 * This metamorphic relation verifies the magnitude scaling property of the copySign operation:
 * For non-zero scaling factor k, verifies that copySign(magnitude * k, sign) == copySign(magnitude, sign) * k
 * When a scaling factor k is applied to magnitude, the copySign result should also be scaled by the same factor.
 */
public class MR2_relation implements MetamorphicRelation {

    // Specified scaling factors
    private static final long[] SCALING_FACTORS = { 2L, -2L };

    @Override
    public String getId() {
        return "MR2";
    }

    @Override
    public String getDescription() {
        return "Magnitude Scaling Relation - verifies that for non-zero scaling factor k, " +
               "copySign(magnitude * k, sign) == copySign(magnitude, sign) * k";
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

            // Generate a follow-up test case for each scaling factor
            for (long k : SCALING_FACTORS) {
                // Scale magnitude
                long scaledMagnitude = magnitude * k;

                // Create follow-up test case
                TestCase followupTest = new TestCase(
                        scaledMagnitude, // Scaled magnitude
                        sign, // Keep sign unchanged
                        sourceTest.getPartitionId() // Keep partition ID unchanged
                );

                followupTests.add(followupTest);
            }
        } catch (Exception e) {
            // If exception occurs, log and return empty list
            System.err.println("Error generating MR2 follow-up test: " + e.getMessage());
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

        // Get scaling factor k (derived from magnitude ratio)
        long magnitude = sourceTest.getMagnitude();
        long scaledMagnitude = followupTest.getMagnitude();

        // Special case handling: when magnitude is 0
        if (magnitude == 0) {
            // If magnitude is 0, result should always be 0, regardless of scaling factor
            return followupResult == 0;
        }

        // Calculate scaling factor
        long k = scaledMagnitude / magnitude;

        // Verify relation: copySign(magnitude * k, sign) == copySign(magnitude, sign) * k
        return followupResult == sourceResult * k;
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