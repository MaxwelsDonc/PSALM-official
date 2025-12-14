package paper.pss.exp.math2_project.utils;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.metamorphicRelation.MR1_relation;
import paper.pss.exp.math2_project.metamorphicRelation.MR2_relation;
import paper.pss.exp.math2_project.metamorphicRelation.MR3_relation;
import paper.pss.exp.math2_project.metamorphicRelation.MR4_relation;
import paper.pss.exp.math2_project.metamorphicRelation.MR5_relation;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;

/**
 * Factory class for creating and managing metamorphic relations for copySign function
 */
public class MRFactory_utils {
    private static final List<MetamorphicRelation> ALL_RELATIONS = new ArrayList<>();

    static {
        // Initialize all metamorphic relations
        ALL_RELATIONS.add(new MR1_relation());
        ALL_RELATIONS.add(new MR2_relation());
        ALL_RELATIONS.add(new MR3_relation());
        ALL_RELATIONS.add(new MR4_relation());
        ALL_RELATIONS.add(new MR5_relation());
    }

    /**
     * Get all available metamorphic relations
     * @return List of all metamorphic relations
     */
    public static List<MetamorphicRelation> getAllRelations() {
        return new ArrayList<>(ALL_RELATIONS);
    }

    /**
     * Get a specific metamorphic relation by ID
     * @param id The ID of the relation (e.g., "MR1", "MR2", etc.)
     * @return The metamorphic relation with the given ID, or null if not found
     */
    public static MetamorphicRelation getRelationById(String id) {
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            if (relation.getId().equals(id)) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Get all applicable metamorphic relations for a given test case
     * @param testCase The test case to check
     * @return List of applicable metamorphic relations
     */
    public static List<MetamorphicRelation> getApplicableRelations(TestCase testCase) {
        List<MetamorphicRelation> applicableRelations = new ArrayList<>();
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            if (relation.isApplicableTo(testCase)) {
                applicableRelations.add(relation);
            }
        }
        return applicableRelations;
    }

    /**
     * Generate all metamorphic groups for a given test case
     * @param sourceTest The source test case
     * @return List of all metamorphic groups generated from applicable relations
     */
    public static List<MetamorphicGroup> generateAllGroups(TestCase sourceTest) {
        List<MetamorphicGroup> allGroups = new ArrayList<>();
        List<MetamorphicRelation> applicableRelations = getApplicableRelations(sourceTest);
        
        for (MetamorphicRelation relation : applicableRelations) {
            allGroups.addAll(relation.createGroups(sourceTest));
        }
        
        return allGroups;
    }

    /**
     * Get the count of available metamorphic relations
     * @return The number of available relations
     */
    public static int getRelationCount() {
        return ALL_RELATIONS.size();
    }

    /**
     * Get the IDs of all available metamorphic relations
     * @return List of relation IDs
     */
    public static List<String> getAllRelationIds() {
        List<String> ids = new ArrayList<>();
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            ids.add(relation.getId());
        }
        return ids;
    }

    /**
     * Test and debug main method for MRFactory_utils
     */
    public static void main(String[] args) {
        System.out.println("=== MRFactory_utils Test for Math2 Project ===");
        
        // Test getting all relations
        List<MetamorphicRelation> allRelations = getAllRelations();
        System.out.println("Total " + allRelations.size() + " metamorphic relations:");
        
        for (MetamorphicRelation relation : allRelations) {
            System.out.println("  " + relation.getId() + ": " + relation.getDescription());
        }
        
        // Test getting relation by ID
        System.out.println("\nGet relation by ID test:");
        String[] testIds = {"MR1", "MR3", "MR6", "INVALID"};
        
        for (String id : testIds) {
            MetamorphicRelation relation = getRelationById(id);
            if (relation != null) {
                System.out.println("  " + id + " -> " + relation.getDescription());
            } else {
                System.out.println("  " + id + " -> Not found");
            }
        }
        
        // Test getting relation count and ID list
        System.out.println("\nRelation count: " + getRelationCount());
        System.out.println("All relation IDs: " + getAllRelationIds());
        
        // Test applicability check
        System.out.println("\nApplicability test:");
        TestCase testCase = new TestCase(100L, 1L, 1);
        
        List<MetamorphicRelation> applicableRelations = getApplicableRelations(testCase);
        System.out.println("For test case (magnitude=100, sign=1):");
        System.out.println("  Number of applicable relations: " + applicableRelations.size());
        
        for (MetamorphicRelation relation : applicableRelations) {
            System.out.println("    " + relation.getId() + ": " + relation.getDescription());
        }
        
        // Test generating metamorphic groups
        List<MetamorphicGroup> allGroups = generateAllGroups(testCase);
        System.out.println("\nNumber of generated metamorphic groups: " + allGroups.size());
        
        for (MetamorphicGroup group : allGroups) {
            System.out.println("  Group ID: " + group.getMRId() + ", Description: " + group.getDescription());
        }
        
        System.out.println("\n=== MRFactory_utils Test Completed ===");
    }
}