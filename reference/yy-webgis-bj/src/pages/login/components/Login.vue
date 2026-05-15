<template>
  <div class="form-title">登录系统</div>
  <t-form ref="form" class="item-container" :class="[`login-${type}`]" :data="formData" :rules="FORM_RULES"
    @submit="onSubmit" label-width="auto" label-align="left">
    <template v-if="type === 'password'">
      <t-form-item name="u" label="账号">
        <t-input v-model="formData.u" name="username" size="large" placeholder="请输入您的账号" clearable>
          <template #prefix-icon>
            <img src="@/assets/icon/login-user.png" alt="">
          </template>
        </t-input>
      </t-form-item>

      <t-form-item name="p" label="密码">
        <t-input v-model="formData.p" name="password" size="large" :type="showPsw ? 'text' : 'password'" clearable
          placeholder="请输入您的密码">
          <template #prefix-icon>
            <img src="@/assets/icon/login-pwd.png" alt="">
          </template>
          <template #suffix-icon>
            <t-icon :name="showPsw ? 'browse' : 'browse-off'" @click="showPsw = !showPsw" />
          </template>
        </t-input>
      </t-form-item>

      <!-- <t-form-item name="verifyCode" label="验证码">
        <div class="flex-between w-full">
          <t-input v-model="formData.verifyCode" size="large" clearable placeholder="请输入验证码" style="width: 235px;">
            <template #prefix-icon>
              <img src="@/assets/icon/login-valid.png" alt="">
            </template>
          </t-input>
          <img :src="validateImgSrc" alt="" @click="getCaptcha"
            style="width: 90px; height: 40px; margin-left: 5px;cursor: pointer;">
        </div>
      </t-form-item> -->
    </template>

    <t-form-item v-if="type !== 'qrcode'" class="btn-container">
      <div class="group w-full">
        <t-button block size="large" type="submit" class="login-btn"> 登录 </t-button>
        <!-- <p class="mt-3">忘记密码</p> -->
      </div>
    </t-form-item>
  </t-form>
</template>

<script setup lang="ts">
import type { FormInstanceFunctions, FormRule, SubmitContext } from "tdesign-vue-next";
import { MessagePlugin } from "tdesign-vue-next";
import { useRoute, useRouter } from "vue-router";
import { debounce } from "lodash-es";
import { UserService } from '@/api'

const userStore = useUserStore();

const INITIAL_DATA = {
  u: "", // 用户名
  p: "", // 密码
  // verifyCode: "", // 验证码
};

const FORM_RULES: Record<string, FormRule[]> = {
  u: [{ required: true, message: "请输入账号", type: "error" }],
  p: [{ required: true, message: "请输入密码", type: "error" }],
  // verifyCode: [{ required: true, message: "请输入验证码", type: "error" }],
};

const type = ref("password");

const form = ref<FormInstanceFunctions>();
const formData = ref({ ...INITIAL_DATA });
const showPsw = ref(false);
const validateImgSrc = ref("");

const router = useRouter();
const route = useRoute();

// const getCaptcha = debounce(async () => {
//   // debugger
//   try {
//     const res = await UserService.validateCode();
//     validateImgSrc.value = `data:image/png;base64,${btoa(
//       new Uint8Array(res).reduce(
//         (data, byte) => data + String.fromCharCode(byte), "")
//     )}`
//   } catch (error) { }
// }, 120);


// getCaptcha();

async function onSubmit(ctx: SubmitContext) {
  if (ctx.validateResult === true) {
    try {
      const res = await userStore.login(formData.value);

      MessagePlugin.success("登录成功");
      const redirect = route.query.redirect as string;
      const redirectUrl = redirect ? decodeURIComponent(redirect) : "/";
      router.push(redirectUrl);
    } catch (e: any) {
      // await getCaptcha();
      MessagePlugin.error(e.response?.data?.message || '登录失败');
    }
  }
}
</script>

<style lang="less" scoped>
@import "../index.less";
</style>
