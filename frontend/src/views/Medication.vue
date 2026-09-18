<template>
  <div class="wrap">
    <div class="head">
      <input v-model="keyword" class="search" placeholder="搜孩子称呼 / 药品名称" />
      <select v-model="classroomFilter" class="mini">
        <option value="">全部班级</option>
        <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <select v-model="statusFilter" class="mini">
        <option value="">全部状态</option>
        <option value="未执行">未执行</option>
        <option value="已执行">已执行</option>
        <option value="家长退回">家长退回</option>
        <option value="未服关闭">未服关闭</option>
      </select>
      <span class="count">共 {{ shown.length }} 张委托</span>
      <span class="newlink" @click="openCreate">＋ 新建午间服药委托</span>
    </div>

    <table class="grid">
      <thead>
        <tr>
          <th style="width:120px">班级</th>
          <th style="width:110px">孩子称呼</th>
          <th>药品名称</th>
          <th style="width:100px">本次剂量</th>
          <th style="width:120px">家长签字日</th>
          <th style="width:160px">执行 / 关闭时刻</th>
          <th style="width:96px">状态</th>
          <th>原因 / 备注</th>
          <th style="width:210px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in shown" :key="d.id" :class="{ pending: d.status === '未执行' }">
          <td>
            {{ classroomName(d.classroomId) }}
            <span v-if="classroomStatus(d.classroomId) === '停用'" class="room-off">停用</span>
          </td>
          <td class="child">{{ d.childName }}</td>
          <td class="medicine">{{ d.medicineName }}</td>
          <td>{{ d.dose }}</td>
          <td class="date">{{ d.parentSignDate }}</td>
          <td class="date">{{ actionTime(d) || '—' }}</td>
          <td><span class="pill" :class="statusClass(d.status)">{{ d.status }}</span></td>
          <td class="reason">{{ d.closeReason || '—' }}</td>
          <td class="ops">
            <template v-if="d.status === '未执行'">
              <span class="link" @click="openAction(d, 'execute')">记录已执行</span>
              <span class="link warn" @click="openAction(d, 'close')">未服关闭</span>
              <span class="link muted" @click="openAction(d, 'return')">家长退回</span>
            </template>
            <span v-else class="dim">委托原文已留档</span>
          </td>
        </tr>
        <tr v-if="!shown.length">
          <td colspan="9" class="none">没有符合条件的服药委托</td>
        </tr>
      </tbody>
    </table>

    <div class="foot-hint">
      停用班级不会自动作废委托；未执行委托必须先记录实际喂药时刻并执行，或由家长退回，班级才能停用。
    </div>

    <el-dialog v-model="createVisible" title="新建午间服药委托" width="480px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="挂到班级" required>
          <el-select v-model="form.classroomId" style="width:100%" placeholder="只能选择使用中的班级">
            <el-option
              v-for="c in activeClassrooms"
              :key="c.id"
              :label="`${c.name}（${c.code}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="孩子称呼" required>
          <el-input v-model="form.childName" placeholder="如 朵朵 / 李明" />
        </el-form-item>
        <el-form-item label="药品名称" required>
          <el-input v-model="form.medicineName" placeholder="按药袋 / 药瓶名称填写" />
        </el-form-item>
        <el-form-item label="这一次剂量" required>
          <el-input v-model="form.dose" placeholder="如 5ml、半片、1袋" />
        </el-form-item>
        <el-form-item label="家长签字日" required>
          <el-date-picker
            v-model="form.parentSignDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDelegation">提交委托</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="actionVisible"
      :title="actionTitle"
      width="470px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="118px">
        <el-form-item v-if="actionType === 'execute'" label="实际喂药时刻" required>
          <el-input v-model="actionForm.time" type="datetime-local" />
        </el-form-item>
        <el-form-item
          v-else
          :label="actionType === 'close' ? '未服原因' : '退回备注'"
          :required="actionType === 'close'"
        >
          <el-input
            v-model="actionForm.reason"
            type="textarea"
            :rows="3"
            :placeholder="actionType === 'close'
              ? '孩子没来或者拒服，必须写下原因，空原因不能关单'
              : '可填写家长退回说明，也可以留空'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button
          :type="actionType === 'close' ? 'danger' : 'primary'"
          :plain="actionType !== 'execute'"
          @click="submitAction"
        >
          {{ actionType === 'execute' ? '确认已执行' : actionType === 'close' ? '关上委托' : '确认家长退回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { classroomApi, medicationApi } from '../api'

const rows = ref([])
const classrooms = ref([])
const keyword = ref('')
const classroomFilter = ref('')
const statusFilter = ref('')
const createVisible = ref(false)
const actionVisible = ref(false)
const actionType = ref('execute')
const actionForm = ref({ id: null, time: '', reason: '' })

const today = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
  .toISOString()
  .slice(0, 10)
const form = ref(emptyForm())

const shown = computed(() => {
  const k = keyword.value.trim()
  return rows.value.filter((d) => {
    const matchKeyword = !k || d.childName.includes(k) || d.medicineName.includes(k)
    const matchClass = !classroomFilter.value || d.classroomId === classroomFilter.value
    const matchStatus = !statusFilter.value || d.status === statusFilter.value
    return matchKeyword && matchClass && matchStatus
  })
})

const activeClassrooms = computed(() => classrooms.value.filter((c) => c.status === '使用中'))

const actionTitle = computed(() => {
  if (actionType.value === 'execute') return '记录实际喂药时刻'
  if (actionType.value === 'close') return '未服关闭'
  return '家长退回委托'
})

function emptyForm() {
  return {
    classroomId: null,
    childName: '',
    medicineName: '',
    dose: '',
    parentSignDate: today
  }
}

function classroomName(id) {
  return classrooms.value.find((c) => c.id === id)?.name || `班级#${id}`
}

function classroomStatus(id) {
  return classrooms.value.find((c) => c.id === id)?.status || ''
}

function statusClass(status) {
  if (status === '已执行') return 'ok'
  if (status === '未执行') return 'pending'
  if (status === '家长退回') return 'returned'
  return 'closed'
}

function actionTime(d) {
  const value = d.executedAt || d.closedAt
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function nowLocalInput() {
  const d = new Date()
  d.setSeconds(0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadClassrooms() {
  try {
    classrooms.value = await classroomApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadDelegations() {
  try {
    rows.value = await medicationApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function refresh() {
  await Promise.all([loadDelegations(), loadClassrooms()])
}

function openCreate() {
  form.value = emptyForm()
  createVisible.value = true
}

async function saveDelegation() {
  if (!form.value.classroomId) return ElMessage.warning('请选择一个使用中的班级')
  if (!form.value.childName?.trim()) return ElMessage.warning('请写清孩子怎么称呼')
  if (!form.value.medicineName?.trim()) return ElMessage.warning('请写清药品名称')
  if (!form.value.dose?.trim()) return ElMessage.warning('请写清这一次剂量')
  if (!form.value.parentSignDate) return ElMessage.warning('请填写家长签字日期')

  try {
    await medicationApi.create({ ...form.value })
    ElMessage.success('委托已挂到班级')
    createVisible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e.message)
    await refresh()
  }
}

function openAction(row, type) {
  actionType.value = type
  actionForm.value = { id: row.id, time: nowLocalInput(), reason: '' }
  actionVisible.value = true
}

async function submitAction() {
  const id = actionForm.value.id
  try {
    if (actionType.value === 'execute') {
      if (!actionForm.value.time) return ElMessage.warning('请先记下实际喂药时刻')
      await medicationApi.execute(id, { executedAt: actionForm.value.time })
    } else if (actionType.value === 'close') {
      if (!actionForm.value.reason.trim()) return ElMessage.warning('孩子没来或者拒服，必须写下原因')
      await medicationApi.close(id, { reason: actionForm.value.reason.trim() })
    } else {
      await medicationApi.returnByParent(id, { reason: actionForm.value.reason.trim() })
    }
    ElMessage.success('委托已更新')
    actionVisible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e.message)
    actionVisible.value = false
    setTimeout(refresh, 300)
  }
}

onMounted(async () => {
  await Promise.all([loadClassrooms(), loadDelegations()])
})
</script>

<style scoped>
.wrap {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 16px 20px 70px;
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.search {
  width: 220px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
}
.search:focus {
  border-color: #409eff;
}
.mini {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 7px 8px;
  font-size: 13px;
  outline: none;
  background: #fff;
}
.count {
  font-size: 12px;
  color: #909399;
}
.newlink {
  margin-left: auto;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  user-select: none;
}
.newlink:hover {
  text-decoration: underline;
}
.grid {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.grid th {
  text-align: left;
  font-weight: 500;
  color: #909399;
  font-size: 12px;
  padding: 8px 10px;
  background: #fafafa;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
}
.grid td {
  padding: 9px 10px;
  border-bottom: 1px solid #f2f2f2;
  vertical-align: top;
}
.grid tbody tr.pending {
  background: #fffdf5;
}
.grid tbody tr.pending td {
  border-bottom-color: #faecd8;
}
.child {
  font-weight: 600;
}
.medicine {
  color: #303133;
}
.date {
  font-family: monospace;
  color: #606266;
  white-space: nowrap;
}
.room-off {
  display: inline-block;
  margin-left: 6px;
  font-size: 11px;
  color: #909399;
  background: #f4f4f5;
  border-radius: 8px;
  padding: 0 6px;
}
.reason {
  color: #606266;
  line-height: 1.4;
}
.none {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
}
.pill {
  display: inline-block;
  font-size: 12px;
  border-radius: 10px;
  padding: 2px 9px;
  white-space: nowrap;
}
.pill.ok {
  color: #529b2e;
  background: #f0f9eb;
}
.pill.pending {
  color: #b88230;
  background: #fdf6ec;
}
.pill.returned {
  color: #909399;
  background: #f4f4f5;
}
.pill.closed {
  color: #c45656;
  background: #fef0f0;
}
.ops {
  white-space: nowrap;
}
.link {
  color: #409eff;
  cursor: pointer;
  user-select: none;
  margin-right: 10px;
  font-size: 12px;
}
.link:hover {
  text-decoration: underline;
}
.link.warn {
  color: #e6a23c;
}
.link.muted {
  color: #909399;
}
.dim {
  color: #a8abb2;
  font-size: 12px;
}
.foot-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
