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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import java.util.regex.Pattern;

public class RenameMutants {

    private static final String BASE_DIR = "math1_test/mutants"; // Base directory containing the numbered
                                                                 // directories
    private static final String FILE_NAME = "convolve.java"; // The file to modify in each directory

    public static void main(String[] args) {
        try {
            // Get the absolute path to the base directory
            String baseDirName = BASE_DIR.replace("/", ".");
            String currentDir = System.getProperty("user.dir");
            currentDir = Paths.get(currentDir, "src/main/java/paper/pss/exp").toString();
            String baseDirPath = Paths.get(currentDir, BASE_DIR).toString();

            File baseDir = new File(baseDirPath);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                System.err.println("Base directory not found: " + baseDirPath);
                return;
            }

            // Get all numbered directories
            List<File> numberedDirs = new ArrayList<>();
            for (File dir : baseDir.listFiles()) {
                if (dir.isDirectory() && dir.getName().matches("\\d+")) {
                    numberedDirs.add(dir);
                }
            }

            System.out.println("Found " + numberedDirs.size() + " numbered directories.");

            // Process each directory
            for (File dir : numberedDirs) {
                String dirNumber = extractNumber(dir.getName());
                String newDirName = "mutant" + dirNumber;

                // Create path for the new directory
                Path newDirPath = Paths.get(dir.getParent(), newDirName);

                // Check if the target Java file exists
                File javaFile = new File(dir, FILE_NAME);
                if (!javaFile.exists()) {
                    System.out.println("Warning: " + FILE_NAME + " not found in directory " + dir.getName());
                    continue;
                }

                // Read the content of the Java file
                String content = readFileContent(javaFile);

                // Add package statement if it doesn't already have one
                if (!content.trim().startsWith("package ")) {
                    // String baseDirName = BASE_DIR.replace("\\", ".");
                    String packageStatement = "package paper.pss.exp." + baseDirName + "." + newDirName
                            + ";\n\n";
                    content = packageStatement + content;
                }

                // Create the new directory
                Files.createDirectories(newDirPath);

                // Write the modified content to the new file
                File newJavaFile = new File(newDirPath.toFile(), FILE_NAME);
                writeFileContent(newJavaFile, content);

                // Copy any other files from the old directory to the new one
                copyOtherFiles(dir, newDirPath.toFile(), FILE_NAME);

                System.out.println("Processed directory: " + dir.getName() + " -> " + newDirName);

                // Optionally, delete the old directory after successful migration
                // Comment this out if you want to keep the original directories
                deleteDirectory(dir);
            }

            System.out.println("Directory renaming and file modification completed successfully.");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String extractNumber(String dirName) {
        // 定义正则表达式，用于匹配一个或多个数字
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(dirName);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static void writeFileContent(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    private static void copyOtherFiles(File sourceDir, File targetDir, String excludeFileName) throws IOException {
        for (File file : sourceDir.listFiles()) {
            if (file.isFile() && !file.getName().equals(excludeFileName)) {
                Files.copy(file.toPath(), Paths.get(targetDir.getPath(), file.getName()));
            }
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}