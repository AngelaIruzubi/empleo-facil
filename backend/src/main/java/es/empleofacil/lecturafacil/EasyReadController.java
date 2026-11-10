package es.empleofacil.lecturafacil;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.empleofacil.pictogramas.ProfessionPictogram;
import es.empleofacil.pictogramas.ProfessionPictogramService;

@RestController
@RequestMapping("/api")
public class EasyReadController {

    private final EasyReadService easyReadService;
    private final ProfessionPictogramService professionPictogramService;

    public EasyReadController(EasyReadService easyReadService,
                              ProfessionPictogramService professionPictogramService) {
        this.easyReadService = easyReadService;
        this.professionPictogramService = professionPictogramService;
    }

    // Dos llamadas a la IA: primero adapta el texto y, si va bien, busca la profesión.
    // La profesión sale del texto original, que dice el nombre real del puesto.
    @PostMapping("/easy-read")
    public EasyReadReport adapt(@Valid @RequestBody EasyReadRequest request) {
        EasyReadResult result = easyReadService.adapt(request.text());
        ProfessionPictogram profession = professionPictogramService.find(request.text());
        return EasyReadReport.from(request.text(), result, profession);
    }
}
