export function onMountedOrActivated(hook: () => any) {
  let mounted: boolean = false; // 首次加载不触发onActivated钩子,防止触发两次事件

  onMounted(() => {
    hook();
    nextTick(() => {
      mounted = true;
    });
  });

  // 请注意
  // onActivated 在组件挂载时也会调用，并且 onDeactivated 在组件卸载时也会调用。
  // 这两个钩子不仅适用于 <KeepAlive> 缓存的根组件，也适用于缓存树中的后代组件。
  // https://cn.vuejs.org/guide/built-ins/keep-alive.html#basic-usage
  onActivated(() => {
    if (mounted) {
      hook();
    }
  });
}
