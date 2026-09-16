<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="消毒记录" name="disinfection">
      <el-row :gutter="16">
        <el-col :span="9">
          <el-card shadow="never">
            <template #header>登记一次消毒</template>
            <el-form label-width="88px">
              <el-form-item label="教具">
                <el-select v-model="disinfectForm.aidId" style="width:100%" placeholder="选一件教具">
                  <el-option v-for="a in aids" :key="a.id" :label="`${a.code} ${a.name}`" :value="a.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="消毒日期">
                <el-date-picker
                  v-model="disinfectForm.disinfectDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width:100%"
                />
              </el-form-item>
              <el-form-item label="方式">
                <el-select v-model="disinfectForm.method" style="width:100%">
                  <el-option label="擦拭" value="擦拭" />
                  <el-option label="浸泡" value="浸泡" />
                  <el-option label="紫外线" value="紫外线" />
                </el-select>
              </el-form-item>
              <el-form-item label="结果">
                <el-select v-model="disinfectForm.result" style="width:100%">
                  <el-option label="合格" value="合格" />
                  <el-option label="不合格" value="不合格" />
                </el-select>
              </el-form-item>
              <el-form-item label="操作人">
                <el-input v-model="disinfectForm.operator" placeholder="如 王老师" />
              </el-form-item>
            </el-form>
            <div style="text-align:right">
              <el-button type="primary" @click="saveDisinfect">提交登记</el-button>
            </div>
          </el-card>
        </el-col>

        <el-col :span="15">
          <el-card shadow="never">
            <template #header>
              <span>消毒流水</span>
              <span class="muted">（最近在前，共 {{ disinfections.length }} 条）</span>
            </template>
            <el-timeline v-if="disinfections.length">
              <el-timeline-item
                v-for="d in disinfections"
                :key="d.id"
                :timestamp="d.disinfectDate"
                :type="d.result === '合格' ? 'success' : 'danger'"
                placement="top"
              >
                <b>{{ aidName(d.aidId) }}</b>
                <el-tag size="small" effect="plain" style="margin-left:8px">{{ d.method }}</el-tag>
                <el-tag
                  size="small"
                  style="margin-left:6px"
                  :type="d.result === '合格' ? 'success' : 'danger'"
                >
                  {{ d.result }}
                </el-tag>
                <span class="muted" style="margin-left:8px">{{ d.operator }}</span>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="还没有消毒记录" :image-size="80" />
          </el-card>
        </el-col>
      </el-row>
    </el-tab-pane>

    <el-tab-pane label="报修单" name="repair">
      <el-row :gutter="16">
        <el-col :span="9">
          <el-card shadow="never">
            <div class="repair-tools">
              <el-button type="primary" plain @click="openRepair">＋ 开一张报修单</el-button>
              <el-button @click="loadRepairs">刷新</el-button>
            </div>
            <el-dialog v-model="repairVisible" title="开报修单" width="460px" append-to-body>
              <el-form label-width="88px">
                <el-form-item label="教具">
                  <el-select v-model="repairForm.aidId" style="width:100%" placeholder="选一件教具">
                    <el-option v-for="a in aids" :key="a.id" :label="`${a.code} ${a.name}`" :value="a.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="repairForm.kind" style="width:100%">
                    <el-option label="点检" value="点检" />
                    <el-option label="报修" value="报修" />
                  </el-select>
                </el-form-item>
                <el-form-item label="问题描述">
                  <el-input v-model="repairForm.faultDesc" type="textarea" placeholder="哪里坏了、什么表现" />
                </el-form-item>
                <el-form-item label="报修人">
                  <el-input v-model="repairForm.reporter" placeholder="如 张老师" />
                </el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="repairVisible = false">取消</el-button>
                <el-button type="primary" @click="saveRepair">提交</el-button>
              </template>
            </el-dialog>
            <ul class="repair-list">
              <li
                v-for="r in repairs"
                :key="r.id"
                :class="{ active: current && current.id === r.id }"
                @click="current = r"
              >
                <div class="repair-line">
                  <span class="repair-aid">{{ aidName(r.aidId) }}</span>
                  <el-tag size="small" :type="statusType(r.status)">{{ r.status }}</el-tag>
                </div>
                <div class="repair-sub">{{ r.kind }} · {{ r.reporter }}</div>
              </li>
              <li v-if="!repairs.length" class="empty">还没有报修单</li>
            </ul>
          </el-card>
        </el-col>

        <el-col :span="15">
          <el-card v-if="current" shadow="never">
            <template #header>{{ aidName(current.aidId) }} 的报修单</template>

            <el-steps :active="stepOf(current.status)" finish-status="success" align-center>
              <el-step title="待处理" />
              <el-step title="维修中" />
              <el-step title="待复检" />
              <el-step title="已结案" />
            </el-steps>

            <div class="step-actions">
              <el-button v-if="current.status === '待处理'" type="primary" @click="advance('start')">
                开工维修
              </el-button>
              <el-button v-if="current.status === '维修中'" type="primary" @click="advance('finish')">
                送去复检
              </el-button>
              <template v-if="current.status === '待复检'">
                <el-button type="success" @click="confirm('合格')">复检合格</el-button>
                <el-button type="danger" plain @click="confirm('不合格')">退回维修</el-button>
              </template>
              <span v-if="current.status === '已结案'" class="muted">这张单子已经结案了</span>
            </div>

            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="类型">{{ current.kind }}</el-descriptions-item>
              <el-descriptions-item label="报修人">{{ current.reporter }}</el-descriptions-item>
              <el-descriptions-item label="问题描述" :span="2">
                {{ current.faultDesc || '（没写）' }}
              </el-descriptions-item>
              <el-descriptions-item label="复检结论" :span="2">
                {{ current.conclusion || '还没出结论' }}
              </el-descriptions-item>
            </el-descriptions>

            <el-timeline style="margin-top:18px">
              <el-timeline-item :timestamp="fmt(current.createdAt)" type="primary">
                开单（{{ current.kind }}）
              </el-timeline-item>
              <el-timeline-item :timestamp="fmt(current.updatedAt)" :type="statusType(current.status)">
                最近一次流转 → {{ current.status }}
              </el-timeline-item>
            </el-timeline>
          </el-card>
          <el-card v-else shadow="never">
            <el-empty description="从左边选一张报修单" :image-size="90" />
          </el-card>
        </el-col>
      </el-row>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { careApi, aidApi } from '../api'

