<template>
  <div class="timeline-container-wrapper">
    <!-- <div class="masker">

        </div> -->
    <div class="timeline-container">

      <div class="datetime-picker">
        <!-- 日期选择器 -->
        <DatePicker class="gradient-color-ipt" :popupProps="{ overlayClassName: 'yy-date-picker-popup' }" size='large'
          overlayClassName v-model:value="selectedDate" type="date" format="YYYY-MM-DD"
          style="width: 250px; margin-right: 10px;" @change="handleDatePicker">
          <template #valueDisplay="{ displayValue }">
            <span style="font-weight: bold;font-size: 13px;color:aliceblue">起报时间：</span>{{ displayValue }}
          </template>
        </DatePicker>

        <!-- 时间选择器 -->
        <Select class="gradient-color-ipt" :popupProps="{ overlayClassName: 'gradient-select-popup' }" size='large'
          v-model:value="selectedTime" @click.stop :disabled="!selectedDate" style="width: 120px;"
          @change="handleSelect">
          <Option value="8" label="08:00">08:00</Option>
          <Option value="20" label="20:00">20:00</Option>
        </Select>
      </div>

      <Select class="gradient-color-ipt" :options="timeLength"
        :popupProps="{ overlayClassName: 'gradient-select-popup' }" size='large' v-model:value="forcastLength"
        @click.stop style="width: 158px;" @change="handleTimeValidSelect">
      </Select>

      <div class="controls">
        <div class="btn_prev" @click="lastTime"></div>
        <ul class="date-list" ref="ulElement" @click="handleDateChange">
          <li v-for="(item, index) in timevalidLabel" :key="index" :data-date="index"
            :class="{ active: index == labelIndex }">{{
              item
            }}</li>
        </ul>
        <div class="btn_next" @click="nextTime"></div>
      </div>


    </div>

  </div>

  <div class="totaltime">{{ totalTime }}</div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { Button as TButton } from 'tdesign-vue-next'
import { DatePicker, Select, Option } from 'tdesign-vue-next';
import { useTimelineStore } from '@/stores';
const store = useTimelineStore()

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
// const selectedDate = ref(dayjs().hour(8).minute(0).second(0).millisecond(0).format('YYYY-MM-DD HH:ss:mm'));
const now = dayjs()
const target1 = dayjs().hour(8).minute(0).second(0)
const target2 = dayjs().hour(15).minute(20).second(0)
let defaulDate = dayjs().format('YYYY-MM-DD')
let defaultTime = '8'
if (now.isBefore(target1)) {
  defaulDate = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  defaultTime = '20'
} else if (now.isAfter(target2)) {
  defaultTime = '20'
}
const selectedDate = ref(defaulDate);
const selectedTime = ref(defaultTime);

const emit = defineEmits(['update:currentPoint', 'update:currentIndex', 'startDateChange', 'startTimeChange'])

// const timelineRef = ref(null)

// const subset = computed(() => {
//   const points = [];
//   let current = props.startTime.startOf('hour');
//   // console.log(current);
//   let count = 0
//   while (current.isBefore(props.endTime)) {
//     points.push({
//       time: current,
//       isCurrent: current.isSame(props.currentPoint, 'hour')
//     });
//     if (count < 24)  //前72小时间隔3小时
//       current = current.add(3, 'hour'); // 每隔3小时取一个点
//     else
//       current = current.add(6, 'hour'); // 每隔3小时取一个点
//     count++
//   }
//   return points;
// });


// 生成时间轴所有时间点
// const timelinePoints = computed(() => {
//   const points = []
//   let current = props.startTime.startOf('hour')

//   while (current.isBefore(props.endTime)) {
//     points.push({
//       time: current,
//       isCurrent: current.isSame(props.currentPoint, 'hour')
//     })
//     current = current.add(3, 'hour') // 改为每3小时生成一个点
//   }
//   return points
// })

