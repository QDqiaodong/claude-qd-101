package com.kindergarten.controller;

import com.kindergarten.entity.Classroom;
import com.kindergarten.service.ClassroomService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ClassroomController {

    private final ClassroomService service;

    public ClassroomController(ClassroomService service) {
        this.service = service;
    }

    @GetMapping("/classrooms")
    public List<Classroom> list(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String keyword) {
        return service.list(status, keyword);
    }

    @PostMapping("/classrooms")
    public Classroom create(@RequestBody Classroom input) {
        return service.create(input);
    }

    @PutMapping("/classrooms/{id}")
    public Classroom update(@PathVariable Long id, @RequestBody Classroom input) {
        return service.update(id, input);
    }
}
