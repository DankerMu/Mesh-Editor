<template>
  <div
    class="yy-header  header w-full z-999 bg-[#fff] flex h-17.5 text-white header-content justify-between items-center relative header-container"
    style="height: 96px;">
    <t-head-menu :value="activeId" @change="handleMenuChange">
      <!-- logo -->
      <template #logo>
        <div style="width:  500px;"></div>
      </template>

      <!-- menu -->
      <!-- <t-menu-item value="/zndz"> 预测产品展示 </t-menu-item>
      <t-menu-item value="/compare"> 实况预报数据对比 </t-menu-item>
      <t-menu-item value="/estimate"> 预报质量分析评估 </t-menu-item>
      <t-menu-item value="/model"> 模型管理 </t-menu-item>
      <t-menu-item value="/dataqc"> 数据质量管理 </t-menu-item>
      <t-menu-item value="/system"> 用户与权限管理 </t-menu-item>
      <t-menu-item value="/log"> 日志管理 </t-menu-item> -->
      <t-menu-item v-for="item in displaySelectList" :key="item.role" :value="item.value">{{ item.label }}</t-menu-item>

      <!-- 右上角 -->
      <template #operations>
        <t-badge class="flex" style="margin-right: 5px;" :count="notiCount" size="small">
          <HiIcon size="26px" :src="notifiSvg" class="noti-btn" @click="openNotification"></HiIcon>
        </t-badge>
        <t-dropdown trigger="click" @click="onConfirm" :popupProps="{ overlayClassName: 't-logout-dropdown' }">
          <t-button variant="text" size="large" style="padding: 0 16px;border: none;">
            <div class="flex items-center " style="color: white;">
              {{ userStore.userName || uname }}
              <t-icon class="ml-3" name="user" />
            </div>
          </t-button>
          <t-dropdown-menu>
            <t-dropdown-item :value="0" class="edit-item">
              <!-- <img :src="editPng" alt=""> -->
              <HiIcon size="16px" :src="editSvg" class="edit"></HiIcon>
              修改密码
            </t-dropdown-item>
            <t-dropdown-item :value="1" class="edit-item">
              <!-- <img :src="logoutPng" alt=""> -->
              <HiIcon size="16px" :src="logoutSvg" class="edit"></HiIcon>
              退出登录
            </t-dropdown-item>
          </t-dropdown-menu>
        </t-dropdown>
      </template>
    </t-head-menu>
  </div>

  <!-- 修改密码 -->
  <dialog-vue :visible="newVisible" :header="header" :customClass="'editpwd-dialog custom-dialog-footer'"
    :close="newClose">
    <template #content>
      <t-form ref="formRef" :data="formData" class="item-container" :rules="FORM_RULES" label-width="auto"
        label-align="left" scrollToFirstError="smooth">
        <t-form-item label="原密码" name="oldpwd">
          <t-input v-model="formData.oldpwd" clearable placeholder="请输入原密码"></t-input>
        </t-form-item>
        <t-form-item label="新密码" name="newpwd">
          <t-input v-model="formData.newpwd" clearable placeholder="请输入新密码"></t-input>
        </t-form-item>
        <t-form-item label="确认密码" name="confirmpwd">
          <t-input v-model="formData.confirmpwd" clearable placeholder="请再次输入新密码"></t-input>
        </t-form-item>
      </t-form>
    </template>
    <template #button>
      <div class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="newClose">取消</t-button>
        <t-button class="confirm-btn" @click="confirm">确定</t-button>
      </div>
    </template>
  </dialog-vue>

  <dialog-vue :visible="notiVisible" header="告警信息" :customClass="'noti-dialog custom-dialog-footer'"
    :close="() => notiVisible = false">
    <template #content>
      <ul>
        <li v-for="(item, index) in info" :key="index">{{ index + 1 }}. {{ item }}</li>
      </ul>
    </template>
  </dialog-vue>
</template>

<script setup lang="ts">
import DialogVue from '@/components/DialogVue.vue'
import { useRoute, useRouter } from "vue-router";
import editPng from '@/assets/icon/editpwd.png'
import logoutPng from '@/assets/icon/logout.png'
import type { FormInstanceFunctions } from 'tdesign-vue-next'
import { MessagePlugin } from "tdesign-vue-next";
import NotificationIcon from 'tdesign-vue-next'
import { HiIcon } from "hoci";
import editSvg from '@/assets/icon/editpwd.svg'
import logoutSvg from '@/assets/icon/logout.svg'
import notifiSvg from '@/assets/icon/Notification.svg'
import { DisplayService } from '@/api';
import dayjs from 'dayjs'

