<template>
  <div class="wrap">
    <div class="head">
      <input v-model="keyword" class="search" placeholder="搜编号 / 名称" />
      <select v-model="statusFilter" class="mini">
        <option value="">全部状态</option>
        <option value="可用">可用</option>
        <option value="破损">破损</option>
        <option value="维修中">维修中</option>
      </select>
      <span class="count">共 {{ shown.length }} 件</span>
      <span class="newlink" @click="openCreate">＋ 登记新教具</span>
    </div>

    <table class="grid">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
          <th style="width:104px">编号</th>
          <th>名称</th>
          <th style="width:88px">类别</th>
          <th style="width:120px">归属</th>
          <th style="width:96px">状态</th>
          <th style="width:34px"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="a in shown"
          :key="a.id"
          :class="{ picked: picked.includes(a.id) }"
          @dblclick="openEdit(a)"
        >
          <td class="ck"><input type="checkbox" :value="a.id" v-model="picked" /></td>
          <td class="code">{{ a.code }}</td>
          <td class="name">{{ a.name }}</td>
          <td>{{ a.kind }}</td>
          <td class="dim">{{ nameOf(a.classroomId) }}</td>
          <td><span class="pill" :class="'p-' + statusKey(a.status)">{{ a.status }}</span></td>
          <td class="go" @click="openEdit(a)">›</td>
        </tr>
        <tr v-if="!shown.length">
          <td colspan="7" class="none">没有符合条件的教具</td>
        </tr>
      </tbody>
    </table>

    <div class="foot-hint">双击任意一行，从右边滑出编辑；勾选几行可以在下面批量改状态</div>

    <transition name="up">
      <div v-if="picked.length" class="batch">
        <span>已勾选 <b>{{ picked.length }}</b> 件</span>
        <span class="sep" />
        <span class="b" @click="batchStatus('可用')">标为可用</span>
        <span class="b" @click="batchStatus('破损')">标为破损</span>
        <span class="b" @click="batchStatus('维修中')">标为维修中</span>
        <span class="b ghost" @click="picked = []">取消勾选</span>
      </div>
    </transition>

    <el-drawer v-model="drawer" :title="form.id ? '编辑教具' : '登记新教具'" size="400px">
      <el-form label-width="86px">
        <el-form-item label="编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 TA-1008" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 彩虹伞" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.kind" style="width:100%">
            <el-option v-for="k in kinds" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属班级">
          <el-select v-model="form.classroomId" placeholder="公共区" clearable style="width:100%">
            <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="可用" value="可用" />
            <el-option label="破损" value="破损" />
            <el-option label="维修中" value="维修中" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { aidApi, classroomApi } from '../api'

const kinds = ['积木', '绘本', '拼图', '乐器', '运动', '手工']
const rows = ref([])
const classrooms = ref([])
const keyword = ref('')
const statusFilter = ref('')
const picked = ref([])
const drawer = ref(false)
const form = ref({})

const shown = computed(() => {
  const k = keyword.value.trim()
  return rows.value.filter(
    (a) =>
      (!k || (a.name || '').includes(k) || (a.code || '').includes(k)) &&
      (!statusFilter.value || a.status === statusFilter.value)
  )
})
const allChecked = computed(() => shown.value.length > 0 && picked.value.length === shown.value.length)

function statusKey(s) {
  if (s === '可用') return 'ok'
  if (s === '破损') return 'bad'
  return 'busy'
}

function nameOf(id) {
  const hit = classrooms.value.find((c) => c.id === id)
  return hit ? hit.name : '公共区'
}

async function load() {
  try {
    rows.value = await aidApi.list({})
    picked.value = []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadClassrooms() {
  try {
    classrooms.value = await classroomApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function toggleAll() {
  picked.value = allChecked.value ? [] : shown.value.map((a) => a.id)
}

function openEdit(row) {
  form.value = { ...row }
  drawer.value = true
}

function openCreate() {
  form.value = { kind: '积木', status: '可用' }
  drawer.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await aidApi.update(form.value.id, form.value)
    } else {
      await aidApi.create(form.value)
    }
    ElMessage.success('已保存')
    drawer.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function batchStatus(status) {
  const targets = rows.value.filter((a) => picked.value.includes(a.id))
  let ok = 0
  const failed = []
  for (const a of targets) {
    try {
      await aidApi.update(a.id, { status })
      ok += 1
    } catch (e) {
      failed.push(`${a.code}：${e.message}`)
    }
  }
  if (ok) ElMessage.success(`已改 ${ok} 件`)
  if (failed.length) ElMessage.warning(`有 ${failed.length} 件没改成 —— ${failed[0]}`)
  await load()
}

onMounted(async () => {
  await loadClassrooms()
  await load()
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
  width: 240px;
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
}
.grid tbody tr:hover {
  background: #fafcff;
}
.grid tbody tr.picked {
  background: #ecf5ff;
}
.grid td.code {
  font-family: monospace;
  color: #606266;
}
.grid td.name {
  font-weight: 500;
}
.grid td.dim {
  color: #909399;
}
.grid td.ck,
.grid th:first-child {
  text-align: center;
}
.grid td.go {
  color: #c0c4cc;
  cursor: pointer;
  text-align: center;
  font-size: 17px;
}
.grid td.none {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
}
.pill {
  display: inline-block;
  font-size: 12px;
  border-radius: 10px;
  padding: 1px 9px;
}
.p-ok {
  color: #529b2e;
  background: #f0f9eb;
}
.p-bad {
  color: #c45656;
  background: #fef0f0;
}
.p-busy {
  color: #b88230;
  background: #fdf6ec;
}
.foot-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #c0c4cc;
}
.batch {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 26px;
  background: #303133;
  color: #fff;
  border-radius: 24px;
  padding: 10px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.18);
  z-index: 20;
}
.batch b {
  color: #ffd04b;
}
.sep {
  width: 1px;
  height: 14px;
  background: rgba(255, 255, 255, 0.25);
}
.b {
  cursor: pointer;
  user-select: none;
}
.b:hover {
  color: #ffd04b;
}
.b.ghost {
  color: #a8abb2;
}
.up-enter-active,
.up-leave-active {
  transition: all 0.18s ease;
}
.up-enter-from,
.up-leave-to {
  opacity: 0;
  transform: translate(-50%, 12px);
}
</style>
