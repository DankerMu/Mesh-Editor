import { defineStore } from "pinia"
import { ref, computed, watch } from "vue"
import dayjs from "dayjs"

interface TimelineConfig {
  min: number
  max: number
  step: number
  speed?: number
  loop?: boolean
  autoReverse?: boolean
  marks?: Array<number>
}

export const useTimelineStore = defineStore("timeline", () => {
  const current = ref(0)
  const isPlaying = ref(false)
  const startDate = ref(dayjs().format('YYYY-MM-DD'))
  const startTime = ref('8')
  const config = ref<TimelineConfig>({
    min: 0,
    max: 100,
    step: 5,
    speed: 1000,
    loop: true,
    autoReverse: false,
    marks: []
  })

  const defaultConfig: TimelineConfig = {
    min: 0,
    max: 100,
    step: 5,
    speed: 1000,
    loop: true,
    autoReverse: false,
    marks: []
  }

  const labelList = ref([])
  let validTime = ref('24')

  let playInterval: ReturnType<typeof setInterval> | null = null
  
  const progress = computed(() => {
    const range = config.value.max - config.value.min
    return ((current.value - config.value.min) / range) * 100
  })
  
  const isAtStart = computed(() => current.value <= config.value.min)
  const isAtEnd = computed(() => current.value >= config.value.max)
  
  function initialize(newConfig: Partial<TimelineConfig> = defaultConfig) {
    config.value = { ...config.value, ...newConfig }
    current.value = config.value.min
    stop()
  }

  function setCurrentTime(time: number) {
    const { min, max } = config.value
    current.value = Math.max(min, Math.min(max, time))
  }

  function setStartDate(date: string) {
    startDate.value = date
  }

  function setStartTime(date: string) {
    startTime.value = date
  }

  function setLabelList(data: any) {
    labelList.value = data
  }

  function setValidTime(data: string) {
    validTime.value = data
  }

  function play() {
    if (isPlaying.value) return
    isPlaying.value = true
    const { step, min, loop, autoReverse } = config.value

    playInterval = setInterval(() => {
      if (isAtEnd.value) {
        if (loop) {
          if (autoReverse) {
            config.value.step = -Math.abs(step)
          } else {
            current.value = min
            return;
          }
        } else {
          stop()
        }
      } else if (isAtStart.value && autoReverse) {
        config.value.step = Math.abs(step)
      }
      
      if (isPlaying.value) {
        current.value += config.value.step
      }
    }, config.value.speed)
  }

  function stop() {
    if (playInterval) {
      clearInterval(playInterval)
      playInterval = null
    }
    isPlaying.value = false
  }

  function pause() {
    stop()
  }

  function next() {
    setCurrentTime(current.value + config.value.step)
  }

  function previous() {
    setCurrentTime(current.value - config.value.step)
  }

  function jumpTo(position: number) {
    const range = config.value.max - config.value.min
    const time = (position / 100) * range + config.value.min
    setCurrentTime(time)
  }

  const listeners = ref<Array<(time: number) => void>>([])
  
  function addTimeChangeListener(callback: (time: number) => void) {
    listeners.value.push(callback)
  }

  function removeTimeChangeListener(callback: (time: number) => void) {
    const index = listeners.value.indexOf(callback)
    if (index > -1) {
      listeners.value.splice(index, 1)
    }
  }

  watch(current, (newTime) => {
    listeners.value.forEach(callback => callback(newTime))
  })

  return {
    current,
    isPlaying,
    startDate,
    startTime,
    labelList,
    validTime,
    config,
    progress,
    isAtStart,
    isAtEnd,
    initialize,
    play,
    pause,
    stop,
    next,
    previous,
    jumpTo,
    setCurrentTime,
    setStartDate,
    setStartTime,
    setLabelList,
    setValidTime,
    addTimeChangeListener,
    removeTimeChangeListener
  }
})
