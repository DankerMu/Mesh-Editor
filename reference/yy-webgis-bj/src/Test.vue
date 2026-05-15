<!-- 起报时间组件 -->
<template>
    <div class="datetime-picker">
        <!-- 日期选择器 -->
        <DatePicker class="yy" :popupProps="{ overlayClassName: 'yy' }" overlayClassName v-model:value="selectedDate"
            type="date" format="YYYY-MM-DD" style="width: 250px; ">
            <template #valueDisplay="{ displayValue }">
                <span style="font-weight: bold;font-size: 13px;color:aliceblue">起报时间：</span>{{ displayValue }}
            </template>
        </DatePicker>

        <!-- 时间选择器 -->
        <Select class="yy" :popupProps="{ overlayClassName: 'yy' }" v-model:value="selectedTime"
            :disabled="!selectedDate" style="width: 120px;">
            <Option value="08:00">08:00</Option>
            <Option value="20:00">20:00</Option>
        </Select>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import dayjs from 'dayjs'; // 确保已安装 dayjs

import { DatePicker, Select, Option } from 'tdesign-vue-next';

// 使用 Day.js 对象存储日期
const selectedDate = ref(dayjs().startOf('day'));
const selectedTime = ref('08:00');

// 合并日期和时间（核心修复点）
const dateTime = computed(() => {
    if (!selectedDate.value || !selectedTime.value) return null;

    const [hours, minutes] = selectedTime.value.split(':').map(Number);

    // Day.js 链式调用设置时间
    return selectedDate.value
        .set('hour', hours)
        .set('minute', minutes)
        .set('second', 0) // 显式设置秒数为0
        ; // 转换为原生 Date 对象
});

// // 格式化显示
const formattedDateTime = computed(() => {
    return ''
    return dateTime.format('YYYY-MM-DD HH:mm') || '请选择日期和时间';
});
</script>

<style scoped>
.datetime-picker {
    position: absolute;
    bottom: 0;
    display: flex;
    align-items: center;
    gap: 5px;
}
</style>