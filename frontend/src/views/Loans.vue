<template>
  <div>
    <div class="board-toolbar">
      <el-select v-model="filters.classroomId" placeholder="全部班级" clearable style="width:180px" @change="load">
        <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="load">刷新</el-button>
      <span style="flex:1" />
      <el-button type="primary" plain @click="openCreate">＋ 登记借用</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="col in columns" :key="col.status" :span="12">
        <div class="board-col">
          <div class="board-head" :class="col.cls">
            <span>{{ col.label }}</span>
            <b>{{ col.rows.length }}</b>
          </div>
          <div class="board-body">
            <div v-if="!col.rows.length" class="board-empty">这一列还没有记录</div>
            <div v-for="l in col.rows" :key="l.id" class="loan-card">
              <div class="loan-title">{{ aidName(l.aidId) }}</div>
              <div class="loan-meta">
                <el-tag size="small" effect="plain">{{ className(l.classroomId) }}</el-tag>
              </div>
              <div class="loan-dates">
                {{ l.loanDate }} → {{ l.dueDate }}
              </div>
              <div class="loan-foot">
                <span v-if="l.returnDate" class="returned">{{ l.returnDate }} 已归还</span>
                <el-button
                  v-if="l.status === '在借'"
                  size="small"
                  type="primary"
                  plain
                  @click="giveBack(l)"
                >
                  登记归还
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="visible" title="登记借用" width="460px">
      <el-form label-width="96px">
        <el-form-item label="教具">
          <el-select v-model="form.aidId" style="width:100%" placeholder="选一件可用教具">
            <el-option
              v-for="a in usableAids"
              :key="a.id"
              :label="`${a.code} ${a.name}`"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="借用班级">
          <el-select v-model="form.classroomId" style="width:100%" placeholder="选班级">
            <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="借出日期">
          <el-date-picker v-model="form.loanDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="应还日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { loanApi, aidApi, classroomApi } from '../api'

const rows = ref([])
const aids = ref([])
const classrooms = ref([])
const filters = ref({ classroomId: null })
const visible = ref(false)
const form = ref({})

const usableAids = computed(() => aids.value.filter((a) => a.status === '可用'))
const columns = computed(() => [
  { status: '在借', label: '在借', cls: 'col-busy', rows: rows.value.filter((r) => r.status === '在借') },
  { status: '已归还', label: '已归还', cls: 'col-done', rows: rows.value.filter((r) => r.status === '已归还') },
])

function aidName(id) {
  const hit = aids.value.find((a) => a.id === id)
  return hit ? `${hit.code} ${hit.name}` : id
}

function className(id) {
  const hit = classrooms.value.find((c) => c.id === id)
  return hit ? hit.name : id
}

async function load() {
  try {
    rows.value = await loanApi.list({ ...filters.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadRefs() {
  try {
    aids.value = await aidApi.list({})
    classrooms.value = await classroomApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = {}
  visible.value = true
}

async function save() {
  try {
    await loanApi.create(form.value)
    ElMessage.success('已登记')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function giveBack(row) {
  try {
    await loanApi.giveBack(row.id, new Date().toISOString().slice(0, 10))
    ElMessage.success('已归还')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadRefs()
  await load()
})
</script>

<style scoped>
.board-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.board-col {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}
.board-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-weight: 600;
  color: #fff;
}
.col-busy {
  background: #e6a23c;
}
.col-done {
  background: #67c23a;
}
.board-head b {
  font-size: 18px;
}
.board-body {
  padding: 12px;
  min-height: 200px;
  max-height: 560px;
  overflow-y: auto;
}
.board-empty {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 32px 0;
  font-size: 13px;
}
.loan-card {
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
}
.loan-title {
  font-weight: 600;
  margin-bottom: 6px;
}
.loan-meta {
  margin-bottom: 6px;
}
.loan-dates {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.loan-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.returned {
  font-size: 12px;
  color: #67c23a;
}
</style>
