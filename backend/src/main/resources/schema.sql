SET NAMES utf8mb4;

DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS disinfection;
DROP TABLE IF EXISTS aid_loan;
DROP TABLE IF EXISTS medication_delegation;
DROP TABLE IF EXISTS teaching_aid;
DROP TABLE IF EXISTS classroom;

CREATE TABLE classroom (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  capacity INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_classroom_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medication_delegation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  child_name VARCHAR(32) NOT NULL,
  medicine_name VARCHAR(64) NOT NULL,
  dose VARCHAR(32) NOT NULL,
  parent_sign_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  executed_at DATETIME NULL,
  closed_at DATETIME NULL,
  close_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_medication_classroom_status (classroom_id, status),
  KEY idx_medication_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teaching_aid (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  kind VARCHAR(16) NOT NULL,
  classroom_id BIGINT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_aid_code (code),
  KEY idx_aid_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aid_loan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  aid_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  loan_date DATE NOT NULL,
  due_date DATE NOT NULL,
  return_date DATE NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_loan_aid (aid_id),
  KEY idx_loan_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disinfection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  aid_id BIGINT NOT NULL,
  disinfect_date DATE NOT NULL,
  method VARCHAR(16) NOT NULL,
  result VARCHAR(16) NOT NULL,
  operator VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_disinfection_aid_date (aid_id, disinfect_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE repair_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  aid_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  fault_desc VARCHAR(255) NULL,
  reporter VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  conclusion VARCHAR(16) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_repair_aid (aid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO classroom (code, name, capacity, status) VALUES
('C-01', '小一班', 25, '使用中'),
('C-02', '小二班', 25, '使用中'),
('C-03', '中一班', 30, '使用中'),
('C-04', '大二班', 30, '使用中'),
('C-05', '大三班', 28, '停用');

INSERT INTO medication_delegation
(classroom_id, child_name, medicine_name, dose, parent_sign_date, status, executed_at, closed_at, close_reason, created_at, updated_at) VALUES
(1, '朵朵', '小儿氨酚黄那敏颗粒', '1袋', '2026-09-18', '未执行', NULL, NULL, NULL, '2026-09-18 08:15:00', '2026-09-18 08:15:00');

INSERT INTO teaching_aid (code, name, kind, classroom_id, status) VALUES
('TA-1001', '大颗粒积木', '积木', 1, '可用'),
('TA-1002', '绘本《好饿的毛毛虫》', '绘本', 1, '可用'),
('TA-1003', '木质拼图', '拼图', 2, '可用'),
('TA-1004', '手摇铃', '乐器', NULL, '可用'),
('TA-1005', '小篮球', '运动', 3, '破损'),
('TA-1006', '平衡木', '运动', NULL, '维修中'),
('TA-1007', '彩色黏土', '手工', 4, '可用');

INSERT INTO aid_loan (aid_id, classroom_id, loan_date, due_date, return_date, status) VALUES
(1, 2, '2026-09-16', '2026-09-20', NULL, '在借'),
(3, 1, '2026-09-16', '2026-09-18', NULL, '在借'),
(4, 1, '2026-09-10', '2026-09-12', '2026-09-12', '已归还');

INSERT INTO disinfection (aid_id, disinfect_date, method, result, operator) VALUES
(1, '2026-09-16', '擦拭', '合格', '王老师'),
(3, '2026-09-16', '紫外线', '合格', '李老师'),
(2, '2026-09-15', '浸泡', '合格', '王老师');

INSERT INTO repair_order (aid_id, kind, fault_desc, reporter, status, conclusion, created_at, updated_at) VALUES
(6, '报修', '拉力绳有点起毛', '张老师', '维修中', NULL, '2026-09-16 09:20:00', '2026-09-16 09:20:00'),
(5, '点检', '边角有磨损', '王老师', '待处理', NULL, '2026-09-16 10:05:00', '2026-09-16 10:05:00');
