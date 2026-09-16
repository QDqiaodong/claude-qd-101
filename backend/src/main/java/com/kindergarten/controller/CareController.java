package com.kindergarten.controller;

import com.kindergarten.entity.Disinfection;
import com.kindergarten.entity.RepairOrder;
import com.kindergarten.service.CareService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CareController {

    private final CareService service;

    public CareController(CareService service) {
        this.service = service;
    }

    @GetMapping("/disinfections")
    public List<Disinfection> listDisinfections(@RequestParam(required = false) Long aidId) {
        return service.listDisinfections(aidId);
    }

    @PostMapping("/disinfections")
    public Disinfection disinfect(@RequestBody Disinfection input) {
        return service.disinfect(input);
    }

    @GetMapping("/repairs")
    public List<RepairOrder> listRepairs() {
        return service.listRepairs();
    }

    @PostMapping("/repairs")
    public RepairOrder open(@RequestBody RepairOrder input) {
        return service.open(input);
    }

    @PostMapping("/repairs/{id}/advance")
    public RepairOrder advance(@PathVariable Long id,
                               @RequestParam String action,
                               @RequestParam(required = false) String conclusion) {
        return service.advance(id, action, conclusion);
    }
}
