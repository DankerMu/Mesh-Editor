<template>
  <main class="center-page">
    <section class="state-panel" aria-labelledby="forbidden-title">
      <h1 id="forbidden-title" class="state-title">权限不足</h1>
      <p class="state-desc">当前账号没有访问该页面的权限。</p>
      <t-button theme="primary" @click="$router.push('/')">返回首页</t-button>
    </section>
  </main>
</template>
