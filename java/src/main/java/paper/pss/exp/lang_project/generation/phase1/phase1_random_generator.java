package paper.pss.exp.lang_project.generation.phase1;

import java.util.*;
import paper.pss.exp.lang_project.model.TestCase;

public class phase1_random_generator {
    private static final Random random = new Random();
    
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TestCase testCase = generateRandomTestCase();
            testCases.add(testCase);
        }
        
        return testCases;
    }
    
    private TestCase generateRandomTestCase() {
        if (random.nextDouble() < 0.1) {
            return generateNullTestCase();
        }
        
        Date date1 = generateRandomDate();
        Date date2 = generateRandomDate();
        int partitionId = determinePartitionId(date1, date2);
        
        return new TestCase(date1, date2, partitionId);
    }
    
    private TestCase generateNullTestCase() {
        int nullCase = random.nextInt(3);
        if (nullCase == 0) {
            return new TestCase((Date) null, (Date) null, 1);
        } else if (nullCase == 1) {
            return new TestCase(null, generateRandomDate(), 1);
        } else {
            return new TestCase(generateRandomDate(), null, 1);
        }
    }
    
    private Date generateRandomDate() {
        Calendar cal = Calendar.getInstance();
        int year = 2000 + random.nextInt(50);
        int month = random.nextInt(12);
        int day = 1 + random.nextInt(28);
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        
        cal.set(year, month, day, hour, minute);
        return cal.getTime();
    }
    
    private int determinePartitionId(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return 1;
        }
        
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        
        int year1 = cal1.get(Calendar.YEAR);
        int month1 = cal1.get(Calendar.MONTH);
        int day1 = cal1.get(Calendar.DAY_OF_MONTH);
        
        int year2 = cal2.get(Calendar.YEAR);
        int month2 = cal2.get(Calendar.MONTH);
        int day2 = cal2.get(Calendar.DAY_OF_MONTH);
        
        boolean sameYear = (year1 == year2);
        boolean sameMonth = (month1 == month2);
        boolean sameDay = (day1 == day2);
        
        if (sameYear && sameMonth && sameDay) {
            return 2;
        } else if (sameYear && sameMonth && !sameDay) {
            return 3;
        } else if (sameYear && !sameMonth && sameDay) {
            return 4;
        } else if (!sameYear && sameMonth && sameDay) {
            return 5;
        } else if (sameYear && !sameMonth && !sameDay) {
            return 6;
        } else if (!sameYear && sameMonth && !sameDay) {
            return 7;
        } else if (!sameYear && !sameMonth && sameDay) {
            return 8;
        } else {
            return 9;
        }
    }
    
    public Map<Integer, Integer> getStatistics(List<TestCase> testCases) {
        Map<Integer, Integer> stats = new HashMap<>();
        
        for (TestCase testCase : testCases) {
            int partitionId = testCase.getPartitionId();
            stats.put(partitionId, stats.getOrDefault(partitionId, 0) + 1);
        }
        
        return stats;
    }
    
    public static void main(String[] args) {
        phase1_random_generator generator = new phase1_random_generator();
        List<TestCase> testCases = generator.generate(10);
        
        for (TestCase testCase : testCases) {
            System.out.println(testCase);
        }
        
        Map<Integer, Integer> stats = generator.getStatistics(testCases);
        System.out.println("\nPartition Statistics:");
        for (Map.Entry<Integer, Integer> entry : stats.entrySet()) {
            System.out.println("Partition " + entry.getKey() + ": " + entry.getValue() + " test cases");
        }
    }
}