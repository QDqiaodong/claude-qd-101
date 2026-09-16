<template>
  <div>
    <div class="toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜班级名或编号"
        clearable
        style="width:220px"
        @keyup.enter="load"
      />
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px" @change="load">
        <el-option label="使用中" value="使用中" />
        <el-option label="停用" value="停用" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <span style="flex:1" />
      <el-button type="primary" plain @click="openCreate">＋ 新增班级</el-button>
    </div>

    <el-empty v-if="!rows.length" description="还没有班级" />

    <el-row :gutter="16">
      <el-col v-for="row in rows" :key="row.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="room-card" shadow="hover" :class="{ off: row.status === '停用' }">
          <div class="room-head">
            <span class="room-name">{{ row.name }}</span>
            <el-tag size="small" :type="row.status === '使用中' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </div>
          <div class="room-code">{{ row.code }}</div>

          <div class="room-stats">
            <div class="stat">
              <b>{{ row.capacity }}</b>
              <span>可容纳</span>
            </div>
            <div class="stat">
              <b>{{ aidCount(row.id) }}</b>
              <span>名下教具</span>
            </div>
          </div>

          <div class="room-foot">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="visible" :title="form.id ? '编辑班级' : '新增班级'" width="440px">
      <el-form label-width="96px">
        <el-form-item label="编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 C-06" />
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
import { classroomApi, aidApi } from '../api'

const rows = ref([])
const aids = ref([])
const filters = ref({ status: '', keyword: '' })
const visible = ref(false)
const form = ref({})

function aidCount(classroomId) {
  return aids.value.filter((a) => a.classroomId === classroomId).length
}

async function load() {
  try {
    rows.value = await classroomApi.list({ ...filters.value })
    aids.value = await aidApi.list({})
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

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.room-card {
  margin-bottom: 16px;
}
.room-card.off {
  opacity: 0.62;
}
.room-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.room-name {
  font-size: 17px;
  font-weight: 600;
}
.room-code {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.room-stats {
  display: flex;
  gap: 26px;
  margin: 16px 0 8px;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat b {
  font-size: 20px;
  line-height: 1.2;
  color: var(--el-color-primary);
}
.stat span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.room-foot {
  text-align: right;
}
</style>
