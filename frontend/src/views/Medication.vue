<template>
  <div>
    <div class="bar">
      <select v-model="form.classroomId">
        <option :value="null" disabled>选班级（只有使用中的能挂）</option>
        <option v-for="c in activeClassrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input v-model="form.childName" class="w-kid" placeholder="孩子怎么称呼" />
      <input v-model="form.medicineName" class="w-med" placeholder="药品名称" />
      <input v-model="form.dose" class="w-dose" placeholder="这一次剂量，如 5ml" />
      <input type="date" v-model="form.parentSignDate" title="家长签字日" />
      <button class="go" @click="create">挂上委托</button>
    </div>

    <div class="panel">
      <div class="panel-cap">
        委托台账
        <span class="hint">单子只流转不删除；还有「未执行」的班，停用会被顶回来</span>
        <select v-model="filterClassroom" class="flt">
          <option :value="null">全部班级</option>
          <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <select v-model="filterStatus" class="flt">
          <option value="">全部状态</option>
          <option value="未执行">未执行</option>
          <option value="已执行">已执行</option>
          <option value="已关闭">已关闭</option>
          <option value="已退回">已退回</option>
        </select>
      </div>

      <table class="sheet">
        <thead>
          <tr>
            <th style="width:110px">班级</th>
            <th style="width:90px">孩子</th>
            <th>药品</th>
            <th style="width:150px">本次剂量</th>
            <th style="width:110px">家长签字日</th>
            <th style="width:86px">状态</th>
            <th style="width:140px">实际喂药时刻</th>
            <th>说明</th>
            <th style="width:170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in shown" :key="o.id" :class="{ done: o.status !== '未执行' }">
            <td>{{ classroomName(o.classroomId) }}</td>
            <td class="kid">{{ o.childName }}</td>
            <td>{{ o.medicineName }}</td>
            <td>{{ o.dose }}</td>
            <td class="dt">{{ o.parentSignDate }}</td>
            <td><span class="pill" :class="'st-' + o.status">{{ o.status }}</span></td>
            <td class="dt">{{ fmt(o.actualTime) || '—' }}</td>
            <td class="note">{{ o.closeReason || '' }}</td>
            <td>
              <template v-if="o.status === '未执行'">
                <span class="lnk ok" @click="openExecute(o)">记已执行</span>
                <span class="lnk" @click="openClose(o)">关单</span>
                <span class="lnk warn" @click="withdraw(o)">退回</span>
              </template>
              <span v-else class="dim">已了结</span>
            </td>
          </tr>
          <tr v-if="!shown.length">
            <td colspan="9" class="empty">没有符合条件的委托</td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="execVisible" title="记下实际喂药时刻" width="420px" append-to-body>
      <div v-if="current" class="dlg-line">
        {{ classroomName(current.classroomId) }} · {{ current.childName }} ·
        {{ current.medicineName }}（{{ current.dose }}）
      </div>
      <input type="datetime-local" v-model="execTime" class="dt-input" />
      <template #footer>
        <el-button @click="execVisible = false">取消</el-button>
        <el-button type="primary" @click="doExecute">确认已执行</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="关单要写清原因" width="420px" append-to-body>
      <div v-if="current" class="dlg-line">
        {{ classroomName(current.classroomId) }} · {{ current.childName }} ·
        {{ current.medicineName }}（{{ current.dose }}）
      </div>
      <div class="quick">
        <span class="lnk" @click="closeReason = '孩子今天没来'">孩子没来</span>
        <span class="lnk" @click="closeReason = '孩子拒服'">孩子拒服</span>
      </div>
      <el-input
        v-model="closeReason"
        type="textarea"
        :rows="3"
        placeholder="孩子没来或拒服都要写下原因，空着关不掉"
      />
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" @click="doClose">写下原因并关单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { medicationApi, classroomApi } from '../api'

const classrooms = ref([])
const orders = ref([])
const filterClassroom = ref(null)
const filterStatus = ref('')

const today = new Date().toISOString().slice(0, 10)
const blank = { classroomId: null, childName: '', medicineName: '', dose: '', parentSignDate: today }
const form = ref({ ...blank })

