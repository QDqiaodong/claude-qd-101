import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const classroomApi = {
  list: (params) => http.get('/classrooms', { params }),
  create: (data) => http.post('/classrooms', data),
  update: (id, data) => http.put(`/classrooms/${id}`, data)
}

export const aidApi = {
  list: (params) => http.get('/aids', { params }),
  create: (data) => http.post('/aids', data),
  update: (id, data) => http.put(`/aids/${id}`, data)
}

export const loanApi = {
  list: (params) => http.get('/loans', { params }),
  create: (data) => http.post('/loans', data),
  giveBack: (id, returnDate) =>
    http.post(`/loans/${id}/giveback`, null, { params: { returnDate } })
}

export const careApi = {
  listDisinfections: (params) => http.get('/disinfections', { params }),
  disinfect: (data) => http.post('/disinfections', data),
  listRepairs: () => http.get('/repairs'),
  openRepair: (data) => http.post('/repairs', data),
  advance: (id, action, conclusion) =>
    http.post(`/repairs/${id}/advance`, null, { params: { action, conclusion } })
}

export default http
