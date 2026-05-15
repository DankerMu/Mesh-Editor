<template>
    <div class="timeline-container-wrapper">
        <!-- <div class="masker">

        </div> -->
        <div class="timeline-container">

            <div class="datetime-picker">
                <!-- 日期选择器 -->
                <DatePicker class="gradient-color-ipt" :popupProps="{ overlayClassName: 'yy-date-picker-popup' }" size='large' overlayClassName
                    v-model:value="selectedDate" type="date" format="YYYY-MM-DD"
                    style="width: 250px; margin-right: 10px;" @change="handleDatePicker">
                    <template #valueDisplay="{ displayValue }">
                        <span style="font-weight: bold;font-size: 13px;color:aliceblue">起报时间：</span>{{ displayValue }}
                    </template>
                </DatePicker>

                <!-- 时间选择器 -->
                <Select class="gradient-color-ipt" :popupProps="{ overlayClassName: 'gradient-select-popup' }" size='large' v-model:value="selectedTime"
                    @click.stop :disabled="!selectedDate" style="width: 120px;" @change="handleSelect">
                    <Option value="8" label="08:00">08:00</Option>
                    <Option value="20" label="20:00">20:00</Option>
                </Select>
            </div>

            <div class="controls">
                <!-- <t-button @click="navigate('prev')">向前</t-button>
           
            <t-button @click="startAutoPlay">开始</t-button>
            <t-button @click="navigate('next')">向后</t-button> -->
                <div class="btn_prev" @click="navigate('prev')"></div>
                <div :class="isPlaying ? 'btn_pause' : 'btn_play'" @click="toggleAutoPlay"></div>
                <div class="btn_next" @click="navigate('next')"></div>

            </div>

            <div class="timeline-wrapper">
                <div class="progress-bar-track">
                    <div v-for="(point, index) in timelinePoints" :key="index" class="progress-point"
                        :class="{ 'active': point.isCurrent }" :style="{ left: calcPointPosition(point.time) }"
                        @click="handlePointClick(point)">
                        <div class="time-label" v-if="point.isCurrent">
                            {{ point.time.format('MM-DD HH') }}时
                        </div>
                    </div>
                </div>
                <div class="date-container">
                    <div v-for="(date, index) in dateMarks" :key="index" class="date-mark"
                        :style="{ left: calcDatePosition(date) }">
                        <div> {{ date.format('MM-DD') }}</div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { Button as TButton } from 'tdesign-vue-next'
import { DatePicker, Select, Option } from 'tdesign-vue-next';
const props = defineProps({
    startTime: {
        type: dayjs,
        required: true
    },
    endTime: {
        type: dayjs,
        required: true
    },
    // subset: {
    //     type: Array,
    //     default: () => []
    // },
    currentPoint: {
        type: dayjs,
        required: true
    },
    currentIndex: {
        type: Number,
        required: true
    }
})


// 使用 Day.js 对象存储日期
const selectedDate = ref(dayjs().hour(8).minute(0).second(0).millisecond(0).format('YYYY-MM-DD HH:ss:mm'));
const selectedTime = ref('8');




const emit = defineEmits(['update:currentPoint', 'update:currentIndex', 'startDateChange', 'startTimeChange'])

const timelineRef = ref(null)

const subset = computed(() => {
    const points = [];
    let current = props.startTime.startOf('hour');
    console.log(current);
    let count = 0
    while (current.isBefore(props.endTime)) {
        points.push({
            time: current,
            isCurrent: current.isSame(props.currentPoint, 'hour')
        });
        if (count < 24)  //前72小时间隔3小时
            current = current.add(3, 'hour'); // 每隔3小时取一个点
        else
            current = current.add(6, 'hour'); // 每隔3小时取一个点
        count++
    }
    return points;
});


// 生成时间轴所有时间点
const timelinePoints = computed(() => {
    const points = []
    let current = props.startTime.startOf('hour')

    while (current.isBefore(props.endTime)) {
        points.push({
            time: current,
            isCurrent: current.isSame(props.currentPoint, 'hour')
        })
        current = current.add(3, 'hour') // 改为每3小时生成一个点
    }
    return points
})

// 自动播放相关逻辑
let autoPlayInterval = null
let isPlaying = ref(false)
const toggleAutoPlay = () => {
    isPlaying.value = !isPlaying.value
    if (isPlaying.value) {
        if (autoPlayInterval) clearInterval(autoPlayInterval)
        // navigate('next')
        autoPlayInterval = setInterval(() => {
            navigate('next')
        }, 2000)
    } else {
        clearInterval(autoPlayInterval)
    }

}

// 导航逻辑
const navigate = (direction) => {
    const currentIndex = subset.value.findIndex(t =>
        t.time.isSame(props.currentPoint, 'hour'))

    let newIndex = direction === 'next'
        ? currentIndex + 1
        : currentIndex - 1

    if (newIndex >= subset.value.length) newIndex = 0
    if (newIndex < 0) newIndex = subset.value.length - 1

    if (subset.value[newIndex]) {
        emit('update:currentPoint', subset.value[newIndex].time)
        scrollToPoint(subset.value[newIndex])
    }
}

// 点击时间点处理
const handlePointClick = (point) => {
    const indexTemp = subset.value.findIndex(p =>
        p.time.isSame(point.time, 'hour'))
    if (indexTemp === -1) {
        // 向前匹配到最近的数据节点
        for (let i = subset.value.length - 1; i >= 0; i--) {
            if (point.time.isAfter(subset.value[i].time)) {
                emit('update:currentPoint', subset.value[i].time)
                console.log('点击：', point.time.format('YYYY-MM-DD HH:mm:ss'), '靠近：', subset.value[i].time.format('YYYY-MM-DD HH:mm:ss'));

                scrollToPoint(subset.value[i])
                return
            }
        }
    } else {
        emit('update:currentPoint', point.time)
        scrollToPoint(point)
    }
}

