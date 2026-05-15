import Layout from "@/layouts/index.vue"
import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from 'vue-router'
import { useUserStoreWithOut } from '@/stores/modules/user'

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: () => import("@/pages/login/index.vue"),
  },
  {
    path: "/",
    component: Layout,
    redirect: "/zndz",
    children: [
      {
        path: "zndz",
        name: "zndz",
        component: () => import("@/pages/panel/index.vue"),
        meta: {
          // showPanels: {
          //   leftPanel: true,
          //   rightPanel: true,
          //   timeline: true,
          //   mapTools: true,
          // },
          permission: 'read'
        }
      },

      // <t-menu - item value = "/all" > 预测产品展示 < /t-menu-item>
      // < t - menu - item value = "/compare" > 实况预报数据对比 < /t-menu-item>
      // < t - menu - item value = "/estimate" > 预报质量分析评估 < /t-menu-item>
      // < t - menu - item value = "/model" > 模型管理 < /t-menu-item>
      // < t - menu - item value = "/dataqc" > 数据质量管理 < /t-menu-item>
      {
        path: "compare",
        name: "compare",
        component: () => import("@/pages/compare/index.vue"),
        meta: {
          permission: 'write'
        }
      },
      {
        path: "estimate",
        name: "estimate",
        component: () => import("@/pages/estimate/index.vue"),
        meta: {
          permission: 'delete'
        }
      },
      {
        path: "model",
        name: "model",
        component: () => import("@/pages/model/index.vue"),
        meta: {
          permission: 'edit'
        }
      },
      {
        path: "model/create",
        name: "create",
        // component: () => import("@/pages/model/components/CreateModel.vue"),
        component: () => import("@/pages/model/components/NewCreateModel.vue"),
        meta: {
          permission: 'edit'
        },
      },
      {
        path: "dataqc",
        name: "dataqc",
        component: () => import("@/pages/dataqc/index.vue"),
        meta: {
          permission: 'data'
        }
      },
      {
        path: "system",
        name: "system",
        component: () => import("@/pages/system/index.vue"),
        meta: {
          permission: 'user'
        }
      },
      {
        path: "log",
        name: "log",
        component: () => import("@/pages/log/index.vue"),
        meta: {
          permission: 'log'
        }
      },
      {
        path: "404",
        name: "404",
        component: () => import("@/pages/404.vue")
      }

      // {
      //   path: "fullscreen",
      //   name: "fullscreen",
      //   component: () => import("@/pages/fullscreen/index.vue"),
      // },

      // {
      //   path: "blank",
      //   name: "blank",
      //   component: () => import("@/pages/blank/index.vue"),
      // },
      // {
      //   path: "timeline",
      //   name: "timeline",
      //   component: () => import("@/pages/timeline/index.vue"),
      //   meta: {
      //     showPanels: {
      //       timeline: true,
      //     },
      //   }
      // },
      // {
      //   path: "customleft",
      //   name: "customleft",
      //   component: () => import("@/pages/customLeft/index.vue"),
      //   meta: {
      //     showPanels: {
      //       leftPanel: true,
      //     },
      //   }
      // },
      // {
      //   path: "rightTopPanel",
      //   name: "rightTopPanel",
      //   component: () => import("@/pages/rightTopPanel/index.vue"),
      //   meta: {
      //     showPanels: {
      //       mapTools: true
      //     },
      //   }
      // },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, from, next) => {
  // next() 
  // return
  if (to.name === '404') {
    next()
    return
  }
  if (to.path === "/login") return next();

  const userStore = useUserStoreWithOut()
  const isLogin = await userStore.checkLogin();
  if (!userStore.token || !isLogin) {
    return next("/login");
  }
  if (window.hasPermission(to.meta.permission as any)) {
    return next();
  } else {
    // 如果没有权限则直接取404页面
    next('404')
  }
});

export default router
