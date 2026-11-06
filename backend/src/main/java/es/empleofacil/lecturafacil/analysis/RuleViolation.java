package es.empleofacil.lecturafacil.analysis;

// Un fallo encontrado: qué regla, qué trozo del texto, en qué frase y cómo arreglarlo.
public record RuleViolation(
        EasyReadRule rule,
        Severity severity,
        String fragment,
        String sentence,
        String advice) {

    static RuleViolation of(EasyReadRule rule, String fragment, String sentence) {
        return new RuleViolation(rule, rule.severity(), fragment, sentence, rule.advice());
    }
}
