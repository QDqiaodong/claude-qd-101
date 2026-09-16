<template>
  <div>
    <!-- 收银台：选完就借，没有弹窗 -->
    <div class="counter">
      <div class="counter-line">
        <select v-model="quick.aidId">
          <option :value="null">借哪件教具…</option>
          <option v-for="a in usableAids" :key="a.id" :value="a.id">{{ a.code }} {{ a.name }}</option>
        </select>
        <span class="arrow">→</span>
        <select v-model="quick.classroomId">
          <option :value="null">借给哪个班…</option>
          <option v-for="c in classrooms" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <input type="date" v-model="quick.loanDate" />
        <span class="arrow">→</span>
        <input type="date" v-model="quick.dueDate" />
        <button class="lend" :disabled="!canLend" @click="lend">借　出</button>
      </div>
      <div class="counter-hint">
        今天借 {{ countToday }} 件；当前有 <b>{{ overdue.length }}</b> 件逾期没还
      </div>
    </div>

    <div class="board">
      <section v-for="col in columns" :key="col.key" class="col">
        <header :class="col.cls">
          <span>{{ col.label }}</span>
          <b>{{ col.rows.length }}</b>
        </header>
        <div class="col-body">
          <div v-if="!col.rows.length" class="none">空</div>
          <article v-for="l in col.rows" :key="l.id" :class="{ late: col.key === 'late' }">
            <div class="aid">{{ aidName(l.aidId) }}</div>
            <div class="who">{{ className(l.classroomId) }}</div>
            <div class="when">
              {{ l.loanDate }} → {{ l.dueDate }}
              <span v-if="l.returnDate" class="ok">{{ l.returnDate }} 已还</span>
            </div>
            <span v-if="l.status === '在借'" class="act" @click="back(l)">还</span>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { loanApi, aidApi, classroomApi } from '../api'

const rows = ref([])
const aids = ref([])
const classrooms = ref([])
const today = new Date().toISOString().slice(0, 10)
const quick = ref({ aidId: null, classroomId: null, loanDate: today, dueDate: '' })

const usableAids = computed(() => aids.value.filter((a) => a.status === '可用'))
const canLend = computed(
  () => quick.value.aidId && quick.value.classroomId && quick.value.loanDate && quick.value.dueDate
)
const countToday = computed(() => rows.value.filter((r) => r.loanDate === today).length)

const borrowing = computed(() => rows.value.filter((r) => r.status === '在借'))
const overdue = computed(() => borrowing.value.filter((r) => (r.dueDate || '') < today))
const normal = computed(() => borrowing.value.filter((r) => (r.dueDate || '') >= today))

const columns = computed(() => [
  { key: 'normal', label: '在借', cls: 'h-busy', rows: normal.value },
  { key: 'late', label: '已逾期', cls: 'h-late', rows: overdue.value },
  { key: 'done', label: '已归还', cls: 'h-done', rows: rows.value.filter((r) => r.status === '已归还') },
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
    rows.value = await loanApi.list({})
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

async function lend() {
  try {
    await loanApi.create({ ...quick.value })
    ElMessage.success(`已借出：${aidName(quick.value.aidId)}`)
    quick.value = { aidId: null, classroomId: null, loanDate: today, dueDate: '' }
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function back(row) {
  try {
    await loanApi.giveBack(row.id, today)
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
.counter {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 14px 18px;
  margin-bottom: 18px;
}
.counter-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.counter select,
.counter input {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 7px 10px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
  background: #fff;
}
.counter select:focus,
.counter input:focus {
  border-color: #409eff;
}
.arrow {
  color: #c0c4cc;
}
.lend {
  margin-left: auto;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 8px 30px;
  font-size: 14px;
  letter-spacing: 2px;
  cursor: pointer;
}
.lend:disabled {
  background: #c8d6e5;
  cursor: not-allowed;
}
.counter-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
.counter-hint b {
  color: #f56c6c;
}
.board {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.col {
  flex: 1;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 6px;
  overflow: hidden;
}
.col header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 14px;
  font-size: 13px;
  color: #fff;
}
.h-busy {
  background: #409eff;
}
.h-late {
  background: #f56c6c;
}
.h-done {
  background: #b8c2cc;
}
.col-body {
  padding: 10px;
  min-height: 160px;
  max-height: 520px;
  overflow-y: auto;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 28px 0;
}
.col article {
  position: relative;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 5px;
  padding: 10px 12px;
  margin-bottom: 9px;
}
.col article.late {
  border-color: #fde2e2;
  background: #fffafa;
}
.aid {
  font-weight: 600;
  font-size: 13px;
}
.who {
  display: inline-block;
  margin: 5px 0;
  font-size: 12px;
  color: #606266;
  background: #f4f4f5;
  border-radius: 3px;
  padding: 1px 7px;
}
.when {
  font-size: 11px;
  color: #a8abb2;
  font-family: monospace;
}
.ok {
  color: #67c23a;
  margin-left: 6px;
}
.act {
  position: absolute;
  right: 10px;
  top: 10px;
  font-size: 12px;
  color: #409eff;
  border: 1px solid #b3d8ff;
  border-radius: 10px;
  padding: 0 9px;
  cursor: pointer;
  user-select: none;
}
.act:hover {
  background: #ecf5ff;
}
</style>
