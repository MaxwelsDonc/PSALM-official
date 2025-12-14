package paper.pss.exp.math2_project.model;

/**
 * TestCase.java
 * 
 * This class represents a test case for the copySign function.
 * Each test case contains the input parameters (magnitude and sign) and a partition ID.
 */
public class TestCase {
    private final long magnitude; // First input parameter
    private final long sign; // Second input parameter
    private final int partitionId; // Partition ID for test organization

    /**
     * Creates a new test case with the specified parameters
     * 
     * @param magnitude The magnitude value
     * @param sign The sign value
     * @param partitionId The partition ID
     */
    public TestCase(long magnitude, long sign, int partitionId) {
        this.magnitude = magnitude;
        this.sign = sign;
        this.partitionId = partitionId;
    }

    /**
     * Gets the magnitude value
     * 
     * @return The magnitude value
     */
    public long getMagnitude() {
        return magnitude;
    }

    /**
     * Gets the sign value
     * 
     * @return The sign value
     */
    public long getSign() {
        return sign;
    }

    /**
     * Gets the partition ID
     * 
     * @return The partition ID
     */
    public int getPartitionId() {
        return partitionId;
    }

    /**
     * Returns a string representation of the test case
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("Partition %d: copySign(%d, %d)",
                partitionId, magnitude, sign);
    }

    /**
     * Checks equality with another object
     * 
     * @param obj The object to compare with
     * @return True if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TestCase testCase = (TestCase) obj;
        return magnitude == testCase.magnitude &&
               sign == testCase.sign &&
               partitionId == testCase.partitionId;
    }

    /**
     * Returns the hash code for this test case
     * 
     * @return Hash code
     */
    @Override
    public int hashCode() {
        return Long.hashCode(magnitude) ^ Long.hashCode(sign) ^ Integer.hashCode(partitionId);
    }
}