package es.empleofacil.lecturafacil;

import java.util.List;

// Los avisos de EasyReadService.adaptStreaming() mientras la IA escribe, en este orden:
// la profesión (una vez), trozos del texto adaptado (muchos), el texto completo (una vez).
// onChunk llega con cada trozo de la IA, también con los de la lista de cambios, que no se ven.
public interface EasyReadStreamListener {

    // Las profesiones tal como las ha escrito la IA, sin limpiar. Vacía si no es una oferta de trabajo.
    default void onProfessions(List<String> professions) {
    }

    // Un trozo nuevo del texto adaptado, ya sin escapes de JSON
    default void onText(String newText) {
    }

    // El texto adaptado está completo. La IA aún escribe la lista de cambios.
    default void onTextEnd() {
    }

    // Ha llegado un trozo de la IA, sea del campo que sea
    default void onChunk() {
    }
}
