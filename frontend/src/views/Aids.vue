<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.classroomId" placeholder="全部班级" clearable style="width:170px">
          <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
          <el-option label="可用" value="可用" />
          <el-option label="破损" value="破损" />
          <el-option label="维修中" value="维修中" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="编号或名称" clearable style="width:200px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增教具</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="kind" label="类别" width="100" />
        <el-table-column label="归属班级" width="150">
          <template #default="{ row }">
            {{ nameOf(row.classroomId) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '可用' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑教具' : '新增教具'" width="480px">
      <el-form label-width="96px">
        <el-form-item label="编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 TA-1008" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 大颗粒积木" />
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
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { aidApi, classroomApi } from '../api'

const kinds = ['积木', '绘本', '拼图', '乐器', '运动', '手工']
const rows = ref([])
const classrooms = ref([])
const filters = ref({ classroomId: null, status: '', keyword: '' })
const visible = ref(false)
const form = ref({})

function nameOf(id) {
  const hit = classrooms.value.find((c) => c.id === id)
  return hit ? hit.name : '公共区'
}

async function load() {
  try {
    rows.value = await aidApi.list({ ...filters.value })
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

function openCreate() {
  form.value = { kind: '积木', status: '可用' }
  visible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  visible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await aidApi.update(form.value.id, form.value)
    } else {
      await aidApi.create(form.value)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadClassrooms()
  await load()
})
</script>
