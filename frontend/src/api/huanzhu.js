import request from './request'

export const huanzhuApi = {
  submit: (prompt) => request.post('/v1/tasks/huanzhu', { prompt }),
  getTaskStatus: (taskId) => request.get(`/v1/tasks/${taskId}/status`),
}
