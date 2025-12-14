package paper.pss.exp.math1_project.model;

import java.util.List;

/**
 * MetamorphicRelation.java
 *
 * This interface defines metamorphic relations used for testing the convolve
 * function.
 * Each metamorphic relation describes how the output should change when the
 * input is
 * transformed in a specific way, and provides methods for generating follow-up
 * test
 * cases and verifying the relation property.
 *
 * The framework includes five metamorphic relations:
 * - MR1: Commutative Property - verifies that convolve(x,h) == convolve(h,x)
 * - MR2: Associative Property - verifies that convolve(convolve(x,h1),h2) ==
 * convolve(x,convolve(h1,h2))
 * - MR3: Distributive Property - verifies that convolve(x,add(h1,h2)) ==
 * add(convolve(x,h1),convolve(x,h2))
 * - MR4: Linear Scaling Property - verifies that convolve(k*x,h) ==
 * k*convolve(x,h)
 * - MR5: Shift Property - verifies that shifting input arrays produces the
 * expected shift in output
 */
public interface MetamorphicRelation {

    /**
     * Gets the relation identifier
     *
     * @return relation ID (e.g., "MR1")
     */
    String getId();

    /**
     * Gets the human-readable description of the relation
     *
     * @return description text
     */
    String getDescription();

    /**
     * Generates follow-up test cases based on a source test case
     *
     * @param sourceTest The original test case
     * @return List of generated follow-up test cases
     */
    List<TestCase> generateFollowupTests(TestCase sourceTest);

    /**
     * Verifies if the test results satisfy the metamorphic relation
     *
     * @param sourceTest        The source test case
     * @param followupTest      The follow-up test case
     * @param sourceResult      The result of the source test
     * @param followupResult    The result of the follow-up test
     * @param sourceExecution   Error message/type from source execution (empty if
     *                          no error)
     * @param followupExecution Error message/type from follow-up execution (empty
     *                          if no error)
     * @return true if the relation is satisfied
     */
    boolean verifyRelation(TestCase sourceTest, TestCase followupTest,
            double[] sourceResult, double[] followupResult,
            String sourceExecution, String followupExecution);

    /**
     * Determines if this relation is applicable to a given test case
     *
     * @param testCase The test case to check
     * @return true if the relation is applicable
     */
    boolean isApplicableTo(TestCase testCase);

    /**
     * Creates metamorphic groups for a given source test case
     *
     * @param sourceTest The source test case
     * @return List of metamorphic groups (one for each follow-up test)
     */
    List<MetamorphicGroup> createGroups(TestCase sourceTest);

}