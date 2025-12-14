package paper.pss.exp.jfreeChart_project.model;

import java.awt.Shape;
import java.util.List;

/**
 * MetamorphicRelation.java
 *
 * 这个接口定义了用于测试createLineRegion函数的蜕变关系。
 * 每个蜕变关系描述了如何在输入发生变化时输出应该如何变化，并提供了
 * 生成后续测试用例和验证关系属性的方法。
 *
 * 框架包括六个蜕变关系：
 * - MR1: 宽度比例关系 - 检查宽度变化时区域面积的变化比例
 * - MR2: 线段长度比例关系 - 检查线段长度变化时区域面积的变化比例
 * - MR3: 线段对称性关系 - 验证交换线段端点后形状的等价性
 * - MR4: 平移不变性关系 - 验证平移操作后形状特性的保持
 * - MR5: 正交特性关系 - 验证水平和垂直线段的特殊性质
 * - MR6: 旋转不变性关系 - 验证旋转操作后形状特性的保持
 */
public interface MetamorphicRelation {

    /**
     * 获取关系标识符
     *
     * @return 关系ID (例如, "MR1")
     */
    String getId();

    /**
     * 获取关系的可读描述
     *
     * @return 描述文本
     */
    String getDescription();

    /**
     * 根据源测试用例生成后续测试用例
     *
     * @param sourceTest 原始测试用例
     * @return 生成的后续测试用例列表
     */
    List<TestCase> generateFollowupTests(TestCase sourceTest);

    /**
     * 验证测试结果是否满足蜕变关系
     *
     * @param sourceTest        源测试用例
     * @param followupTest      后续测试用例
     * @param sourceResult      源测试结果
     * @param followupResult    后续测试结果
     * @param sourceExecution   源执行中的错误消息/类型 (若无错误则为空)
     * @param followupExecution 后续执行中的错误消息/类型 (若无错误则为空)
     * @return 如果关系满足则返回true
     */
    boolean verifyRelation(TestCase sourceTest, TestCase followupTest,
            Shape sourceResult, Shape followupResult,
            String sourceExecution, String followupExecution);

    /**
     * 判断此关系是否适用于给定的测试用例
     *
     * @param testCase 要检查的测试用例
     * @return 如果关系适用则返回true
     */
    boolean isApplicableTo(TestCase testCase);

    /**
     * 为给定的源测试用例创建蜕变组
     *
     * @param sourceTest 源测试用例
     * @return 蜕变组列表 (每个后续测试一个)
     */
    List<MetamorphicGroup> createGroups(TestCase sourceTest);


}