// 滚动定位逻辑
const scrollToPoint = ({ time }) => {
    const index = timelinePoints.value.findIndex(p =>
        p.time.isSame(time, 'hour'))

    if (timelineRef.value && index !== -1) {
        const pointWidth = 80 // 每个时间点宽度
        const scrollPos = index * pointWidth - timelineRef.value.offsetWidth / 2
        timelineRef.value.scrollTo({
            left: scrollPos,
            behavior: 'smooth'
        })
    }
}

// 日期显示判断逻辑
// 计算时间点位置
const calcPointPosition = (time) => {
    const totalHours = props.endTime.diff(props.startTime, 'hour');
    const elapsedHours = time.diff(props.startTime, 'hour');
    return `${(elapsedHours / totalHours) * 100}%`;
}

// 生成日期刻度
const dateMarks = computed(() => {
    const dates = [];
    let current = props.startTime;
    let lastDate = current.date();

    while (current.isBefore(props.endTime)) {
        if (current.date() !== lastDate) {
            dates.push(current.startOf('day'));
            lastDate = current.date();
        }
        current = current.add(1, 'hour');
    }
    return dates;
})

// 计算日期位置
const calcDatePosition = (date) => {
    const totalHours = props.endTime.diff(props.startTime, 'hour');
    const elapsedHours = date.diff(props.startTime, 'hour');
    return `${(elapsedHours / totalHours) * 100}%`;
}

// 修改起报日期
const handleDatePicker = (val) => {
    emit('startDateChange', val)
}

// 修改起报时间
const handleSelect = (val) => {
    emit('startTimeChange', val)
}

watch(() => props.currentPoint, (newval) => {
    let newIndex = subset.value.findIndex(t => {
        if (t.time.isSame(props.currentPoint)) {
            return true
        }
    })
    emit('update:currentIndex', newIndex)
}, {
    immediate: true
})

// 初始化滚动位置
onMounted(() => {
    scrollToPoint(props.currentPoint)
})
onUnmounted(() => {
    if (autoPlayInterval) clearInterval(autoPlayInterval)
})
</script>

<style lang="less" scoped>
.timeline-container-wrapper {
    // height: 130px;
    width: 100vw;
    z-index: 9999;
    position: absolute;
    bottom: 0;
}

.timeline-container {
    position: absolute;
    bottom: 0;
    // z-index: 1;
    display: flex;
    gap: 2px;
    width: 100%;
    padding: 10px;
    overflow: visible;
    width: 100%;
    flex-direction: row;
    align-items: center;


    .controls {
        margin-right: 10px;
        gap: 2px;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        flex-direction: row;
    }

    .timeline-wrapper {
        // margin-right: 15px;
        flex: 1;
        // overflow-x: auto;
        overflow: visible;
    }

    .progress-bar-track {
        position: relative;
        height: 12px;
        background: linear-gradient(225deg, #21C0FF 0%, #3856E8 100%);
        border-radius: 6px;
        margin: 5px;
        margin-top: 15px;

        .progress-point {
            position: absolute;

            width: 12px;
            height: 12px; //background: rgba(1, 1, 1, 0);
            border-radius: 50%;
            transform: translateX(-50%);
            cursor: pointer;

            &.active {
                width: 12px;
                height: 12px;
                background: #fff;
                border-radius: 50%;
                box-shadow: 0 0 2px rgba(255, 255, 255, 0.9);
                border: 1px solid rgba(0, 0, 0, 0.3);
            }

            .time-label {
                position: absolute;
                top: -20px;
                left: 50%;
                width: 70px;
                text-align: center;
                transform: translateX(-50%);
                color: black;
                font-size: 12px;
                background-color: aliceblue;
                padding: 2px;
                border-radius: 2px;
            }
        }
    }

    .date-mark {
        font-size: 13px;
        transform: translateX(-50%) translateY(2px);
    }

    .date-container {
        position: relative;
        height: 14px;
        margin: 0 10px;

        .date-mark {
            position: absolute;
            bottom: 0;
            color: rgba(186, 203, 255, 0.8);
            font-size: 12px;
            transform: translateX(-50%);

            div {
                min-width: 34px;
            }
        }
    }

    .timeline-wrapper {
        flex: 1;
        max-width: calc(100% - 180px);
    }
}


.datetime-picker {

    display: flex;
    align-items: center;
    gap: 2px;
    margin-right: 10px;

    // :deep(.t-input) {
    //     border-radius: 8px 8px 8px 8px;
    //     border-color: var(--app-border-color-gray) !important;
    // }
}


.controls {

    >div {
        cursor: pointer;
    }

    .btn_prev {
        width: 40px;
        height: 40px;
        background: url('@/assets/icon/prev_btn.png') no-repeat center;
        background-size: contain;
    }

    .btn_next {
        width: 40px;
        height: 40px;
        background: url('@/assets/icon/next_btn.png') no-repeat center;
        background-size: contain;
    }

    .btn_play {
        width: 40px;
        height: 40px;
        background: url('@/assets/icon/play_btn.png') no-repeat center;
        background-size: contain;
    }

    .btn_pause {
        width: 40px;
        height: 40px;
        background: url('@/assets/icon/pause_btn.png') no-repeat center;
        background-size: contain;
    }
}

.timeline-wrapper {
    max-width: none;
}



.masker {
    // z-index: 2;
    // position: absolute;
    // bottom: 0;
    background: linear-gradient(180deg, rgba(11, 16, 30, 0) 9%, #0B101E 85%);
    opacity: 0.6;
    height: 100%;
    width: 100%;
    // z-index: 0;
}
</style>