const userStore = useUserStore();

const route = useRoute();
const router = useRouter();

const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
const uname = ref(userInfo?.username ?? "")

const menuList = [
  {
    label: "预报展示",
    value: '/zndz',
    role: "read"
  },
  {
    label: "预报对比",
    value: '/compare',
    role: "write"
  },
  {
    label: "预报检验",
    value: '/estimate',
    role: "delete"
  },
  {
    label: "模型管理",
    value: '/model',
    role: "edit"
  },
  {
    label: "数据管理",
    value: '/dataqc',
    role: "data"
  },
  {
    label: "用户与权限管理",
    value: '/system',
    role: "user"
  },
  {
    label: "日志管理",
    value: '/log',
    role: "log"
  },
]
const displaySelectList = computed(() => {
  // return menuList
  return menuList.filter((item) => {
    return window.hasPermission(item.role)
  })
})

interface DropdownItem {
  value: number;
  content: string;
}

const userOptions: DropdownItem[] = [
  {
    content: "修改密码",
    value: 0,
  },
  {
    content: "退出登录",
    value: 1,
  },
];

function onConfirm(dropdownItem: DropdownItem) {
  const { value } = dropdownItem;
  switch (value) {
    case 0:
      openEdit()
      break
    case 1:
      // 退出
      userStore.logout();
      break;
    default:
      break;
  }
}

const activeId = computed(() => {
  return `/${route.path.split("/")[1]}`;
});

function handleMenuChange(value: string) {
  router.push(value);
}

let newVisible = ref(false)
const openEdit = () => {
  newVisible.value = true
}
let header = ref('修改密码')
const formRef = ref<FormInstanceFunctions>()
const formData = reactive({
  oldpwd: '',
  newpwd: '',
  confirmpwd: ''
});

const FORM_RULES = {
  oldpwd: [{ required: true, message: "请输入原密码", type: "error" }],
  newpwd: [
    { required: true, message: "请输入新密码", type: "error" },
    {
      validator: (val: string) => {
        if (!val) return true;
        return val !== formData.oldpwd;
      },
      message: '新密码不能与旧密码相同',
      type: 'error',
      trigger: 'blur'
    }
  ],
  confirmpwd: [
    { required: true, message: "请再次输入新密码", type: "error" },
    {
      validator: (val: string) => {
        if (!val) return true;
        return val === formData.newpwd;
      },
      message: '确认密码与新密码不一致',
      type: 'error',
      trigger: 'blur'
    }
  ],
};

const newClose = () => {
  formData.oldpwd = ''
  formData.newpwd = ''
  formData.confirmpwd = ''

  newVisible.value = false
}

const confirm = () => {
  formRef.value?.validate().then(async (result) => {
    if (result === true) {
      // 校验通过
      let params = {
        userName: uname.value,
        oldPwd: formData.oldpwd,
        newPwd: formData.newpwd
      }
      await userStore.modifyPassword(params)
      MessagePlugin.success("修改成功，请重新登录");
    } else {
      console.log('校验失败', result);
    }
  }).catch((errors: any) => {
    // 校验失败
    MessagePlugin.error(errors.message);
  })
}

let notiVisible = ref(false)
let notiCount = ref(0)
let info = ref([])
let clickTime = ''

// 系统刷新，初始化获取告警信息和数量
const getClickTime = () => {
  let localClickTime = localStorage.getItem('clickTime')
  if (localClickTime) {
    clickTime = localClickTime
    getWarningCount(clickTime)
    getWarningInfo()
  } else {
    setClickTime()
  }
}

const setClickTime = async () => {
  try {
    clickTime = await DisplayService.getServerTime()
    localStorage.setItem('clickTime', clickTime)

    getWarningCount(clickTime)
    getWarningInfo()
  } catch (error) { }
}

const openNotification = async () => {
  await setClickTime()
  notiVisible.value = true
}

const getWarningCount = async (time) => {
  const res = await DisplayService.getWarningCount({
    insertTime: time
  })
  notiCount.value = res ?? 0
}
const getWarningInfo = async () => {
  const res = await DisplayService.getWarningInfo()
  info.value = res.map((item: any) => item.insertTime + ' ' + item.content).reverse()
}

getClickTime() // 系统刷新，初始化获取告警信息和数量


