package paper.pss.exp.math2_project.model;

/**
 * MetamorphicGroup.java
 * 
 * This file defines the data structure for metamorphic groups used in
 * metamorphic testing of the copySign function.
 * A metamorphic group represents a pair of test cases: a source test case and a
 * follow-up test case, along with the identifier of the metamorphic relation they're testing.
 * 
 * In this simplified model:
 * - Each MetamorphicGroup contains exactly one source test and one follow-up test
 * - Multiple follow-up tests for the same source require multiple MetamorphicGroup instances
 * - Verification logic is handled by the metamorphic relation classes, not in this data structure
 */
public class MetamorphicGroup {
    private final String mrId; // Metamorphic relation ID (e.g., "MR1")
    private final String description; // Brief description of the relation
    private final TestCase sourceTest; // Original test case
    private final TestCase followupTest; // Derived follow-up test case

    /**
     * Creates a metamorphic group with a source and follow-up test case
     * 
     * @param mrId         Metamorphic relation ID (e.g., "MR1")
     * @param description  Brief description of the relation
     * @param sourceTest   Source test case
     * @param followupTest Follow-up test case
     */
    public MetamorphicGroup(String mrId, String description, TestCase sourceTest, TestCase followupTest) {
        this.mrId = mrId;
        this.description = description;
        this.sourceTest = sourceTest;
        this.followupTest = followupTest;
    }

    /**
     * Gets the metamorphic relation ID
     * 
     * @return Relation ID (e.g., "MR1")
     */
    public String getMRId() {
        return mrId;
    }

    /**
     * Gets the description of the metamorphic relation
     * 
     * @return Description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the source test case
     * 
     * @return Source test case
     */
    public TestCase getSourceTest() {
        return sourceTest;
    }

    /**
     * Gets the follow-up test case
     * 
     * @return Follow-up test case
     */
    public TestCase getFollowupTest() {
        return followupTest;
    }

    /**
     * Returns a string representation of the metamorphic group
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("MetamorphicGroup[%s]: %s -> %s", 
                mrId, sourceTest.toString(), followupTest.toString());
    }
}