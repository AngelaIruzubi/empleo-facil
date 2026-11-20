package es.empleofacil.pictogramas;

import java.util.List;

// Las profesiones que propone la IA y el pictograma de la primera que tiene uno en ARASAAC.
// pictogram es null si el texto no es una oferta o ninguna profesión tiene pictograma.
public record ProfessionPictogram(List<String> professions, Pictogram pictogram) {

    public static final ProfessionPictogram NONE = new ProfessionPictogram(List.of(), null);
}
