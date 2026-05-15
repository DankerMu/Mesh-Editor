<template>
  <div ref="chartRef" class="echarts" style="width: 100%;" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onMounted, ref, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import handlerImg from '@/assets/icon/handler.png'
import dayjs from 'dayjs';

const height = ref(340 / window.devicePixelRatio)
function updateHeight() {
  height.value = 340 / window.devicePixelRatio
}

const chartRef = ref(null);
let chart = null
let data = null
let xData = []
let yData = []
let series = []
let legendData = []
let seriesData = []
let legends = []
let unit = ''
let lastDate = ''

// 图例映射配置
const legendMapping = {
  obs: "实况值",
  ecmf: "ECMF",
  grapes: "GRAPES"
};

const option = {
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    data: legends,
    textStyle: {
      color: '#fff',
      fontSize: 14
    },
    right: '5%',
    width: '60%'
  },
  grid: {
    top: '14%',
    left: '7%',
    right: '5%',
    bottom: '25%'
  },
  dataZoom: [
    {
      type: 'slider',
      start: 0,
      end: 58,
      // selectedDataBackground: '#091D5D',
      // fillerColor: '#0F2484'
      // moveHandleSize: 0,
      height: 20,
      bottom: '25'
    }
  ],
  xAxis: {
    type: 'category',
    // boundaryGap: false,
    data: xData,
    axisLabel: {
      interval: 'auto',
      color: '#fff',
      fontSize: 14,
      align: 'center',
      formatter: (value, index) => {
        const [date, hour] = value.split(' ')
        // if (index === 0) {
        //   lastDate = date
        //   return `${hour}\n${date}`
        // }

        // if (lastDate !== date) {
        //   lastDate = date
        //   return `${hour}\n${date}`
        // }
        // return hour
        return `${hour}\n${date}`
      }
    },
    boundaryGap: ['20%', '20%'],
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
    name: '温度(°C)',
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
  series: seriesData
};

const formatData = (data, feature) => {
  // xData = []
  // realtimeData = []
  // predictionData = []
  // let sortedObj = null
  // // 转换成键值对数组并排序
  // sortedObj = Object.fromEntries(
  //   data
  //     .map(obj => Object.entries(obj)[0]) // 转换成 [键, 值] 形式
  //     .sort(([keyA], [keyB]) => new Date(keyA) - new Date(keyB)) // 根据时间排序
  // );
  // for (let [k, v] of Object.entries(sortedObj)) {
  //   xData.push(dayjs(k).format('HH'))
  //   realtimeData.push(v.split(',')[0] === '999999' ? null : v.split(',')[0])
  //   predictionData.push(v.split(',')[1] === '999999' ? null : v.split(',')[1])
  // }

  // debugger
  xData = []
  seriesData = []
  legends = []
  for (let k in data) {
    let dataObj = Object.values(data[k].data).map(item => item === 'null' ? null : item)
    seriesData.push({
      name: data[k].label,
      type: 'line',
      // type: 'bar',
      data: dataObj,
      // smooth: true,
      itemStyle: {
        color: data[k].color
      },
      connectNulls: true
    })
    legends.push(data[k].label)

    // 取任何一个订正的数据的x
    if (xData.length === 0) {
      if(data[k].label.includes('订正')) {
        xData = Object.keys(data[k].data).map(item => dayjs(item).format('MM/DD HH'))
      }
    }
  }

  // x轴取fst订正的数据
  // let xtempdata = data.filter(x => x.label === '订正')[0]
  // xData = Object.keys(xtempdata.data).map(item => dayjs(item).format('MM/DD HH'))
}

const initChart = (data, feature, type) => {
  formatData(data, feature)

  let yName = feature.unit ? feature.label + '(' + feature.unit + ')' : feature.label
  if (chartRef.value) {
    if (chart) {
      chart.dispose(); // 先销毁旧实例
      chart = null;
    }
    option.xAxis.data = xData
    option.yAxis.name = yName
    option.legend.data = legends
    // option.series[0].data = predictionData
    // option.series[1].data = realtimeData
    option.series = seriesData
    chart = echarts.init(chartRef.value);
    chart.setOption(option, true);
  }
}

onMounted(() => {
  window.addEventListener('resize', updateHeight)
})

onBeforeUnmount(() => {
  // console.log('echarts销毁');
  if (chart) {
    chart.dispose();
    chart = null;
  }
  window.removeEventListener('resize', updateHeight)
})

defineExpose({
  initChart
})
</script>

<style>
.echarts {
  canvas {
    width: 100% !important;
  }
}
</style>