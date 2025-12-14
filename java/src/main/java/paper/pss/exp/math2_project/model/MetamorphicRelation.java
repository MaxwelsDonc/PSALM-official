package paper.pss.exp.math2_project.model;

import java.util.List;

/**
 * MetamorphicRelation.java
 *
 * This interface defines the contract for metamorphic relations used in testing the copySign function.
 * Each relation describes how changes to inputs should affect outputs and provides
 * methods to generate follow-up tests and verify relation properties.
 *
 * The framework includes five metamorphic relations:
 * - MR1: Sign Consistency Relation - verifies sign consistency with positive scaling factors
 * - MR2: Magnitude Scaling Relation - verifies proportional scaling of magnitude
 * - MR3: Sign Inversion Relation - verifies sign inversion properties
 * - MR4: Idempotence Relation - verifies idempotent properties of copySign
 * - MR5: Summation Property Relation - verifies additive properties
 */
public interface MetamorphicRelation {

    /**
     * Gets the relation identifier
     *
     * @return The relation ID (e.g., "MR1")
     */
    String getId();

    /**
     * Gets a human-readable description of the relation
     *
     * @return Description text
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
     * @param sourceTest     The source test case
     * @param followupTest   The follow-up test case
     * @param sourceResult   Result from the source test
     * @param followupResult Result from the follow-up test
     * @param sourceExecution  Error message/type from source execution (empty if no error)
     * @param followupExecution  Error message/type from followup execution (empty if no error)
     * @return True if the relation is satisfied
     */
    boolean verifyRelation(TestCase sourceTest, TestCase followupTest, long sourceResult, long followupResult, String sourceExecution, String followupExecution);

    /**
     * Determines if this relation can be applied to the given test case
     *
     * @param testCase The test case to check
     * @return True if the relation is applicable
     */
    boolean isApplicableTo(TestCase testCase);

    /**
     * Creates metamorphic groups from a source test case
     *
     * @param sourceTest The source test case
     * @return List of metamorphic groups
     */
    List<MetamorphicGroup> createGroups(TestCase sourceTest);
}