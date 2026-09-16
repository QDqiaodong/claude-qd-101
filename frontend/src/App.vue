<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">幼儿园 · 班级与玩具教具管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/classrooms">班级与活动室</el-menu-item>
        <el-menu-item index="/aids">玩具教具台账</el-menu-item>
        <el-menu-item index="/loans">教具借用归还</el-menu-item>
        <el-menu-item index="/care">消毒与报修</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
