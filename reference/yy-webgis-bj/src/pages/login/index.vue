<template>
  <div class="login-wrapper">
    <div class="title"></div>
    <div class="login-container">
      <Login v-if="type === 'login'" />
    </div>
  </div>
</template>

<script lang="ts">
</script>

<script setup lang="ts">
import { ref } from "vue"

import Login from "./components/Login.vue"

const type = ref("login")
</script>

<style lang="less" scoped>
@import './index.less';
</style>
