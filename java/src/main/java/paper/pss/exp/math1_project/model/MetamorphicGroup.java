package paper.pss.exp.math1_project.model;

/**
 * 表示蜕变测试中的测试组，包含一个源测试用例和一个后续测试用例
 */
public class MetamorphicGroup {
    private final String mrId; // 蜕变关系ID (例如, "MR1")
    private final String description; // 蜕变关系描述
    private final TestCase sourceTest; // 源测试用例
    private final TestCase followupTest; // 后续测试用例

    /**
     * 创建一个包含源测试和后续测试用例的蜕变组
     */
    public MetamorphicGroup(String mrId, String description, TestCase sourceTest, TestCase followupTest) {
        this.mrId = mrId;
        this.description = description;
        this.sourceTest = sourceTest;
        this.followupTest = followupTest;
    }

    public String getMRId() {
        return mrId;
    }

    public TestCase getSourceTest() {
        return sourceTest;
    }

    public TestCase getFollowupTest() {
        return followupTest;
    }

    public String getDescription() {
        return description;
    }
}