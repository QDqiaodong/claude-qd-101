<template>
  <div class="sheet-wrap">
    <div class="sheet-cap">
      <span class="sheet-title">班级表</span>
      <span class="sheet-hint">格子里的内容直接改，点一下别处就存了；最后一行是新增行</span>
      <input v-model="keyword" class="sheet-search" placeholder="按班级名 / 编号过滤" />
    </div>

    <table class="sheet">
      <thead>
        <tr>
          <th style="width:110px">编号</th>
          <th>班级名</th>
          <th style="width:120px">可容纳</th>
          <th style="width:130px">状态</th>
          <th style="width:100px">名下教具</th>
          <th style="width:72px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in shown" :key="row.id" :class="{ off: row.status === '停用' }">
          <td class="code">{{ row.code }}</td>
          <td><input v-model="row.name" @blur="commit(row, 'name')" /></td>
          <td><input type="number" min="1" v-model.number="row.capacity" @blur="commit(row, 'capacity')" /></td>
          <td>
            <select v-model="row.status" @change="commit(row, 'status')">
              <option value="使用中">使用中</option>
              <option value="停用">停用</option>
            </select>
          </td>
          <td class="num">{{ aidCount(row.id) }}</td>
          <td>
            <span class="lnk" @click="toggle(row)">{{ row.status === '停用' ? '启用' : '停用' }}</span>
          </td>
        </tr>

        <tr class="draft">
          <td><input v-model="draft.code" placeholder="C-06" /></td>
          <td><input v-model="draft.name" placeholder="新班级名，填完按回车" @keyup.enter="append" /></td>
          <td><input type="number" min="1" v-model.number="draft.capacity" /></td>
          <td>
            <select v-model="draft.status">
              <option value="使用中">使用中</option>
              <option value="停用">停用</option>
            </select>
          </td>
          <td class="num">—</td>
          <td><span class="lnk add" @click="append">新增</span></td>
        </tr>
      </tbody>
    </table>

    <div v-if="savedTip" class="saved-tip">{{ savedTip }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { classroomApi, aidApi } from '../api'

const rows = ref([])
const aids = ref([])
const keyword = ref('')
const savedTip = ref('')
const draft = ref({ code: '', name: '', capacity: 20, status: '使用中' })

const shown = computed(() => {
  const k = keyword.value.trim()
  if (!k) return rows.value
  return rows.value.filter((r) => (r.name || '').includes(k) || (r.code || '').includes(k))
})

function aidCount(id) {
  return aids.value.filter((a) => a.classroomId === id).length
}

function flash(text) {
  savedTip.value = text
  setTimeout(() => (savedTip.value = ''), 1200)
}

async function load() {
  try {
    rows.value = await classroomApi.list({})
    aids.value = await aidApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function commit(row, field) {
  try {
    await classroomApi.update(row.id, { [field]: row[field] })
    flash('已自动保存')
    aids.value = await aidApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
    await load()
  }
}

function toggle(row) {
  row.status = row.status === '停用' ? '使用中' : '停用'
  commit(row, 'status')
}

async function append() {
  if (!draft.value.code || !draft.value.name) {
    ElMessage.warning('编号和班级名都要填')
    return
  }
  try {
    await classroomApi.create({ ...draft.value })
    flash('已新增')
    draft.value = { code: '', name: '', capacity: 20, status: '使用中' }
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.sheet-wrap {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 18px 22px 26px;
}
.sheet-cap {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 14px;
}
.sheet-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}
.sheet-hint {
  font-size: 12px;
  color: #999;
  flex: 1;
}
.sheet-search {
  width: 220px;
  border: none;
  border-bottom: 1px solid #ddd;
  padding: 4px 2px;
  font-size: 13px;
  outline: none;
}
.sheet-search:focus {
  border-bottom-color: #888;
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
  padding: 2px 8px;
  border-bottom: 1px solid #f0f0f0;
}
.sheet tr.off td {
  color: #bbb;
}
.sheet td.code {
  font-family: monospace;
  color: #666;
  padding-left: 8px;
}
.sheet td.num {
  text-align: right;
  padding-right: 18px;
  color: #666;
}
.sheet input,
.sheet select {
  width: 100%;
  border: 1px solid transparent;
  background: transparent;
  padding: 6px 6px;
  font-size: 13px;
  border-radius: 3px;
  outline: none;
  font-family: inherit;
}
.sheet input:hover,
.sheet select:hover {
  border-color: #e0e0e0;
}
.sheet input:focus,
.sheet select:focus {
  border-color: #888;
  background: #fff;
}
.sheet tr.draft td {
  border-bottom: none;
  padding-top: 8px;
}
.sheet tr.draft input::placeholder {
  color: #c8c8c8;
}
.lnk {
  color: #409eff;
  cursor: pointer;
  font-size: 12px;
  user-select: none;
}
.lnk:hover {
  text-decoration: underline;
}
.lnk.add {
  font-weight: 600;
}
.saved-tip {
  position: fixed;
  right: 26px;
  bottom: 26px;
  background: #f0f9eb;
  color: #529b2e;
  border: 1px solid #d1edc4;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
