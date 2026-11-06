package es.empleofacil.lecturafacil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.empleofacil.lecturafacil.analysis.EasyReadRule;
import es.empleofacil.lecturafacil.analysis.RuleViolation;

class EasyReadReportTest {

    private static final String ORIGINAL = """
            Se precisa incorporar personal de reposición para centro logístico. \
            Requisitos: disponibilidad para turnos rotativos e incorporación inmediata.""";

    private static final String ADAPTED = """
            Buscamos una persona para reponer productos.
            Trabajas en un almacén grande (centro logístico).""";

    @Test
    void measuresOriginalAndAdaptedTextAndChecksTheAdaptedOne() {
        List<Change> changes = List.of(new Change("reposición", "reponer productos", "Palabra difícil."));

        EasyReadReport report = EasyReadReport.from(ORIGINAL, new EasyReadResult(ADAPTED, changes));

        assertThat(report.adaptedText()).isEqualTo(ADAPTED);
        assertThat(report.changes()).isEqualTo(changes);
        assertThat(report.readabilityBefore().sentences()).isEqualTo(2);
        assertThat(report.readabilityAfter().sentences()).isEqualTo(2);
        // La versión adaptada es más fácil de leer
        assertThat(report.readabilityAfter().inflesz().value())
                .isGreaterThan(report.readabilityBefore().inflesz().value());
        // Las reglas se comprueban solo en el texto adaptado
        assertThat(report.violations()).extracting(RuleViolation::rule)
                .containsExactly(EasyReadRule.FORBIDDEN_SIGN);
    }
}
