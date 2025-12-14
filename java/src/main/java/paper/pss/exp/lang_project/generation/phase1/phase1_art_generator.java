package paper.pss.exp.lang_project.generation.phase1;

import java.util.*;
import paper.pss.exp.lang_project.model.TestCase;

public class phase1_art_generator {
    private static final Random random = new Random();
    private final int candidateNum;
    
    public phase1_art_generator() {
        this.candidateNum = 10;
    }
    
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        TestCase firstTestCase = generateRandomTestCase();
        testCases.add(firstTestCase);
        
        while (testCases.size() < count) {
            TestCase bestCandidate = null;
            double bestMinDist = -1;
            
            for (int i = 0; i < candidateNum; i++) {
                TestCase candidate = generateRandomTestCase();
                
                double minDist = Double.MAX_VALUE;
                for (TestCase existing : testCases) {
                    double dist = distance(candidate, existing);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                
                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    bestCandidate = candidate;
                }
            }
            
            testCases.add(bestCandidate);
        }
        
        return testCases;
    }
    
    private double distance(TestCase a, TestCase b) {
        Date date1A = a.getDate1();
        Date date2A = a.getDate2();
        Date date1B = b.getDate1();
        Date date2B = b.getDate2();
        
        if (date1A == null || date2A == null || date1B == null || date2B == null) {
            boolean nullA = (date1A == null || date2A == null);
            boolean nullB = (date1B == null || date2B == null);
            return nullA == nullB ? 0.0 : 1.0;
        }
        
        double dist1 = Math.abs(date1A.getTime() - date1B.getTime());
        double dist2 = Math.abs(date2A.getTime() - date2B.getTime());
        
        double daysDist1 = dist1 / (24.0 * 60.0 * 60.0 * 1000.0);
        double daysDist2 = dist2 / (24.0 * 60.0 * 60.0 * 1000.0);
        
        return (daysDist1 + daysDist2) / 2.0;
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
    
    public static void main(String[] args) {
        phase1_art_generator generator = new phase1_art_generator();
        List<TestCase> testCases = generator.generate(5);
        
        for (int i = 0; i < testCases.size(); i++) {
            System.out.println("ART Test #" + (i + 1) + ": " + testCases.get(i));
        }
    }
}