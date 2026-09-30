import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

/**
 * 3D 渲染子树：必须交给 Vite 默认分块（由 ArchiveDetailView 的路由级
 * `await import('@google/model-viewer')` 单独成 chunk 并按需加载）。
 * 否则会被下面 manualChunks 末尾的 catch-all 塞进 vendor-other，
 * 而 vendor-other 被 index.html modulepreload —— 等于每个页面白下约 1 MB。
 */
const LAZY_3D = /node_modules\/(@google\/model-viewer|@monogrid\/gainmap-js|lit|lit-html|@lit|three)\//

export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          // <model-viewer> 是 Web Component，不是 Vue 组件；
          // 声明为自定义元素可消除 dev 下 "Failed to resolve component" 警告。
          isCustomElement: (tag) => tag === 'model-viewer'
        }
      }
    }),
    // Element Plus 按需自动引入
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    proxy: {
      // 前端请求 /api/* 自动转发到后端 8080 端口，解决跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 上传文件由后端 WebMvcConfig 映射到本地磁盘（zhiguan.assets.url-prefix = /assets）。
      // 生产环境 nginx.conf 同样把 /assets/ 反代到后端；开发环境此前漏了这一条，
      // 导致 <img src="/assets/..."> 落到 SPA 回退（拿到 index.html）而全部破图。
      '/assets': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  build: {
    // ⚠️ 不能沿用 Vite 默认的 assetsDir='assets'：
    //    后端把「用户上传文件」挂在 /assets/**（zhiguan.assets.url-prefix），
    //    生产 nginx 也把 /assets/ 反代到后端。若构建产物同样落在 /assets/，
    //    两者命名空间冲突 —— nginx 的 `^~ /assets/` 会把 index-*.js 也送去后端。
    //    因此构建产物改用 /static/，与上传文件彻底分开。
    assetsDir: 'static',
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          // 3D 子树放行给默认分块 → 路由级懒加载，不进 modulepreload
          if (LAZY_3D.test(id)) return
          if (id.includes('node_modules/vue') ||
              id.includes('node_modules/@vue') ||
              id.includes('node_modules/pinia') ||
              id.includes('node_modules/vue-router')) {
            return 'vendor-vue'
          }
          if (id.includes('node_modules/element-plus') ||
              id.includes('node_modules/@element-plus')) {
            return 'vendor-element'
          }
          if (id.includes('node_modules')) {
            return 'vendor-other'
          }
        }
      }
    }
  }
})
