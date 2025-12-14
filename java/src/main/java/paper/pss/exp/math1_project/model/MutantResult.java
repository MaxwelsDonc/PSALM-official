package paper.pss.exp.math1_project.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MutantResult - 突变体测试结果类
 * 用于收集和管理单个突变体的测试结果
 */
public class MutantResult {
    private final String mutantId;
    private final Map<String, Boolean> mrResults; // MR ID -> 是否满足关系
    private final Map<String, String> descriptions; // MR ID -> 结果描述
    private int totalTests = 0;
    private int passedTests = 0;
    
    /**
     * 构造函数
     * @param mutantId 突变体ID
     */
    public MutantResult(String mutantId) {
        this.mutantId = mutantId;
        this.mrResults = new HashMap<>();
        this.descriptions = new HashMap<>();
    }
    
    /**
     * 添加测试结果
     * @param mrId 蜕变关系ID
     * @param satisfied 是否满足关系
     * @param description 结果描述
     */
    public void addResult(String mrId, boolean satisfied, String description) {
        mrResults.put(mrId, satisfied);
        descriptions.put(mrId, description);
        totalTests++;
        if (satisfied) {
            passedTests++;
        }
    }
    
    /**
     * 获取突变体ID
     */
    public String getMutantId() {
        return mutantId;
    }
    
    /**
     * 获取总体通过率
     */
    public double getOverallPassRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests * 100.0;
    }
    
    /**
     * 是否检测到缺陷
     */
    public boolean hasDetectedDefect() {
        return passedTests < totalTests;
    }
    
    /**
     * 获取所有测试结果
     */
    public Map<String, Boolean> getAllResults() {
        return new HashMap<>(mrResults);
    }
    
    /**
     * 获取特定MR的结果
     */
    public Boolean getResult(String mrId) {
        return mrResults.get(mrId);
    }
    
    /**
     * 获取失败的MR列表
     */
    public List<String> getFailedMRs() {
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : mrResults.entrySet()) {
            if (!entry.getValue()) {
                failed.add(entry.getKey());
            }
        }
        return failed;
    }
    
    /**
     * 获取通过的MR列表
     */
    public List<String> getPassedMRs() {
        List<String> passed = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : mrResults.entrySet()) {
            if (entry.getValue()) {
                passed.add(entry.getKey());
            }
        }
        return passed;
    }
    
    /**
     * 获取总测试数
     */
    public int getTotalTests() {
        return totalTests;
    }
    
    /**
     * 获取通过测试数
     */
    public int getPassedTests() {
        return passedTests;
    }
    
    /**
     * 获取失败测试数
     */
    public int getFailedTests() {
        return totalTests - passedTests;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Mutant: ").append(mutantId).append("\n");
        sb.append("Total Tests: ").append(totalTests).append("\n");
        sb.append("Passed: ").append(passedTests).append("\n");
        sb.append("Failed: ").append(getFailedTests()).append("\n");
        sb.append("Pass Rate: ").append(String.format("%.2f%%", getOverallPassRate())).append("\n");
        sb.append("Defect Detected: ").append(hasDetectedDefect() ? "Yes" : "No").append("\n");
        
        if (!getFailedMRs().isEmpty()) {
            sb.append("Failed MRs: ").append(getFailedMRs()).append("\n");
        }
        
        return sb.toString();
    }
}