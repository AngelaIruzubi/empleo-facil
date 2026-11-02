package es.empleofacil.lecturafacil;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EasyReadController.class)
class EasyReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EasyReadService easyReadService;

    @Test
    void adaptReturnsAdaptedText() throws Exception {
        when(easyReadService.adapt("Texto difícil")).thenReturn("Texto fácil");

        mockMvc.perform(post("/api/easy-read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Texto difícil"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adaptedText").value("Texto fácil"));
    }
}