const route = useRoute()
const tab = ref(route.query.tab === 'repair' ? 'repair' : 'disinfection')
const aids = ref([])
const disinfections = ref([])
const repairs = ref([])
const current = ref(null)

const disinfectForm = ref({ method: '擦拭', result: '合格' })
const repairVisible = ref(false)
const repairForm = ref({})

function aidName(id) {
  const hit = aids.value.find((a) => a.id === id)
  return hit ? `${hit.code} ${hit.name}` : id
}

function statusType(status) {
  if (status === '已结案') return 'success'
  if (status === '待复检') return 'warning'
  if (status === '维修中') return 'primary'
  return 'info'
}

function stepOf(status) {
  if (status === '待处理') return 0
  if (status === '维修中') return 1
  if (status === '待复检') return 2
  return 4
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function loadAids() {
  try {
    aids.value = await aidApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadDisinfections() {
  try {
    disinfections.value = await careApi.listDisinfections({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadRepairs() {
  try {
    repairs.value = await careApi.listRepairs()
    const keep = current.value && repairs.value.find((r) => r.id === current.value.id)
    current.value = keep || repairs.value[0] || null
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function saveDisinfect() {
  try {
    await careApi.disinfect(disinfectForm.value)
    ElMessage.success('已登记')
    disinfectForm.value = { method: '擦拭', result: '合格' }
    await loadDisinfections()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openRepair() {
  repairForm.value = { kind: '点检' }
  repairVisible.value = true
}

async function saveRepair() {
  try {
    await careApi.openRepair(repairForm.value)
    ElMessage.success('已开单')
    repairVisible.value = false
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function advance(action) {
  try {
    await careApi.advance(current.value.id, action)
    ElMessage.success('已推进')
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function confirm(conclusion) {
  try {
    await careApi.advance(current.value.id, 'confirm', conclusion)
    ElMessage.success('已复检')
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadAids()
  await loadDisinfections()
  await loadRepairs()
})
</script>

<style scoped>
.muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.repair-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.repair-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 520px;
  overflow-y: auto;
}
.repair-list li {
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.repair-list li:hover {
  background: var(--el-color-primary-light-9);
}
.repair-list li.active {
  background: var(--el-color-primary-light-8);
}
.repair-list li.empty {
  cursor: default;
  text-align: center;
  color: var(--el-text-color-secondary);
}
.repair-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.repair-aid {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.repair-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}
.step-actions {
  margin: 20px 0;
  text-align: center;
  min-height: 32px;
}
</style>
