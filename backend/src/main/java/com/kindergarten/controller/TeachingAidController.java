package com.kindergarten.controller;

import com.kindergarten.entity.TeachingAid;
import com.kindergarten.service.TeachingAidService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TeachingAidController {

    private final TeachingAidService service;

    public TeachingAidController(TeachingAidService service) {
        this.service = service;
    }

    @GetMapping("/aids")
    public List<TeachingAid> list(@RequestParam(required = false) Long classroomId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String keyword) {
        return service.list(classroomId, status, keyword);
    }

    @PostMapping("/aids")
    public TeachingAid create(@RequestBody TeachingAid input) {
        return service.create(input);
    }

    @PutMapping("/aids/{id}")
    public TeachingAid update(@PathVariable Long id, @RequestBody TeachingAid input) {
        return service.update(id, input);
    }
}
