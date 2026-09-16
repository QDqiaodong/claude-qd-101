<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
          <el-option label="使用中" value="使用中" />
          <el-option label="停用" value="停用" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="编号或名称" clearable style="width:200px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增班级</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="班级名称" min-width="160" />
        <el-table-column prop="capacity" label="可容纳人数" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '使用中' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑班级' : '新增班级'" width="460px">
      <el-form label-width="96px">
        <el-form-item label="编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 C-05" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="如 中二班" />
        </el-form-item>
        <el-form-item label="可容纳人数">
          <el-input-number v-model="form.capacity" :min="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="使用中" value="使用中" />
            <el-option label="停用" value="停用" />
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
import { classroomApi } from '../api'

const rows = ref([])
const filters = ref({ status: '', keyword: '' })
const visible = ref(false)
const form = ref({})

async function load() {
  try {
    rows.value = await classroomApi.list({ ...filters.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { capacity: 20, status: '使用中' }
  visible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  visible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await classroomApi.update(form.value.id, form.value)
    } else {
      await classroomApi.create(form.value)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
