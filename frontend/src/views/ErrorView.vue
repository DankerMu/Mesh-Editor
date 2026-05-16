<template>
  <main class="center-page">
    <section class="state-panel" aria-labelledby="error-title">
      <h1 id="error-title" class="state-title">服务器错误</h1>
      <p class="state-desc">服务暂时不可用，请稍后重试。</p>
      <t-button theme="primary" @click="$router.push('/')">返回首页</t-button>
    </section>
  </main>
</template>
