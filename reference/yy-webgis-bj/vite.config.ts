import vue from "@vitejs/plugin-vue"
import unocss from "unocss/vite"
import { defineConfig } from "vite"
import AutoImport from 'unplugin-auto-import/vite'
import postcsspxtoremv2 from 'postcss-pxtorem'
// import vueDevTools from 'vite-plugin-vue-devtools'


// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    unocss(),
    // https://github.com/antfu/unplugin-auto-import
    AutoImport({
      imports: [
        'vue',
        'vue-i18n',
        '@vueuse/head',
        '@vueuse/core',
        'pinia',
      ],
      dts: 'src/auto-imports.d.ts',
      dirs: [
        'src/composables',
        'src/stores',
        'src/hooks'
      ],
      vueTemplate: true,
      eslintrc: {
        enabled: true
      }
    }),
  ],
  resolve: {
    alias: {
      "@": "/src",
    },
  },
  server: {
    host: "0.0.0.0",
    port: 5555,
    proxy: {
      '/maps': {
        target: 'http://192.168.2.46:19998/',
        changeOrigin: true,
      },
      '/cdsz': {
        target: 'http://117.50.8.108:7088/cdsz',
        // target: 'http://117.50.8.108:7088/cdsz',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/cdsz/, '')
      },
      '/testapi': {
        target: 'http://192.168.2.81:8035/',
        // target: 'http://117.50.8.108:7088/cdsz',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/testapi/, '')
      }
    },
  },
  css: {
    postcss: {
      plugins: [
        // register vueDevTools before createHtmlPlugin
        // vueDevTools(),


        // postcsspxtoremv2({
        //   rootValue: 12, // 基准值
        //   propList: ['*'], // 需要转换的属性，这里表示全部都会转换
        //   selectorBlackList: [], // 不需要转换的选择器
        //   // exclude: /node_modules\/(?!tdesign)/, // 排除其他node_modules但包含tdesign
        // })


      ]
    }
  }
})
