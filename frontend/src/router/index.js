import { createRouter, createWebHistory } from 'vue-router'
import Classrooms from '../views/Classrooms.vue'
import Aids from '../views/Aids.vue'
import Loans from '../views/Loans.vue'
import Care from '../views/Care.vue'

const routes = [
  { path: '/', redirect: '/classrooms' },
  { path: '/classrooms', component: Classrooms, meta: { title: '班级与活动室' } },
  { path: '/aids', component: Aids, meta: { title: '玩具教具台账' } },
  { path: '/loans', component: Loans, meta: { title: '教具借用归还' } },
  { path: '/care', component: Care, meta: { title: '消毒与报修' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
