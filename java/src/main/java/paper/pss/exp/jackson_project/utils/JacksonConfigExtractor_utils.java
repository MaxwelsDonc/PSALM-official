package paper.pss.exp.jackson_project.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JacksonConfigExtractor_utils {
    private final List<Partition> partitions = new ArrayList<>();
    private final Map<String, MR> mrs = new HashMap<>();

    public JacksonConfigExtractor_utils(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(configPath));

        JsonNode partitionsNode = root.get("partitions");
        if (partitionsNode == null || !partitionsNode.isArray()) {
            throw new IllegalArgumentException("Invalid or missing 'partitions' field");
        }

        for (JsonNode node : partitionsNode) {
            int id = node.get("id").asInt();
            int size = node.get("size").asInt();
            int weight = node.get("weight").asInt();
            partitions.add(new Partition(id, size, weight));
        }

        JsonNode mrsNode = root.get("mrs");
        if (mrsNode == null || !mrsNode.isObject()) {
            throw new IllegalArgumentException("Invalid or missing 'mrs' field");
        }

        Iterator<Map.Entry<String, JsonNode>> fields = mrsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String name = entry.getKey();
            String type = entry.getValue().get("type").asText();
            mrs.put(name, new MR(name, type));
        }
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    public Map<String, MR> getMrs() {
        return mrs;
    }

    public static class Partition {
        private final int id;
        private final int size;
        private final int weight;

        public Partition(int id, int size, int weight) {
            this.id = id;
            this.size = size;
            this.weight = weight;
        }

        public int getId() { return id; }
        public int getSize() { return size; }
        public int getWeight() { return weight; }
    }

    public static class MR {
        private final String name;
        private final String type;

        public MR(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public String getType() { return type; }
    }
}

