package com.kindergarten.service;

import com.kindergarten.dto.BizException;
import com.kindergarten.entity.Disinfection;
import com.kindergarten.entity.RepairOrder;
import com.kindergarten.entity.TeachingAid;
import com.kindergarten.repository.DisinfectionRepository;
import com.kindergarten.repository.RepairOrderRepository;
import com.kindergarten.repository.TeachingAidRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareService {

    private final DisinfectionRepository disinfections;
    private final RepairOrderRepository repairs;
    private final TeachingAidRepository aids;

    public CareService(DisinfectionRepository disinfections, RepairOrderRepository repairs,
                       TeachingAidRepository aids) {
        this.disinfections = disinfections;
        this.repairs = repairs;
        this.aids = aids;
    }

    public List<Disinfection> listDisinfections(Long aidId) {
        return disinfections.findAllByOrderByIdDesc().stream()
                .filter(d -> aidId == null || aidId.equals(d.aidId))
                .toList();
    }

    @Transactional
    public Disinfection disinfect(Disinfection input) {
        if (input.aidId == null) {
            throw new BizException("请选择要消毒的教具");
        }
        if (input.disinfectDate == null) {
            throw new BizException("请填消毒日期");
        }
        TeachingAid aid = aids.findById(input.aidId)
                .orElseThrow(() -> new BizException("教具不存在"));
        if (!disinfections.findByAidIdAndDisinfectDate(aid.id, input.disinfectDate).isEmpty()) {
            throw new BizException("这件教具 " + input.disinfectDate + " 已经登过消毒了，别重复登");
        }
        Disinfection saved = new Disinfection();
        saved.aidId = aid.id;
        saved.disinfectDate = input.disinfectDate;
        saved.method = (input.method == null || input.method.isBlank()) ? "擦拭" : input.method;
        saved.result = (input.result == null || input.result.isBlank()) ? "合格" : input.result;
        saved.operator = input.operator;
        return disinfections.save(saved);
    }

    public List<RepairOrder> listRepairs() {
        return repairs.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional
    public RepairOrder open(RepairOrder input) {
        if (input.aidId == null) {
            throw new BizException("请选择要报修的教具");
        }
        TeachingAid aid = aids.findById(input.aidId)
                .orElseThrow(() -> new BizException("教具不存在"));
        if (!repairs.findByAidIdAndStatusNot(aid.id, "已结案").isEmpty()) {
            throw new BizException("这件教具还有没结案的报修单，先走完才能再开一张");
        }
        RepairOrder saved = new RepairOrder();
        saved.aidId = aid.id;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "点检" : input.kind;
        saved.faultDesc = input.faultDesc;
        saved.reporter = input.reporter;
        saved.status = "报修".equals(saved.kind) ? "维修中" : "待处理";
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        if ("维修中".equals(saved.status)) {
            aid.status = "维修中";
            aids.save(aid);
        }
        return repairs.save(saved);
    }

    @Transactional
    public RepairOrder advance(Long id, String action, String conclusion) {
        RepairOrder order = repairs.findById(id).orElseThrow(() -> new BizException("报修单不存在"));
        TeachingAid aid = aids.findById(order.aidId)
                .orElseThrow(() -> new BizException("教具不存在"));

        if ("start".equals(action)) {
            if (!"待处理".equals(order.status)) {
                throw new BizException("只有待处理的单子能开工，现在是 " + order.status);
            }
            order.status = "维修中";
            aid.status = "维修中";
        } else if ("finish".equals(action)) {
            if (!"维修中".equals(order.status)) {
                throw new BizException("只有维修中的单子能送去复检，现在是 " + order.status);
            }
            order.status = "待复检";
        } else if ("confirm".equals(action)) {
            if (!"待复检".equals(order.status)) {
                throw new BizException("只有待复检的单子能验收，现在是 " + order.status);
            }
            if ("合格".equals(conclusion)) {
                order.status = "已结案";
                order.conclusion = "合格";
                aid.status = "可用";
            } else {
                order.status = "维修中";
                order.conclusion = "退回维修";
                aid.status = "维修中";
            }
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        order.updatedAt = LocalDateTime.now();
        aids.save(aid);
        return repairs.save(order);
    }
}
