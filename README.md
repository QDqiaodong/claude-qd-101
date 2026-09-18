# 幼儿园 · 班级与玩具教具管理系统

幼儿园日常管理的台账系统：**班级与活动室**、**玩具教具台账**、
**教具借用归还**、**消毒与报修**、**午间服药委托**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8211/ |
| 后端接口 | http://127.0.0.1:8311/api/ |
| MySQL | 127.0.0.1:3511（库 `kindergarten`） |
| Redis | 127.0.0.1:6511 |

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 业务模块

### 1. 班级与活动室（`classroom`）

班级编号 `C-xx` 全库唯一，状态为 `使用中 / 停用`，还有一个「可容纳人数」。
**停用班级之前要先把它名下登记的教具全都挪走或者处理掉**，否则不允许停用；
**班头上只要还有「未执行」的服药委托，停用同样会被顶回来**——班级仍是「使用中」，
委托原样保留，系统不会顺手清掉任何一张单子。
支持按状态、编号或名称关键字筛选。

- 页面：班级与活动室（`/classrooms`）
- 接口：`GET /api/classrooms`、`POST /api/classrooms`、`PUT /api/classrooms/{id}`

### 2. 玩具教具台账（`teaching_aid`）

教具编号 `TA-xxxx` 全库唯一，每件教具要么挂在某个班级，要么放在公共区（不填班级）。
类别分 `积木 / 绘本 / 拼图 / 乐器 / 运动 / 手工`；状态为 `可用 / 破损 / 维修中`。
**不能把教具归到已停用的班级**；**处于「维修中」的教具不能直接改回可用**，必须先走完报修单。

- 页面：玩具教具台账（`/aids`）
- 接口：`GET /api/aids`、`POST /api/aids`、`PUT /api/aids/{id}`

### 3. 教具借用归还（`aid_loan`）

一条借用 = 某个班级把一件教具借走一段时间。写入时校验：
应还日期必须晚于借出日期；停用的班级不能借；非「可用」状态的教具不能借；
**同一件教具的借用区间不能重叠**；归还后该时段即释放。

- 页面：教具借用归还（`/loans`）
- 接口：`GET /api/loans`、`POST /api/loans`、`POST /api/loans/{id}/giveback?returnDate=`

### 4. 消毒与报修（`disinfection` / `repair_order`）

- **消毒记录**：一件教具做一次消毒，记录日期、方式（擦拭 / 浸泡 / 紫外线）、结果与操作人。
  **同一件教具同一天只能登记一条**。
- **报修单**：状态机 `待处理 → 维修中 → 待复检 → 已结案`（复检不合格则退回 `维修中`）。
  类型分 `点检` 和 `报修`（报修直接进 `维修中`）。**一件教具同时只能有一张未结案的单子**；
  单子进 `维修中` 时教具跟着变成 `维修中`，验收合格后教具回到 `可用`。

- 页面：消毒与报修（`/care`）
- 接口：`GET /api/disinfections`、`POST /api/disinfections`、`GET /api/repairs`、
  `POST /api/repairs`、`POST /api/repairs/{id}/advance?action=&conclusion=`

### 5. 午间服药委托（`medication_order`）

家长签字委托班级中午给孩子喂一次药。一张委托写清：**挂在哪个班、孩子怎么称呼、
药品名称、这一次剂量、家长签字日**。**只能挂到「使用中」的班级**，停用的班级挂不上新委托。
委托只流转状态、永不删除：

- `未执行 → 已执行`：当班老师记下**实际喂药时刻**再点已执行；
- `未执行 → 已关闭`：孩子没来或拒服，**必须写下原因才能关单，空关不许过**；
- `未执行 → 已退回`：家长把委托退回。

**班级停用的第二道关**：班头上还有「未执行」委托时，停用必须失败，
班级身份继续是「使用中」，委托原文一张不少（不做点停用就自动作废）。
执行/关单/退回与停用并发抢同一个班时，两边在班级行锁上排队：
执行先落库，停用数到 0 张未执行才放行；停用先拿到锁，就失败并提示还有未执行委托——
失败的那一侧看到的始终是「班级还在用、委托还停在未执行」，不会出现半截账。

- 页面：午间服药委托（`/medication`）
- 接口：`GET /api/medications`、`POST /api/medications`、
  `POST /api/medications/{id}/execute?actualTime=`、`POST /api/medications/{id}/close?reason=`、
  `POST /api/medications/{id}/withdraw`

## 目录

```
backend/src/main/java/com/kindergarten/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       6 张业务表
├── repository/   Spring Data JPA（班级行 / 委托行带 SELECT ... FOR UPDATE 锁读）
└── service/      业务规则（唯一性、区间重叠、状态机、归属校验、停用双道关）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     5 个业务页面
```
