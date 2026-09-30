import request from './request'

export const uploadApi = {
  /**
   * 上传图片文件
   * @param {File} file - 要上传的文件
   * @param {string} subDir - 子目录（avatars, images, covers 等）
   * @returns {Promise} 返回 { url: "/assets/xxx/xxx.jpg" }
   */
  uploadImage: (file, subDir = 'images') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('subDir', subDir)
    return request.post('/v1/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
