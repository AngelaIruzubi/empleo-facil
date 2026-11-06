package es.empleofacil.lecturafacil.analysis;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Comprueba un texto frase a frase con las reglas de EasyReadRule.
// Es determinista: con el mismo texto siempre da el mismo resultado.
public final class RuleChecker {

    static final int MAX_WORDS_PER_SENTENCE = 15;

    // (?<!\p{L}) y (?!\p{L}) significan "sin una letra justo antes" y "sin una letra justo después":
    // así se busca una palabra entera, también con tildes.
    private static final Map<EasyReadRule, Pattern> PATTERNS = new EnumMap<>(EasyReadRule.class);

    static {
        PATTERNS.put(EasyReadRule.FORBIDDEN_SIGN,
                Pattern.compile("\\([^)]*\\)?|\\)|\\[[^\\]]*\\]?|\\]|[;&$#§]|\\.{3}|…"));
        PATTERNS.put(EasyReadRule.PERCENTAGE, Pattern.compile("(?:\\d+(?:[.,]\\d+)?\\s?)?%"));
        PATTERNS.put(EasyReadRule.ORDINAL, Pattern.compile("\\d+\\.?[ºª]"));
        PATTERNS.put(EasyReadRule.DATE_WITH_SLASHES, Pattern.compile("(?<!\\d)\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?(?!\\d)"));
        PATTERNS.put(EasyReadRule.MENTE_WORD,
                Pattern.compile("(?<!\\p{L})\\p{L}+mente(?!\\p{L})", CASE_INSENSITIVE | UNICODE_CASE));
        PATTERNS.put(EasyReadRule.ISIMO_WORD,
                Pattern.compile("(?<!\\p{L})\\p{L}+ísim[oa]s?(?!\\p{L})", CASE_INSENSITIVE | UNICODE_CASE));
        PATTERNS.put(EasyReadRule.ACRONYM, Pattern.compile("(?<!\\p{L})\\p{Lu}{2,}(?!\\p{L})"));
        PATTERNS.put(EasyReadRule.VAGUE_WORD,
                Pattern.compile("(?<!\\p{L})(?:cosas?|algo)(?!\\p{L})", CASE_INSENSITIVE | UNICODE_CASE));
        // Una barra que no está entre dos números: "y/o" o "C/ Mayor", pero no "13/10/2026"
        PATTERNS.put(EasyReadRule.SLASH, Pattern.compile("\\S*(?:(?<!\\d)/|/(?!\\d))\\S*"));
    }

    private RuleChecker() {
    }

    public static List<RuleViolation> check(String text) {
        List<RuleViolation> violations = new ArrayList<>();
        // Cada sigla solo se avisa la primera vez: hay que explicarla una vez, no siempre
        Set<String> seenAcronyms = new HashSet<>();

        for (String sentence : TextSplitter.sentences(text)) {
            int words = TextSplitter.words(sentence).size();
            if (words > MAX_WORDS_PER_SENTENCE) {
                violations.add(RuleViolation.of(EasyReadRule.LONG_SENTENCE, words + " palabras", sentence));
            }
            PATTERNS.forEach((rule, pattern) -> {
                Matcher match = pattern.matcher(sentence);
                while (match.find()) {
                    if (rule == EasyReadRule.ACRONYM && !seenAcronyms.add(match.group())) {
                        continue;
                    }
                    violations.add(RuleViolation.of(rule, match.group(), sentence));
                }
            });
        }
        return violations;
    }
}