// 自动播放相关逻辑
// let autoPlayInterval = null
// let isPlaying = ref(false)
// const toggleAutoPlay = () => {
//   isPlaying.value = !isPlaying.value
//   if (isPlaying.value) {
//     if (autoPlayInterval) clearInterval(autoPlayInterval)
//     // navigate('next')
//     autoPlayInterval = setInterval(() => {
//       navigate('next')
//     }, 2000)
//   } else {
//     clearInterval(autoPlayInterval)
//   }

// }

// 导航逻辑
// const navigate = (direction) => {
//   const currentIndex = subset.value.findIndex(t =>
//     t.time.isSame(props.currentPoint, 'hour'))

//   let newIndex = direction === 'next'
//     ? currentIndex + 1
//     : currentIndex - 1

//   if (newIndex >= subset.value.length) newIndex = 0
//   if (newIndex < 0) newIndex = subset.value.length - 1

//   if (subset.value[newIndex]) {
//     emit('update:currentPoint', subset.value[newIndex].time)
//     scrollToPoint(subset.value[newIndex])
//   }
// }

// 点击时间点处理
// const handlePointClick = (point) => {
//   const indexTemp = subset.value.findIndex(p =>
//     p.time.isSame(point.time, 'hour'))
//   if (indexTemp === -1) {
//     // 向前匹配到最近的数据节点
//     for (let i = subset.value.length - 1; i >= 0; i--) {
//       if (point.time.isAfter(subset.value[i].time)) {
//         emit('update:currentPoint', subset.value[i].time)
//         console.log('点击：', point.time.format('YYYY-MM-DD HH:mm:ss'), '靠近：', subset.value[i].time.format('YYYY-MM-DD HH:mm:ss'));

//         scrollToPoint(subset.value[i])
//         return
//       }
//     }
//   } else {
//     emit('update:currentPoint', point.time)
//     scrollToPoint(point)
//   }
// }

// 滚动定位逻辑
// const scrollToPoint = ({ time }) => {
//   const index = timelinePoints.value.findIndex(p =>
//     p.time.isSame(time, 'hour'))

//   if (timelineRef.value && index !== -1) {
//     const pointWidth = 80 // 每个时间点宽度
//     const scrollPos = index * pointWidth - timelineRef.value.offsetWidth / 2
//     timelineRef.value.scrollTo({
//       left: scrollPos,
//       behavior: 'smooth'
//     })
//   }
// }

// 日期显示判断逻辑
// 计算时间点位置
// const calcPointPosition = (time) => {
//   const totalHours = props.endTime.diff(props.startTime, 'hour');
//   const elapsedHours = time.diff(props.startTime, 'hour');
//   return `${(elapsedHours / totalHours) * 100}%`;
// }

// 生成日期刻度
// const dateMarks = computed(() => {
//   const dates = [];
//   let current = props.startTime;
//   let lastDate = current.date();

//   while (current.isBefore(props.endTime)) {
//     if (current.date() !== lastDate) {
//       dates.push(current.startOf('day'));
//       lastDate = current.date();
//     }
//     current = current.add(1, 'hour');
//   }
//   return dates;
// })

// 计算日期位置
// const calcDatePosition = (date) => {
//   const totalHours = props.endTime.diff(props.startTime, 'hour');
//   const elapsedHours = date.diff(props.startTime, 'hour');
//   return `${(elapsedHours / totalHours) * 100}%`;
// }

// 修改起报日期
const handleDatePicker = (val) => {
  labelIndex.value = 0
  emit('startDateChange', val)
}

// 修改起报时间
const handleSelect = (val) => {
  labelIndex.value = 0
  emit('startTimeChange', val)
}


// watch(() => props.currentPoint, (newval) => {
//   let newIndex = subset.value.findIndex(t => {
//     if (t.time.isSame(props.currentPoint)) {
//       return true
//     }
//   })
//   emit('update:currentIndex', newIndex)
// }, {
//   immediate: true
// })

// 初始化滚动位置
// onMounted(() => {
//   scrollToPoint(props.currentPoint)
// })
// onUnmounted(() => {
//   if (autoPlayInterval) clearInterval(autoPlayInterval)
// })

