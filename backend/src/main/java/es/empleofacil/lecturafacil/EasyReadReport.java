package es.empleofacil.lecturafacil;

import java.util.List;

import es.empleofacil.lecturafacil.analysis.Readability;
import es.empleofacil.lecturafacil.analysis.ReadabilityCalculator;
import es.empleofacil.lecturafacil.analysis.RuleChecker;
import es.empleofacil.lecturafacil.analysis.RuleViolation;
import es.empleofacil.pictogramas.Pictogram;
import es.empleofacil.pictogramas.ProfessionPictogram;

// Respuesta de /api/easy-read. Junta lo que hace la IA (texto adaptado, cambios y profesión)
// con lo que calcula el código (legibilidad antes y después, y reglas que fallan)
// y el pictograma de la profesión, si es una oferta y ARASAAC tiene uno.
public record EasyReadReport(
        String adaptedText,
        List<Change> changes,
        // Las profesiones que propuso la IA, para entender por qué hay o no hay pictograma
        List<String> professions,
        // Puede ser null: el texto no es una oferta o la profesión no tiene pictograma
        Pictogram pictogram,
        Readability readabilityBefore,
        Readability readabilityAfter,
        List<RuleViolation> violations) {

    static EasyReadReport from(String originalText, EasyReadResult result, ProfessionPictogram profession) {
        return new EasyReadReport(
                result.adaptedText(),
                result.changes(),
                profession.professions(),
                profession.pictogram(),
                ReadabilityCalculator.measure(originalText),
                ReadabilityCalculator.measure(result.adaptedText()),
                RuleChecker.check(result.adaptedText()));
    }
}
