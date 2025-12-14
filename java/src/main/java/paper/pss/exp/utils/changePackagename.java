package paper.pss.exp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class changePackagename {

    private static final String BASE_PATH = "src/main/java/paper/pss/exp/math2_project/mutants";
    private static final String OLD_PACKAGE_PREFIX = "package paper.pss.exp.math2_test.mutants";
    private static final String NEW_PACKAGE_PREFIX = "package paper.pss.exp.math2_project.mutants";

    public static void main(String[] args) {
        Path startPath = Paths.get(BASE_PATH);

        try (Stream<Path> stream = Files.walk(startPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.toString().endsWith(".java"))
                  .forEach(changePackagename::processFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processFile(Path filePath) {
        File file = filePath.toFile();
        StringBuilder content = new StringBuilder();
        boolean changed = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                if (lineNumber == 0 && line.startsWith(OLD_PACKAGE_PREFIX)) {
                    String newPackageLine = line.replace(OLD_PACKAGE_PREFIX, NEW_PACKAGE_PREFIX);
                    content.append(newPackageLine).append(System.lineSeparator());
                    changed = true;
                } else {
                    content.append(line).append(System.lineSeparator());
                }
                lineNumber++;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            return;
        }

        if (changed) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content.toString());
                System.out.println("Successfully updated package in: " + filePath);
            } catch (IOException e) {
                System.err.println("Error writing to file: " + filePath + " - " + e.getMessage());
            }
        }
    }
}