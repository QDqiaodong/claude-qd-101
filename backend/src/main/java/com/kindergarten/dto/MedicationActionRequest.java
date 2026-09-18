package com.kindergarten.dto;

import java.time.LocalDateTime;

/** 服药委托动作：执行时必须带实际喂药时刻，未服关闭时必须带原因。 */
public record MedicationActionRequest(LocalDateTime executedAt, String reason) {
}
