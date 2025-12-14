package paper.pss.exp.jackson_project.utils;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.metamorphicRelations.MR1_relation;
import paper.pss.exp.jackson_project.metamorphicRelations.MR2_relation;
import paper.pss.exp.jackson_project.metamorphicRelations.MR3_relation;
import paper.pss.exp.jackson_project.metamorphicRelations.MR4_relation;
import paper.pss.exp.jackson_project.metamorphicRelations.MR5_relation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * Factory class for creating and managing metamorphic relations
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
}