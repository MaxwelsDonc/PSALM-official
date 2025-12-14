package paper.pss.exp.math2_project.metamorphicRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * MR1_relation.java - Sign Consistency Relation
 * 
 * This metamorphic relation verifies the sign consistency property of the copySign operation:
 * For any non-zero positive coefficient k, copySign(magnitude, sign) and copySign(magnitude, sign*k)
 * should have consistent signs, i.e., as long as sign and sign*k have the same sign,
 * the copySign results should maintain the same sign.
 */
public class MR1_relation implements MetamorphicRelation {

    // Random number generator for generating non-zero positive coefficient k
    private static final Random random = new Random();

    // Range for generating non-zero positive coefficient k
    private static final double MIN_K = 1.0;
    private static final double MAX_K = 10.0;

    @Override
    public String getId() {
        return "MR1";
    }

    @Override
    public String getDescription() {
        return "Sign Consistency Relation - verifies that for any non-zero positive coefficient k, " +
               "copySign(magnitude, sign) and copySign(magnitude, sign*k) have consistent signs";
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

            // Generate non-zero positive coefficient k (between 1.0 and 10.0)
            double k = MIN_K + (MAX_K - MIN_K) * random.nextDouble();

            // Calculate new sign value
            long sign2 = (long) (sign * k);

            // Ensure sign2 and sign have the same sign
            if ((sign >= 0 && sign2 < 0) || (sign < 0 && sign2 >= 0)) {
                // If sign flipped, use a different k value
                k = 0.5; // Use a value less than 1
                sign2 = (long) (sign * k);
            }

            // Create follow-up test case
            TestCase followupTest = new TestCase(
                    magnitude, // Keep magnitude unchanged
                    sign2, // Use modified sign2
                    sourceTest.getPartitionId() // Keep partition ID unchanged
            );

            followupTests.add(followupTest);
        } catch (Exception e) {
            // If exception occurs, log and return empty list
            System.err.println("Error generating MR1 follow-up test: " + e.getMessage());
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

        // Verify sign consistency of results
        // If both results are 0, consider signs consistent
        if (sourceResult == 0 && followupResult == 0) {
            return true;
        }

        // Check if both results have consistent signs
        return (sourceResult >= 0 && followupResult >= 0) ||
               (sourceResult < 0 && followupResult < 0);
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