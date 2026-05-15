<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <t-tabs v-model="value" class="estimate-tabs content-bg">
              <t-tab-panel value="first" label="用户列表" :destroyOnHide="false">
                <UserPanel ref='userRef'></UserPanel>
              </t-tab-panel>
              <t-tab-panel value="second" label="角色与权限" :destroyOnHide="false">
                <RolePanel ref='roleRef'></RolePanel>
              </t-tab-panel>
            </t-tabs>
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import LayoutManager from "@/layouts/LayoutManager.vue"
import UserPanel from './components/UserPanel.vue'
import RolePanel from './components/RolePanel.vue'


let value = ref('first')
let userRef = ref<InstanceType<typeof UserPanel> | null>(null);
let roleRef = ref<InstanceType<typeof RolePanel> | null>(null)

watch(value, (val) => {
  if (val === 'first') {
    if(roleRef.value) {
      roleRef.value.currentRoleName = ''
    }
    if(userRef.value) {
      userRef.value.getRoleList()
      userRef.value.getTableData()
      console.log('重新获取用户列表');
    }
  }
  else if (val === 'second') {
    userRef.value && (userRef.value.username = '')
    if(roleRef.value) {
      roleRef.value.search()
      console.log('重新获取角色列表');
      
    }
  }
})
</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 2;
  padding: var(--app-view-padding);
}

:deep(.t-tabs) {
  .t-tabs__nav-scroll {
    height: 5.0625rem;
  }
}

:deep(.t-tabs__nav-item-text-wrapper) {
  font-size: 1.5rem;
}

:deep(.search-ipt) {
  width: 344px;
  height: 40px;
  margin-right: 24px;

  .t-input {
    width: 344px;
    height: 40px;
  }
}

:deep(.btn) {

  // :deep(.t-button) {
  .t-button {
    height: 40px;
    background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
    border-radius: 4px 4px 4px 4px;

    .t-button__text {
      display: flex;
      align-items: center;

      img {
        width: 16px;
        height: 16px;
        margin-right: 4px;
      }
    }
  }
}

.operation-list {
  padding-left: 7.6875rem;
}

:deep(.divider) {
  border-top: 1px solid #263D98;
  margin: 1.3125rem 0;
}

.default-content {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;

  div {
    font-size: 1.5rem;
  }

  img {
    width: 17.625rem;
    height: 12.75rem;
  }
}

// :deep(.t-table__header--fixed:not(.t-table__header--multiple) > tr > th) {
//   background-color: #14266b;
// }

// :deep(.t-table) {
//   th {
//     border: none !important;
//   }

//   td {
//     border: none !important;
//   }
// }
</style>
