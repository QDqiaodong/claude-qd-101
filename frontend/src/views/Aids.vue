<template>
  <el-row :gutter="16">
    <el-col :span="9">
      <el-card shadow="never" class="picker">
        <el-input v-model="filters.keyword" placeholder="搜编号或名称" clearable @input="load" />
        <div class="picker-row">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="flex:1" @change="load">
            <el-option label="可用" value="可用" />
            <el-option label="破损" value="破损" />
            <el-option label="维修中" value="维修中" />
          </el-select>
          <el-select v-model="filters.classroomId" placeholder="全部班级" clearable style="flex:1" @change="load">
            <el-option v-for="c in classrooms" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
        <el-button type="primary" plain class="picker-add" @click="startCreate">＋ 新增教具</el-button>

        <ul class="aid-list">
          <li
            v-for="a in rows"
            :key="a.id"
            :class="{ active: form.id === a.id }"
            @click="select(a)"
          >
            <span class="dot" :class="'dot-' + statusKey(a.status)" />
            <span class="aid-name">{{ a.name }}</span>
            <span class="aid-code">{{ a.code }}</span>
          </li>
          <li v-if="!rows.length" class="empty">没有符合条件的教具</li>
        </ul>
      </el-card>
    </el-col>

    <el-col :span="15">
      <el-card shadow="never" class="detail">
        <template #header>
          <span>{{ form.id ? '教具详情' : form.code ? '新增教具' : '详情' }}</span>
        </template>

        <el-empty
          v-if="!form.id && !form.code"
          description="从左边点一件教具查看，或点「新增教具」"
          :image-size="90"
        />

        <template v-else>
          <el-form label-width="96px">
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
          <div class="detail-foot">
            <el-button @click="reset">清空</el-button>
            <el-button type="primary" @click="save">保存</el-button>
          </div>
        </template>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { aidApi, classroomApi } from '../api'

const kinds = ['积木', '绘本', '拼图', '乐器', '运动', '手工']
const rows = ref([])
const classrooms = ref([])
const filters = ref({ classroomId: null, status: '', keyword: '' })
const form = ref({})

function statusKey(status) {
  if (status === '可用') return 'ok'
  if (status === '破损') return 'bad'
  return 'busy'
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

function select(row) {
  form.value = { ...row }
}

function startCreate() {
  form.value = { kind: '积木', status: '可用' }
}

function reset() {
  form.value = {}
}

async function save() {
  try {
    if (form.value.id) {
      await aidApi.update(form.value.id, form.value)
    } else {
      await aidApi.create(form.value)
    }
    ElMessage.success('已保存')
    form.value = {}
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

<style scoped>
.picker-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.picker-add {
  width: 100%;
  margin-top: 8px;
}
.aid-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  max-height: 520px;
  overflow-y: auto;
}
.aid-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 6px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.aid-list li:hover {
  background: var(--el-color-primary-light-9);
}
.aid-list li.active {
  background: var(--el-color-primary-light-8);
  font-weight: 600;
}
.aid-list li.empty {
  cursor: default;
  color: var(--el-text-color-secondary);
  justify-content: center;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}
.dot-ok {
  background: #67c23a;
}
.dot-bad {
  background: #f56c6c;
}
.dot-busy {
  background: #e6a23c;
}
.aid-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.aid-code {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-family: monospace;
}
.detail-foot {
  text-align: right;
  margin-top: 8px;
}
</style>
