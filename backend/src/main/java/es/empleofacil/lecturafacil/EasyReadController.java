package es.empleofacil.lecturafacil;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EasyReadController {

    private final EasyReadService easyReadService;

    public EasyReadController(EasyReadService easyReadService) {
        this.easyReadService = easyReadService;
    }

    @PostMapping("/easy-read")
    public EasyReadReport adapt(@Valid @RequestBody EasyReadRequest request) {
        EasyReadResult result = easyReadService.adapt(request.text());
        return EasyReadReport.from(request.text(), result);
    }
}
