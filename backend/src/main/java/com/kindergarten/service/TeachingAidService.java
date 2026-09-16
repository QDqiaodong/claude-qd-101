package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.Classroom;
import com.kindergarten.entity.TeachingAid;
import com.kindergarten.repository.ClassroomRepository;
import com.kindergarten.repository.TeachingAidRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingAidService {

    private final TeachingAidRepository aids;
    private final ClassroomRepository classrooms;

    public TeachingAidService(TeachingAidRepository aids, ClassroomRepository classrooms) {
        this.aids = aids;
        this.classrooms = classrooms;
    }

    public List<TeachingAid> list(Long classroomId, String status, String keyword) {
        return aids.findAllByOrderByIdAsc().stream()
                .filter(a -> classroomId == null || classroomId.equals(a.classroomId))
                .filter(a -> status == null || status.isEmpty() || status.equals(a.status))
                .filter(a -> keyword == null || keyword.isEmpty()
                        || a.name.contains(keyword) || a.code.contains(keyword))
                .toList();
    }

    @Transactional
    public TeachingAid create(TeachingAid input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("教具编号不能为空");
        }
        if (aids.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的教具用掉了");
        }
        if (input.classroomId != null) {
            Classroom room = classrooms.findById(input.classroomId)
                    .orElseThrow(() -> new BizException("要归的班级不存在"));
            if ("停用".equals(room.status)) {
                throw new BizException("班级 " + room.name + " 已经停用了，教具不能往那儿放");
            }
        }
        TeachingAid saved = new TeachingAid();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "积木" : input.kind;
        saved.classroomId = input.classroomId;
        saved.status = (input.status == null || input.status.isBlank()) ? "可用" : input.status;
        return aids.save(saved);
    }

    @Transactional
    public TeachingAid update(Long id, TeachingAid input) {
        TeachingAid a = aids.findById(id).orElseThrow(() -> new BizException("教具不存在"));
        if (input.name != null) {
            a.name = input.name;
        }
        if (input.kind != null && !input.kind.isBlank()) {
            a.kind = input.kind;
        }
        if (input.classroomId != null && !input.classroomId.equals(a.classroomId)) {
            Classroom room = classrooms.findById(input.classroomId)
                    .orElseThrow(() -> new BizException("要归的班级不存在"));
            if ("停用".equals(room.status)) {
                throw new BizException("班级 " + room.name + " 已经停用了，教具不能往那儿放");
            }
            a.classroomId = input.classroomId;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(a.status)) {
            if ("维修中".equals(a.status) && !"维修中".equals(input.status)) {
                throw new BizException("这件教具还有没结案的报修单，先走完才能改状态");
            }
            a.status = input.status;
        }
        return aids.save(a);
    }
}
