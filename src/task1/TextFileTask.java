package task1;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextFileTask {

    public static void main(String[] args) throws Exception {
        String rootDir = "D:/Downloads/textFiles";// Укажите здесь путь к вашей корневой директории
        List<Path> textFiles = collectTextFiles(rootDir);
        Map<Path, List<Path>> dependencies = extractDependencies(textFiles);

        // Проверка на наличие циклов и топологическая сортировка
        List<Path> sortedFiles = topologicalSort(dependencies.keySet());

        if (sortedFiles == null) {
            System.out.println("Ошибка: Найден цикл зависимостей между файлами.");
            return;
        }

        // Вывод списка вложенности файлов
        System.out.println("Список вложенности файлов:");
        sortedFiles.forEach(System.out::println);

        // Конкатенация содержимого файлов
        StringBuilder contentBuilder = new StringBuilder();
        for (Path file : sortedFiles) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                contentBuilder.append(reader.lines().collect(Collectors.joining(System.lineSeparator())));
            }
        }

        // Запись результата в файл
        Path outputFilePath = Paths.get("output.txt");
        Files.write(outputFilePath, Arrays.asList(contentBuilder.toString()));
    }

    private static List<Path> collectTextFiles(String rootDir) throws IOException {
        return Files.walk(Paths.get(rootDir))
                .filter(path -> path.toString().endsWith(".txt"))
                .collect(Collectors.toList());
    }

    private static Map<Path, List<Path>> extractDependencies(List<Path> files) throws IOException {
        Map<Path, List<Path>> dependencies = new HashMap<>();
        Pattern pattern = Pattern.compile("\\*require '(.*?)'");
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file);
            Matcher matcher = pattern.matcher(String.join("\n", lines));
            List<Path> requiredFiles = new ArrayList<>();
            while (matcher.find()) {
                requiredFiles.add(Paths.get(matcher.group(1)));
            }
            dependencies.put(file, requiredFiles);
        }
        return dependencies;
    }

    private static List<Path> topologicalSort(Set<Path> nodes) {
        Map<Path, List<Path>> graph = new HashMap<>();
        for (Path node : nodes) {
            graph.put(node, findDependentNodes(graph, node));
        }

        List<Path> result = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        Set<Path> visiting = new HashSet<>();

        for (Path node : nodes) {
            if (!visited.contains(node)) {
                if (dfs(graph, node, visited, visiting, result)) {
                    return null; // Цикл найден
                }
            }
        }

        Collections.reverse(result); // Обратный порядок для корректной топологической сортировки
        return result;
    }

    private static boolean dfs(Map<Path, List<Path>> graph, Path node, Set<Path> visited, Set<Path> visiting, List<Path> result) {
        if (visiting.contains(node)) {
            return true; // Цикл найден
        }

        if (visited.contains(node)) {
            return false;
        }

        visiting.add(node);
        for (Path dependentNode : graph.getOrDefault(node, Collections.emptyList())) {
            if (dfs(graph, dependentNode, visited, visiting, result)) {
                return true; // Цикл найден
            }
        }
        visiting.remove(node);
        visited.add(node);
        result.add(node);
        return false;
    }

    private static List<Path> findDependentNodes(Map<Path, List<Path>> graph, Path node) {
        List<Path> dependentNodes = new ArrayList<>();
        if (graph.containsKey(node)) {
            dependentNodes.addAll(graph.get(node));
        }
        return dependentNodes;
    }
}
