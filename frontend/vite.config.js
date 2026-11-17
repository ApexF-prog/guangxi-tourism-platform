import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      { find: '@screen', replacement: fileURLToPath(new URL('./src/screen', import.meta.url)) },
      { find: '@', replacement: fileURLToPath(new URL('./src', import.meta.url)) }
    ]
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    target: 'es2018',
    cssCodeSplit: true,
    assetsInlineLimit: 4096,
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        // 公共库按更新频率与体积拆开：echarts 只有进大屏/收入报表时才下载
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('zrender') || id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
          if (id.includes('/vue/') || id.includes('@vue/') || id.includes('vue-router')) return 'vendor-vue'
          return 'vendor'
        }
      }
    }
  }
})
