<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="消毒" name="disinfection">
      <div class="bar">
        <input type="date" v-model="dDate" />
        <select v-model="dMethod">
          <option value="擦拭">擦拭</option>
          <option value="浸泡">浸泡</option>
          <option value="紫外线">紫外线</option>
        </select>
        <select v-model="dResult">
          <option value="合格">合格</option>
          <option value="不合格">不合格</option>
        </select>
        <input v-model="dOperator" class="who" placeholder="操作人" />
        <button class="go" :disabled="!picked.length" @click="batchDisinfect">
          给勾选的 {{ picked.length }} 件登记消毒
        </button>
      </div>

      <el-row :gutter="16">
        <el-col :span="11">
          <div class="panel">
            <div class="panel-cap">选教具（可多选）</div>
            <table class="pick">
              <thead>
                <tr>
                  <th style="width:38px"><input type="checkbox" :checked="allPicked" @change="toggleAll" /></th>
                  <th>教具</th>
                  <th style="width:120px">上次消毒</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in aids" :key="a.id" :class="{ on: picked.includes(a.id) }">
                  <td class="ck"><input type="checkbox" :value="a.id" v-model="picked" /></td>
                  <td>
                    <span class="code">{{ a.code }}</span>
                    <span class="nm">{{ a.name }}</span>
                  </td>
                  <td class="last" :class="{ warn: !lastDate(a.id) }">
                    {{ lastDate(a.id) || '还没消过' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-col>

        <el-col :span="13">
          <div class="panel">
            <div class="panel-cap">消毒流水（最近 {{ disinfections.length }} 条）</div>
            <el-timeline v-if="disinfections.length" style="padding: 8px 4px 0 2px">
              <el-timeline-item
                v-for="d in disinfections"
                :key="d.id"
                :timestamp="d.disinfectDate"
                :type="d.result === '合格' ? 'success' : 'danger'"
              >
                <b>{{ aidName(d.aidId) }}</b>
                <span class="tag">{{ d.method }}</span>
                <span class="tag" :class="d.result === '合格' ? 'ok' : 'bad'">{{ d.result }}</span>
                <span class="by">{{ d.operator }}</span>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="还没有消毒记录" :image-size="70" />
          </div>
        </el-col>
      </el-row>
    </el-tab-pane>

    <el-tab-pane label="报修单" name="repair">
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="panel">
            <div class="panel-cap">
              报修单
              <span class="newlink" @click="openRepair">＋ 开单</span>
            </div>
            <ul class="repair-list">
              <li
                v-for="r in repairs"
                :key="r.id"
                :class="{ on: current && current.id === r.id }"
                @click="current = r"
              >
                <div class="r-line">
                  <span class="r-aid">{{ aidName(r.aidId) }}</span>
                  <span class="pill" :class="'st-' + r.status">{{ r.status }}</span>
                </div>
                <div class="r-sub">{{ r.kind }} · {{ r.reporter }}</div>
              </li>
              <li v-if="!repairs.length" class="empty">还没有报修单</li>
            </ul>
          </div>
        </el-col>

        <el-col :span="16">
          <div v-if="current" class="panel">
            <div class="panel-cap">{{ aidName(current.aidId) }} 的报修单</div>
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
              <span v-if="current.status === '已结案'" class="dim">这张单子已经结案了</span>
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
            <el-timeline style="margin-top:16px">
              <el-timeline-item :timestamp="fmt(current.createdAt)" type="primary">
                开单（{{ current.kind }}）
              </el-timeline-item>
              <el-timeline-item :timestamp="fmt(current.updatedAt)">
                最近一次流转 → {{ current.status }}
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else class="panel">
            <el-empty description="从左边选一张报修单" :image-size="80" />
          </div>
        </el-col>
      </el-row>

      <el-dialog v-model="repairVisible" title="开报修单" width="440px" append-to-body>
        <el-form label-width="86px">
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
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { careApi, aidApi } from '../api'

const route = useRoute()
const tab = ref(route.query.tab === 'repair' ? 'repair' : 'disinfection')

const aids = ref([])
const disinfections = ref([])
const repairs = ref([])
const current = ref(null)
const picked = ref([])

const today = new Date().toISOString().slice(0, 10)
const dDate = ref(today)
const dMethod = ref('擦拭')
const dResult = ref('合格')
const dOperator = ref('')

const repairVisible = ref(false)
const repairForm = ref({})

const allPicked = computed(() => aids.value.length > 0 && picked.value.length === aids.value.length)

function aidName(id) {
  const hit = aids.value.find((a) => a.id === id)
  return hit ? `${hit.code} ${hit.name}` : id
}

function lastDate(aidId) {
  const list = disinfections.value
    .filter((d) => d.aidId === aidId)
    .map((d) => d.disinfectDate)
    .sort()
  return list.length ? list[list.length - 1] : ''
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

function toggleAll() {
  picked.value = allPicked.value ? [] : aids.value.map((a) => a.id)
}

async function batchDisinfect() {
  const targets = aids.value.filter((a) => picked.value.includes(a.id))
  let ok = 0
  const failed = []
  for (const a of targets) {
    try {
      await careApi.disinfect({
        aidId: a.id,
        disinfectDate: dDate.value,
        method: dMethod.value,
        result: dResult.value,
        operator: dOperator.value
      })
      ok += 1
    } catch (e) {
      failed.push(`${a.code}：${e.message}`)
    }
  }
  if (ok) ElMessage.success(`已登记 ${ok} 件`)
  if (failed.length) ElMessage.warning(`${failed.length} 件没登成 —— ${failed[0]}`)
  picked.value = []
  await loadDisinfections()
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
.bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.bar input,
.bar select {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
  background: #fff;
}
.bar input:focus,
.bar select:focus {
  border-color: #409eff;
}
.bar .who {
  width: 130px;
}
.go {
  margin-left: auto;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 8px 22px;
  font-size: 13px;
  cursor: pointer;
}
.go:disabled {
  background: #c8d6e5;
  cursor: not-allowed;
}
.panel {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 14px 16px;
}
.panel-cap {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.newlink {
  margin-left: auto;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  user-select: none;
}
.pick {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.pick th {
  text-align: left;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  padding: 6px 8px;
  border-bottom: 1px solid #eee;
}
.pick td {
  padding: 7px 8px;
  border-bottom: 1px solid #f5f5f5;
}
.pick tr.on {
  background: #ecf5ff;
}
.pick td.ck,
.pick th:first-child {
  text-align: center;
}
.pick .code {
  font-family: monospace;
  color: #909399;
  margin-right: 6px;
}
.pick .nm {
  font-weight: 500;
}
.pick td.last {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}
.pick td.last.warn {
  color: #e6a23c;
  font-family: inherit;
}
.tag {
  display: inline-block;
  font-size: 12px;
  border-radius: 3px;
  padding: 0 7px;
  margin-left: 6px;
  background: #f4f4f5;
  color: #606266;
}
.tag.ok {
  background: #f0f9eb;
  color: #529b2e;
}
.tag.bad {
  background: #fef0f0;
  color: #c45656;
}
.by {
  font-size: 12px;
  color: #a8abb2;
  margin-left: 8px;
}
.repair-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 460px;
  overflow-y: auto;
}
.repair-list li {
  padding: 9px 10px;
  border-radius: 5px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
}
.repair-list li:hover {
  background: #f7fbff;
}
.repair-list li.on {
  background: #ecf5ff;
}
.repair-list li.empty {
  cursor: default;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 30px 0;
}
.r-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.r-aid {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.r-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}
.pill {
  font-size: 12px;
  border-radius: 9px;
  padding: 1px 8px;
  background: #f4f4f5;
  color: #606266;
  flex: none;
}
.st-待处理 {
  background: #fdf6ec;
  color: #b88230;
}
.st-维修中 {
  background: #ecf5ff;
  color: #337ecc;
}
.st-待复检 {
  background: #fdf6ec;
  color: #b88230;
}
.st-已结案 {
  background: #f0f9eb;
  color: #529b2e;
}
.step-actions {
  margin: 18px 0;
  text-align: center;
  min-height: 32px;
}
.dim {
  color: #909399;
  font-size: 12px;
}
</style>
