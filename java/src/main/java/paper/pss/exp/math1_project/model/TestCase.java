package paper.pss.exp.math1_project.model;

import java.util.Arrays;

public class TestCase {
    private final double[] x; // 第一个输入数组
    private final double[] h; // 第二个输入数组
    private final int partitionId; // 分区ID

    public TestCase(double[] x, double[] h, int partitionId) {
        this.x = Arrays.copyOf(x, x.length);
        this.h = Arrays.copyOf(h, h.length);
        this.partitionId = partitionId;
    }

    public double[] getX() {
        return Arrays.copyOf(x, x.length);
    }

    public double[] getH() {
        return Arrays.copyOf(h, h.length);
    }

    public int getPartitionId() {
        return partitionId;
    }

    @Override
    public String toString() {
        return String.format("Partition %d: convolve(%s, %s)",
                partitionId, Arrays.toString(x), Arrays.toString(h));
    }
}