// ===========================================================================
let date = ref('')
const forcastLength = ref('24')
const timeLength = ref([
  {
    label: '3小时降水量',
    value: '3',
    start: 3,
    end: 72,
    step: 3
  },
  {
    label: '6小时降水量',
    value: '6',
    start: 6,
    end: 168,
    step: 6
  },
  {
    label: '12小时降水量',
    value: '12',
    start: 12,
    end: 240,
    step: 12
  },
  {
    label: '24小时降水量',
    value: '24',
    start: 24,
    end: 240,
    step: 12
  },
  {
    label: '48小时降水量',
    value: '48',
    start: 48,
    end: 240,
    step: 12
  },
  {
    label: '72小时降水量',
    value: '72',
    start: 72,
    end: 240,
    step: 12
  },
  {
    label: '96小时降水量',
    value: '96',
    start: 96,
    end: 240,
    step: 12
  },
  {
    label: '120小时降水量',
    value: '120',
    start: 120,
    end: 240,
    step: 12
  },
  {
    label: '144小时降水量',
    value: '144',
    start: 144,
    end: 240,
    step: 12
  },
  {
    label: '168小时降水量',
    value: '168',
    start: 168,
    end: 240,
    step: 12
  }
])

const handleTimeValidSelect = () => {
  labelIndex.value = 0
}
let labelIndex = ref(0)
const timevalidLabel = ref([])

const lastTime = () => {
  if (labelIndex.value > 0) labelIndex.value--
}
const nextTime = () => {
  if (labelIndex.value < timevalidLabel.value.length - 1) labelIndex.value++
}

const handleDateChange = (event) => {
  const target = event.target;
  // 确保点击的是 li 标签
  if (target.tagName.toLowerCase() === 'li') {
    labelIndex.value = target.dataset.date

  }
};

const fillNumber = (num) => {
  if (num < 10) return '00' + num.toString()
  else if (num < 100) return '0' + num.toString()
  else return num.toString()
}

watch(forcastLength, (val) => {
  let obj = timeLength.value.find(x => x.value === val)
  if (obj) {
    timevalidLabel.value = []
    for (let i = obj.start; i <= obj.end; i = i + obj.step) {
      timevalidLabel.value.push(fillNumber(i))
    }
  }
  store.setValidTime(val)
  store.setLabelList(timevalidLabel.value)

}, {
  immediate: true
})

watch(labelIndex, (val) => {
  emit('update:currentIndex', parseInt(val))
})

let totalTime = computed(() => {
  let hour = timevalidLabel.value[labelIndex.value]
  return dayjs(selectedDate.value).hour(selectedTime.value).add(parseInt(hour), 'hour').format('YYYY-MM-DD HH:mm:ss')
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
    margin-left: 10px;
    // gap: 2px;
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
    width: 30px;
    height: 30px;
    background: url('@/assets/icon/prev_btn.png') no-repeat center;
    background-size: contain;
  }

  .btn_next {
    width: 30px;
    height: 30px;
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

.date-list {
  display: flex;
  list-style: none;
  overflow-x: auto;
  flex: 1;
  padding: 0.9375rem 0;
  margin: 0 10px;
  white-space: nowrap;
  scroll-behavior: smooth;
}

.date-list li {
  height: 20px;
  line-height: 20px;
  padding: 0 4px;
  margin-right: 4px;
  font-size: 1.3125rem;
  color: #bcc8ff;
  // color: #000;
  background: #2D4098;
  // border: 1px solid #2D4098;
  cursor: pointer;
  border-radius: 0.375rem;
  transition: background 0.3s;
}

.date-list li:last-child {
  margin-right: 0;
}

.date-list li.active {
  color: #fff;
  background: linear-gradient(188deg, #21CFFF 0%, #3856E8 100%);
}

.date-list::-webkit-scrollbar {
  display: none;
  /* 隐藏滚动条 */
}

.totaltime {
  position: fixed;
  top: 112px;
  left: 50%;
  width: 250px;
  height: 46px;
  text-align: center;
  line-height: 46px;
  transform: translateX(-50%);
  border-radius: 8px;
  font-size: 24px;
  font-weight: 500;
  z-index: 1;
  color: var(--app-text-color-purple);
  background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%)
}
</style>