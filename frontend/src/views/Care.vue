<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="消毒记录" name="disinfection">
        <el-card shadow="never">
          <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
            <el-select v-model="aidFilter" placeholder="全部教具" clearable style="width:220px">
              <el-option v-for="a in aids" :key="a.id" :label="`${a.code} ${a.name}`" :value="a.id" />
            </el-select>
            <el-button type="primary" @click="loadDisinfections">查询</el-button>
            <el-button @click="openDisinfect">登记消毒</el-button>
          </div>

          <el-table :data="disinfections" border stripe>
            <el-table-column label="教具" min-width="200">
              <template #default="{ row }">{{ aidName(row.aidId) }}</template>
            </el-table-column>
            <el-table-column prop="disinfectDate" label="消毒日期" width="130" />
            <el-table-column prop="method" label="方式" width="100" />
            <el-table-column prop="result" label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.result === '合格' ? 'success' : 'danger'">{{ row.result }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="报修单" name="repair">
        <el-card shadow="never">
          <div style="margin-bottom:14px">
            <el-button @click="openRepair">开一张报修单</el-button>
            <el-button @click="loadRepairs">刷新</el-button>
          </div>

          <el-table :data="repairs" border stripe>
            <el-table-column label="教具" min-width="180">
              <template #default="{ row }">{{ aidName(row.aidId) }}</template>
            </el-table-column>
            <el-table-column prop="kind" label="类型" width="90" />
            <el-table-column prop="faultDesc" label="问题描述" min-width="170" />
            <el-table-column prop="reporter" label="报修人" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '已结案' ? 'success' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="conclusion" label="结论" width="110" />
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === '待处理'"
                  link
                  type="primary"
                  @click="advance(row, 'start')"
                >
                  开工
                </el-button>
                <el-button
                  v-if="row.status === '维修中'"
                  link
                  type="primary"
                  @click="advance(row, 'finish')"
                >
                  送复检
                </el-button>
                <template v-if="row.status === '待复检'">
                  <el-button link type="success" @click="confirm(row, '合格')">验收合格</el-button>
                  <el-button link type="danger" @click="confirm(row, '不合格')">退回维修</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="disinfectVisible" title="登记消毒" width="460px">
      <el-form label-width="96px">
        <el-form-item label="教具">
          <el-select v-model="disinfectForm.aidId" style="width:100%">
            <el-option v-for="a in aids" :key="a.id" :label="`${a.code} ${a.name}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消毒日期">
          <el-date-picker
            v-model="disinfectForm.disinfectDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="方式">
          <el-select v-model="disinfectForm.method" style="width:100%">
            <el-option label="擦拭" value="擦拭" />
            <el-option label="浸泡" value="浸泡" />
            <el-option label="紫外线" value="紫外线" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="disinfectForm.result" style="width:100%">
            <el-option label="合格" value="合格" />
            <el-option label="不合格" value="不合格" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="disinfectForm.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disinfectVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDisinfect">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="repairVisible" title="开报修单" width="460px">
      <el-form label-width="96px">
        <el-form-item label="教具">
          <el-select v-model="repairForm.aidId" style="width:100%">
            <el-option v-for="a in aids" :key="a.id" :label="`${a.code} ${a.name}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="repairForm.kind" style="width:100%">
            <el-option label="点检" value="点检" />
            <el-option label="报修" value="报修" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="repairForm.faultDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="报修人">
          <el-input v-model="repairForm.reporter" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRepair">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { careApi, aidApi } from '../api'

const tab = ref('disinfection')
const aids = ref([])
const disinfections = ref([])
const repairs = ref([])
const aidFilter = ref(null)

const disinfectVisible = ref(false)
const disinfectForm = ref({})
const repairVisible = ref(false)
const repairForm = ref({})

function aidName(id) {
  const hit = aids.value.find((a) => a.id === id)
  return hit ? `${hit.code} ${hit.name}` : id
}

async function loadAids() {
  try {
    aids.value = await aidApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadDisinfections() {
  try {
    disinfections.value = await careApi.listDisinfections({ aidId: aidFilter.value })
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadRepairs() {
  try {
    repairs.value = await careApi.listRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openDisinfect() {
  disinfectForm.value = { method: '擦拭', result: '合格' }
  disinfectVisible.value = true
}

async function saveDisinfect() {
  try {
    await careApi.disinfect(disinfectForm.value)
    ElMessage.success('已登记')
    disinfectVisible.value = false
    await loadDisinfections()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openRepair() {
  repairForm.value = { kind: '点检' }
  repairVisible.value = true
}

async function saveRepair() {
  try {
    await careApi.openRepair(repairForm.value)
    ElMessage.success('已开单')
    repairVisible.value = false
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function advance(row, action) {
  try {
    await careApi.advance(row.id, action)
    ElMessage.success('已推进')
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function confirm(row, conclusion) {
  try {
    await careApi.advance(row.id, 'confirm', conclusion)
    ElMessage.success('已验收')
    await loadRepairs()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadAids()
  await loadDisinfections()
  await loadRepairs()
})
</script>
