package es.empleofacil.lecturafacil;

import java.util.List;

import es.empleofacil.lecturafacil.analysis.Readability;
import es.empleofacil.lecturafacil.analysis.ReadabilityCalculator;
import es.empleofacil.lecturafacil.analysis.RuleChecker;
import es.empleofacil.lecturafacil.analysis.RuleViolation;

// Respuesta de /api/easy-read. Junta lo que hace la IA (texto adaptado y cambios)
// con lo que calcula el código (legibilidad antes y después, y reglas que fallan).
public record EasyReadReport(
        String adaptedText,
        List<Change> changes,
        Readability readabilityBefore,
        Readability readabilityAfter,
        List<RuleViolation> violations) {

    static EasyReadReport from(String originalText, EasyReadResult result) {
        return new EasyReadReport(
                result.adaptedText(),
                result.changes(),
                ReadabilityCalculator.measure(originalText),
                ReadabilityCalculator.measure(result.adaptedText()),
                RuleChecker.check(result.adaptedText()));
    }
}
