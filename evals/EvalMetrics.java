import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import es.empleofacil.lecturafacil.analysis.EasyReadRule;
import es.empleofacil.lecturafacil.analysis.ReadabilityCalculator;
import es.empleofacil.lecturafacil.analysis.RuleChecker;
import es.empleofacil.lecturafacil.analysis.RuleViolation;
import es.empleofacil.lecturafacil.analysis.Severity;

// Mide los resultados guardados en evals/resultados con las reglas y los índices del backend.
// No llama a la IA: solo lee archivos.
//
// Uso, desde la carpeta raíz del proyecto (antes hay que compilar el backend con ./mvnw compile):
//   java -cp backend/target/classes evals/EvalMetrics.java
public class EvalMetrics {

    private static final Path TEXTS = Path.of("evals/textos");
    private static final Path RESULTS = Path.of("evals/resultados");
    private static final Path OUTPUT = Path.of("evals/metricas-2.6.md");

    public static void main(String[] args) throws IOException {
        StringBuilder md = new StringBuilder();
        md.append("# Métricas de la fase 2\n\n");
        md.append("Resultados de `evals/resultados` medidos con el código de la fase 2 ");
        md.append("(`RuleChecker` y `ReadabilityCalculator`). Lo genera `evals/EvalMetrics.java`.\n\n");
        md.append("INFLESZ: cuanto más alto, más fácil. Menos de 40 es muy difícil; de 65 a 80, bastante fácil.\n\n");

        List<String> summary = new ArrayList<>();
        for (Path modelDir : list(RESULTS)) {
            if (!Files.isDirectory(modelDir)) {
                continue;
            }
            summary.add(measureModel(modelDir, md));
        }

        md.append("## Resumen\n\n");
        md.append("| Modelo | INFLESZ medio antes | INFLESZ medio después | Errores | Avisos |\n");
        md.append("|---|---|---|---|---|\n");
        summary.forEach(md::append);

        md.append("\n## Conclusión\n\n");
        md.append("_Pendiente: la escribe Angela._\n");

        Files.writeString(OUTPUT, md.toString());
        System.out.print(md);
    }

    // Escribe la tabla de un modelo y devuelve su fila del resumen
    private static String measureModel(Path modelDir, StringBuilder md) throws IOException {
        String model = modelDir.getFileName().toString();
        md.append("## ").append(model).append("\n\n");
        md.append("| Texto | INFLESZ antes | INFLESZ después | Errores | Avisos | Reglas que fallan |\n");
        md.append("|---|---|---|---|---|---|\n");

        double sumBefore = 0;
        double sumAfter = 0;
        int errors = 0;
        int warnings = 0;
        List<Path> results = list(modelDir);
        for (Path result : results) {
            String name = result.getFileName().toString();
            double before = ReadabilityCalculator.measure(Files.readString(TEXTS.resolve(name))).inflesz().value();
            String adapted = adaptedText(Files.readString(result));
            double after = ReadabilityCalculator.measure(adapted).inflesz().value();
            List<RuleViolation> violations = RuleChecker.check(adapted);
            int textErrors = count(violations, Severity.ERROR);
            int textWarnings = count(violations, Severity.WARNING);

            md.append("| ").append(name.replace(".txt", ""))
                    .append(" | ").append(before)
                    .append(" | ").append(after)
                    .append(" | ").append(textErrors)
                    .append(" | ").append(textWarnings)
                    .append(" | ").append(rulesSummary(violations))
                    .append(" |\n");

            sumBefore += before;
            sumAfter += after;
            errors += textErrors;
            warnings += textWarnings;
        }
        md.append("\n");

        // Locale.ROOT: decimales con punto, como en las tablas, sea cual sea el idioma del ordenador
        return String.format(Locale.ROOT, "| %s | %.1f | %.1f | %d | %d |\n", model,
                sumBefore / results.size(), sumAfter / results.size(), errors, warnings);
    }

    // Quita la cabecera (líneas que empiezan por "# ") y la lista de cambios si la hay
    private static String adaptedText(String file) {
        String withoutChanges = file.split("\n## Cambios", 2)[0];
        return withoutChanges.lines()
                .filter(line -> !line.startsWith("# "))
                .reduce("", (text, line) -> text + line + "\n");
    }

    private static int count(List<RuleViolation> violations, Severity severity) {
        return (int) violations.stream().filter(v -> v.severity() == severity).count();
    }

    // Por ejemplo: "LONG_SENTENCE ×2, ACRONYM ×1"
    private static String rulesSummary(List<RuleViolation> violations) {
        Map<EasyReadRule, Integer> counts = new EnumMap<>(EasyReadRule.class);
        violations.forEach(v -> counts.merge(v.rule(), 1, Integer::sum));
        if (counts.isEmpty()) {
            return "—";
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((rule, n) -> parts.add(rule + " ×" + n));
        return String.join(", ", parts);
    }

    private static List<Path> list(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.sorted().toList();
        }
    }
}