const current = ref(null)
const execVisible = ref(false)
const execTime = ref('')
const closeVisible = ref(false)
const closeReason = ref('')

const activeClassrooms = computed(() => classrooms.value.filter((c) => c.status === '使用中'))

const shown = computed(() =>
  orders.value.filter(
    (o) =>
      (filterClassroom.value == null || o.classroomId === filterClassroom.value) &&
      (!filterStatus.value || o.status === filterStatus.value)
  )
)

function classroomName(id) {
  const hit = classrooms.value.find((c) => c.id === id)
  return hit ? hit.name : id
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

function nowLocal() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load() {
  try {
    classrooms.value = await classroomApi.list({})
    orders.value = await medicationApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function create() {
  try {
    await medicationApi.create(form.value)
    ElMessage.success('委托已挂上')
    form.value = { ...blank }
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

function openExecute(o) {
  current.value = o
  execTime.value = nowLocal()
  execVisible.value = true
}

async function doExecute() {
  try {
    await medicationApi.execute(current.value.id, execTime.value || undefined)
    ElMessage.success('已记下喂药时刻，单子执行完毕')
    execVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    execVisible.value = false
    await load()
  }
}

function openClose(o) {
  current.value = o
  closeReason.value = ''
  closeVisible.value = true
}

async function doClose() {
  if (!closeReason.value.trim()) {
    ElMessage.warning('先写下原因，空着关不掉')
    return
  }
  try {
    await medicationApi.close(current.value.id, closeReason.value.trim())
    ElMessage.success('单子已关闭')
    closeVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    closeVisible.value = false
    await load()
  }
}

async function withdraw(o) {
  try {
    await ElMessageBox.confirm(
      `确认家长把「${o.childName} · ${o.medicineName}」这张委托退回？退回后单子了结，原文保留。`,
      '家长退回委托',
      { confirmButtonText: '确认退回', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await medicationApi.withdraw(o.id)
    ElMessage.success('委托已退回')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

onMounted(load)
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
.bar .w-kid {
  width: 120px;
}
.bar .w-med {
  width: 180px;
}
.bar .w-dose {
  width: 160px;
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
  gap: 10px;
}
.panel-cap .hint {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  flex: 1;
}
.flt {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 5px 8px;
  font-size: 12px;
  background: #fff;
  font-family: inherit;
}
.sheet {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.sheet th {
  text-align: left;
  font-weight: 500;
  color: #888;
  padding: 6px 8px;
  border-bottom: 2px solid #333;
  font-size: 12px;
}
.sheet td {
  padding: 7px 8px;
  border-bottom: 1px solid #f0f0f0;
}
.sheet tr.done td {
  color: #a8abb2;
}
.sheet td.kid {
  font-weight: 600;
}
.sheet td.dt {
  font-family: monospace;
  font-size: 12px;
  color: #666;
}
.sheet td.note {
  font-size: 12px;
  color: #909399;
}
.sheet td.empty {
  text-align: center;
  color: #c0c4cc;
  padding: 26px 0;
}
.pill {
  font-size: 12px;
  border-radius: 9px;
  padding: 1px 8px;
  background: #f4f4f5;
  color: #606266;
  white-space: nowrap;
}
.st-未执行 {
  background: #fdf6ec;
  color: #b88230;
}
.st-已执行 {
  background: #f0f9eb;
  color: #529b2e;
}
.st-已关闭 {
  background: #f4f4f5;
  color: #909399;
}
.st-已退回 {
  background: #ecf5ff;
  color: #337ecc;
}
.lnk {
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
  user-select: none;
  margin-right: 10px;
  white-space: nowrap;
}
.lnk:hover {
  text-decoration: underline;
}
.lnk.ok {
  color: #529b2e;
}
.lnk.warn {
  color: #b88230;
}
.dim {
  color: #c0c4cc;
  font-size: 12px;
}
.dlg-line {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
}
.dt-input {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 13px;
  font-family: inherit;
  box-sizing: border-box;
}
.quick {
  margin-bottom: 8px;
}
</style>
