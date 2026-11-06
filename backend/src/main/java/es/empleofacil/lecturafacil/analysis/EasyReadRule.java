package es.empleofacil.lecturafacil.analysis;

// Las reglas de lectura fácil que se pueden comprobar con código.
// Salen de docs/lectura-facil.md. Cada una lleva un consejo para la persona que revisa el texto.
public enum EasyReadRule {

    LONG_SENTENCE(Severity.ERROR,
            "La frase tiene más de 15 palabras. Divídela en frases más cortas."),
    FORBIDDEN_SIGN(Severity.ERROR,
            "Evita el punto y coma, los puntos suspensivos, los paréntesis, los corchetes y los signos & $ # §."),
    PERCENTAGE(Severity.ERROR,
            "Evita los porcentajes. Usa \"muchos\", \"algunos\" o \"pocos\"."),
    ORDINAL(Severity.ERROR,
            "No uses números ordinales. Escribe \"la entrevista número 2\"."),
    DATE_WITH_SLASHES(Severity.ERROR,
            "Escribe la fecha completa, por ejemplo \"martes 13 de octubre de 2026\"."),
    MENTE_WORD(Severity.WARNING,
            "Evita las palabras en -mente: \"rápidamente\" pasa a \"rápido\"."),
    ISIMO_WORD(Severity.WARNING,
            "Evita las palabras que exageran: \"importantísimo\" pasa a \"muy importante\"."),
    ACRONYM(Severity.WARNING,
            "Es una sigla. Explícala la primera vez que aparece."),
    VAGUE_WORD(Severity.WARNING,
            "Evita \"cosa\" y \"algo\": no tienen un significado claro."),
    SLASH(Severity.WARNING,
            "Evita la barra /. Escribe las palabras completas.");

    private final Severity severity;
    private final String advice;

    EasyReadRule(Severity severity, String advice) {
        this.severity = severity;
        this.advice = advice;
    }

    public Severity severity() {
        return severity;
    }

    public String advice() {
        return advice;
    }
}
