<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.classroomId" placeholder="全部班级" clearable style="width:170px">
          <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
          <el-option label="在借" value="在借" />
          <el-option label="已归还" value="已归还" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">登记借用</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column label="教具" min-width="180">
          <template #default="{ row }">{{ aidName(row.aidId) }}</template>
        </el-table-column>
        <el-table-column label="借用班级" width="140">
          <template #default="{ row }">{{ className(row.classroomId) }}</template>
        </el-table-column>
        <el-table-column prop="loanDate" label="借出日期" width="130" />
        <el-table-column prop="dueDate" label="应还日期" width="130" />
        <el-table-column prop="returnDate" label="归还日期" width="130" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '在借' ? 'warning' : 'success'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === '在借'" link type="primary" @click="giveBack(row)">
              归还
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="登记借用" width="480px">
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
const filters = ref({ classroomId: null, status: '' })
const visible = ref(false)
const form = ref({})

const usableAids = computed(() => aids.value.filter((a) => a.status === '可用'))

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