let interval = null
onMounted(() => {
  interval = setInterval(() => {
    getWarningCount(clickTime)
    getWarningInfo()
  }, 30 * 1000 * 1)
})


onBeforeUnmount(() => {
  clearInterval(interval)
  interval = null
})
</script>

<style lang="less">
.header {
  .t-menu {
    height: 100%;

    .t-head-menu__inner {
      height: 100%;
      align-items: center;

      .t-menu__item {
        height: 65%;
        padding: 0 2rem;

        .t-menu__content {
          height: 100%;
          display: flex;
          align-items: center;
        }
      }

      .t-menu__logo {
        width: 15rem;
        margin-left: 3rem;
        margin-right: 3rem;

        * {
          margin-left: 0;
        }

        img {
          width: 100%;
          object-fit: contain;
        }
      }

      .t-menu__operations {
        height: 100%;
      }
    }
  }
}

.editpwd-dialog {
  width: 368px;
  // height: 410px;

  .t-dialog__body {
    padding-top: 0;

    .item-container {
      margin-top: 16px;

      .t-form__item {
        width: 100%;
        margin-bottom: 20px;
        margin-right: 0;

        .t-form__label {
          float: none;
          color: var(--app-text-color);
          font-size: 16px;
        }

        .t-form__controls {
          margin-left: 0 !important;
        }
      }

      .t-form__item:last-child {
        margin-bottom: 0;
      }

      .t-input__wrap {
        width: 100%;
        height: 36px;
        color: #fff !important;

        .t-input {
          width: 100%;
          height: 36px;
          color: #fff;
          border: 1px solid #2E3C8A !important;
          background: var(--app-border-color-dark);
        }

        input::placeholder {
          color: var(--app-text-color-purple);
        }
      }

      .t-input--focused {
        box-shadow: none;
      }

      .t-textarea {
        .t-textarea__inner {
          width: 100%;
          height: 96px;
          color: #fff;
          border-radius: 4px 4px 4px 4px;
          border: 1px solid #2E3C8A;
          background-color: var(--app-border-color-dark);

          &:focus {
            box-shadow: none;
          }
        }

        textarea::placeholder {
          color: var(--app-text-color-purple);
        }
      }
    }
  }
}

.noti-dialog {
  width: 500px;
  height: 470px;

  .t-dialog__body {
    height: calc(100% - 53px);

    ul {
      height: 100%;
      color: rgb(231 224 224);
      font-size: 12px;
      overflow-y: auto;

      li {
        background-color: rgba(255, 255, 255, 0.2);
        border-radius: 8px;
        padding: 5px;
        margin-bottom: 10px;
        letter-spacing: 1px;

        &:hover {
          background-color: rgba(255, 255, 255, 0.3);
          box-shadow: 0 0 3px 0 rgba(255, 255, 255, 0.9);
        }
      }

      li:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.noti-btn {
  color: #fff;
  cursor: pointer;

  &:hover {
    color: var(--app-text-color-normal);
  }
}
</style>
<style>
.t-logout-dropdown {
  width: 208px;
  height: 104px;
  background: linear-gradient(45deg, #1E2763 0%, #263A8C 100%);
  box-shadow: 0px 3px 6px 0px rgba(19, 29, 80, 0.4);
  border-radius: 4px 4px 4px 4px;
  opacity: 0.98;
}

.t-logout-dropdown .t-dropdown {
  background-color: transparent;
  border: none;
  box-shadow: none;

}

.t-logout-dropdown .t-dropdown .t-dropdown__item {
  max-width: 100% !important;
  color: #fff;
  padding-top: 8px;
  padding-bottom: 8px;
  border-radius: 4px 4px 4px 4px;
}

.t-logout-dropdown .t-dropdown .t-dropdown__item .t-dropdown__item-text {
  display: flex;
  align-items: center;

  >img {
    margin-right: 4px;
  }
}


.edit-item .edit {
  margin-right: 4px;
  color: #a8b8ff;
}

.edit-item:hover .edit {
  /* color: #fff; */
  background: linear-gradient(225deg, #1EA9FF 0%, #3856E8 100%);
  ;
}


.edit-item:hover {
  background-color: #1f2d6cE6;
  color: var(--app-text-color-normal) !important;
}

/* .edit-item:hover img {
  content: url('../../assets/icon/editpwd-active.png');
} */

/* .logout-item:hover {
  background-color: #1f2d6cE6;
  color: var(--app-text-color-normal) !important;
}

.logout-item:hover img {
  content: url('../../assets/icon/logout-active.png');
} */
</style>
