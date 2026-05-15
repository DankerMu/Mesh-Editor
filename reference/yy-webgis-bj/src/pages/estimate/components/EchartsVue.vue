<template>
  <div ref="chartRef" style="width: 100%; height: 100%; min-height: 350px;margin-top: 10px;" />
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

interface SeriesItem {
  name: string
  data: number[]
  color: string
}

interface Props {
  xAxisData: string[]
  series: SeriesItem[]
  chartType: 'bar' | 'line'
  title?: string
  xUnit?: string
  yUnit?: string
}

const props = defineProps<Props>()

const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let intersectionObserver: IntersectionObserver | null = null

const initChart = () => {
  if (!chartRef.value || chartInstance) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const option: echarts.EChartsOption = {
    title: {
      text: props.title || '',
      left: 'center',
      textStyle: {
        color: '#fff',
      },
    },
    grid: {
      top: '15%',
      left: '7%',
      right: '5%',
      bottom: '10%'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      // top: 'right',
      data: props.series.map(s => s.name),
      textStyle: {
        color: '#fff',
        fontSize: 14
      },
      right: '5%',
      width: '25%'
    },
    // toolbox: {
    //   feature: {
    //     saveAsImage: {
    //       backgroundColor: '#172B79',
    //       // iconStyle: {
    //       //   color: '#fff'
    //       // }
    //     }
    //   }
    // },
    xAxis: {
      name: props.xUnit,
      nameLocation: 'end',
      nameTextStyle: {
        color: '#fff',
        align: 'left',
        padding: [0, 10, 0, 0],
        fontSize: 12
      },
      type: 'category',
      data: props.xAxisData,
      axisLabel: {
        interval: 'auto',
        color: '#fff',
        fontSize: 14,
        align: 'center'
      },
      // boundaryGap: ['20%', '20%'],
      axisTick: {
        show: false,
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#263D98'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: props.yUnit,
      nameLocation: 'end',
      nameTextStyle: {
        color: '#fff',
        align: 'left',
        // padding: [0, 0, 0, 0],
        fontSize: 12
      },
      axisLabel: {
        color: '#fff',
        fontSize: 14
      },
      splitLine: {
        lineStyle: {
          color: '#263D98'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#263D98'
        }
      }
    },
    series: props.series.map(s => ({
      name: s.name,
      type: props.chartType,
      data: s.data,
      // smooth: props.chartType === 'line',
      color: s.color,
      connectNulls: true,
    })),
  }

  chartInstance.setOption(option, true)
}

const resizeChart = () => {
  chartInstance?.resize()
}

watch(
  () => [props.xAxisData, props.series, props.chartType, props.title],
  () => updateChart(),
  { deep: true }
)

onMounted(() => {
  if (!chartRef.value) return

  intersectionObserver = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        if (!chartInstance) initChart()
        else resizeChart()
      }
    })
  }, { threshold: 0.1 })

  intersectionObserver.observe(chartRef.value)

  resizeObserver = new ResizeObserver(() => {
    resizeChart()
  })
  resizeObserver.observe(chartRef.value)
})

onBeforeUnmount(() => {
  if (resizeObserver && chartRef.value) resizeObserver.unobserve(chartRef.value)
  if (intersectionObserver && chartRef.value) intersectionObserver.unobserve(chartRef.value)
  chartInstance?.dispose()
  chartInstance = null
})

defineExpose({
  resizeChart,
})
</script>
