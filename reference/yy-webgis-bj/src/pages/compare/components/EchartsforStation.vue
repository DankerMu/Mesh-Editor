<template>
  <div ref="chartRef" class="echarts" style="width: 100%;" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onMounted, ref, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import handlerImg from '@/assets/icon/handler.png'
import dayjs from 'dayjs';

const height = ref(380 / window.devicePixelRatio)
function updateHeight() {
  height.value = 380 / window.devicePixelRatio
}

const chartRef = ref(null);
let chart = null
let data = null
let xData = []
let yData = []
let unit = ''
let realtimeData = null
let predictionData = null
let seriesData = []
let legends = []
let lastDate = ''

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
      // type: 'inside',
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
    let dataObj = data[k].data[feature.value].map(item => {
      let arr = Object.values(item)
      if (arr[0] === 'null') arr[0] = null
      return arr[0]
    })
    seriesData.push({
      name: data[k].label,
      // type: 'bar',
      type: 'line',
      data: dataObj,
      // smooth: true,
      itemStyle: {
        color: data[k].color
      },
      // lineStyle: {
      //   color: data[k].color
      // },
      connectNulls: true
    })
    legends.push(data[k].label)
  }

  // x轴取fst订正的数据
  let xtempdata = data.filter(x => x.label === '订正')[0]
  for (let k in xtempdata.data) {
    xtempdata.data[k].forEach(item => {
      xData.push(dayjs(Object.keys(item)).format('MM/DD HH'))
    })
    return
  }
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