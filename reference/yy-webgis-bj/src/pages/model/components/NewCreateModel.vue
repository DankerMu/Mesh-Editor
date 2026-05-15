<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <div class="model-header">
              <HiIcon class="back" size="16px" :src="backstepSvg" @click="router.back()"></HiIcon>
              新建站点
              <img src="@/assets/icon/close.png" style="position: absolute;right: 24px;cursor: pointer;" alt=""
                @click="router.back()">
            </div>
            <div class="model-content">
              <t-radio-group v-model="mode" class="stationRadioGroup">
                <t-radio value="nil">
                  <div class="radio-title">无站点</div>
                </t-radio>
                <div class="flex" style="flex-direction: column;align-items: start;">
                  <div class="choose-box operate-container" style="padding: 0;">
                    <div class="title">名&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;称：</div>
                    <t-input v-model="nilStationName" class="ipt" placeholder="请输入站号" clearable
                      :disabled="mode === 'station'"></t-input>
                  </div>
                  <div class="choose-box operate-container" style="margin-top: 8px;padding: 0;">
                    <div class="title">站点选择：</div>
                    <t-input-group class="choose-district">
                      <t-input v-model="position.lon" label="经度：" placeholder="请输入经度" clearable
                        :disabled="mode === 'station'" />
                      <t-input v-model="position.lat" label="纬度：" placeholder="请输入纬度" clearable
                        :disabled="mode === 'station'" />
                    </t-input-group>
                    <!-- <t-button class="choose-btn" @click="openMap" :disabled="mode === 'station'">
                    <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                    手动选择
                  </t-button> -->
                  </div>
                  <div class="iptTip">经度：[70, 111），纬度：（25, 50]</div>
                </div>

                <t-radio value="station">
                  <div class="radio-title">站点&nbsp;&nbsp;&nbsp;</div>
                </t-radio>

                <div class="choose-box">
                  <div class="title">站点选择：</div>
                  <div class="extentStation">{{ selectedStations.join(',') }}</div>
                  <!-- <t-button class="choose-btn" @click="openMap" :disabled="mode === 'nil'">
                      <HiIcon size="20px" :src="chooseSvg"></HiIcon>
                      手动选择
                    </t-button> -->
                </div>

              </t-radio-group>

              <ChooseMap ref="choosemapRef" :mapMode="mode" @confirmStation="confirmStation"></ChooseMap>

            </div>
            <div class="operation-list" style="margin-top: 16px;">
              <t-button theme="primary" class="search mr-5" @click="createModel">
                <HiIcon class="svg" size="16px" :src="okSvg"></HiIcon>
                确认
              </t-button>
              <t-button type="reset" class="reset" @click="cancelCreate">
                <HiIcon class="svg" size="16px" :src="cancelSvg"></HiIcon>
                取消
              </t-button>
            </div>
          </div>
        </div>

      </template>
    </LayoutManager>
  </div>


</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import LayoutManager from "@/layouts/LayoutManager.vue"
import ChooseMap from './ChooseMapNew.vue'
import type { Feature } from 'ol';
import type { Geometry } from 'ol/geom';
import { useRouter } from "vue-router";
import { EstimateService, ModelService, DisplayService } from '@/api'
import type { Province, StationItem, City, Cnty, Station } from '@/types/PagesType'
import { type CascaderValue, type CascaderChangeContext, type TreeNodeModel, type TreeOptionData as CascaderOption, MessagePlugin } from 'tdesign-vue-next'
import { HiIcon } from "hoci";
import backstepSvg from '@/assets/icon/backstep.svg'
import chooseSvg from '@/assets/icon/choose.svg'
import okSvg from '@/assets/icon/ok.svg'
import cancelSvg from '@/assets/icon/cancel.svg'

const router = useRouter()

let choosemapRef = ref(null)
let mode = ref('nil')

watch(mode, (newval) => {
  if (newval === 'nil') {
    selectedStations.value = []
  } else {
    position.value.lon = ''
    position.value.lat = ''
  }
})

let nilStationName = ref('')
let position = ref({
  lon: '', // [70, 111]
  lat: '' // [25, 50]
})

function isValid(str) {
  // 不能全部是数字
  if (/^\d+$/.test(str)) return false
  // 不能包含小数点或负号
  if (/[.-]/.test(str)) return false
  return true
}

const checkParams = () => {
  if (mode.value === 'nil') {
    if (!nilStationName.value) {
      MessagePlugin.warning('请输入站名');
      return false
    }
    if (nilStationName.value.length > 20) {
      MessagePlugin.warning('站点名称不超过20个字符');
      return false
    }
    if (!isValid(nilStationName.value)) {
      MessagePlugin.warning('站名不能全部是数字，不能包含小数点或负号');
      return false
    }

    // try {
    //   const res = await ModelService.isDuplicatedStaionName({
    //     stationName: nilStationName.value
    //   })
    //   if (res > 0) {
    //     MessagePlugin.warning('站点名称重复，请重新输入');
    //     return false
    //   }
    // } catch (error) {
    //   MessagePlugin.warning('站点名称重复，请重新输入');
    //   return false
    // }

    if (!position.value.lon || !position.value.lat) {
      MessagePlugin.warning('请输入经纬度');
      return false
    }

    let reg = new RegExp(/^-?\d+(\.\d{1,6})?$/)
    if (!reg.test(position.value.lon)) {
      MessagePlugin.warning('经度请输入1-20位浮点数字符，最多保留6位小数');
      return false
    }
    if (!reg.test(position.value.lat)) {
      MessagePlugin.warning('纬度请输入1-20位浮点数字符，最多保留6位小数');
      return false
    }

    let iptLon = parseFloat(position.value.lon)
    let iptLat = parseFloat(position.value.lat)
    if (iptLon < 70 || iptLon > 110.9) {
      MessagePlugin.warning('经度范围在70至110.9之间');
      return false
    }
    if (iptLat < 25.1 || iptLat > 50) {
      MessagePlugin.warning('纬度范围在25.1至50之间');
      return false
    }
  } else {
    if (selectedStations.value.length === 0) {
      MessagePlugin.warning('请选择站点');
      return false
    }
  }

  return true
}

const createModel = async () => {
  if (!checkParams()) return
  if (mode.value === 'nil') {
    try {
      const userInfo = JSON.parse(sessionStorage.getItem("userInfo") ?? '{}')
      const uname = userInfo?.username ?? ""
      const res = await ModelService.addNoneStation({
        stationName: nilStationName.value,
        lon: position.value.lon,
        lat: position.value.lat,
        author: uname
      })
      if (res > 0) {
        MessagePlugin.success('创建成功！')
        choosemapRef && choosemapRef.value.openMap()
        nilStationName.value = ''
        position.value.lon = ''
        position.value.lat = ''
        // location.reload()
        // cancelCreate()
      } else {
        if (res == -10) {
          MessagePlugin.error('该经纬度已有站点，无法重复创建！')
        } else {
          MessagePlugin.error('创建失败！')
        }
      }

    } catch (error) {
      MessagePlugin.error('创建失败！')
    }
  } else {
    try {
      const res = await ModelService.addNoModelStation({
        stationsNum: selectedStations.value
      })
      if (res > 0) {
        MessagePlugin.success('创建成功！')
        choosemapRef && choosemapRef.value.openMap()
        // location.reload()
        // cancelCreate()
      } else {
        if (res == -10) {
          MessagePlugin.error('该经纬度已有站点，无法重复创建！')
        } else {
          MessagePlugin.error('创建失败！')
        }
        //  MessagePlugin.error('创建失败！')
      }
    } catch (error) {
      MessagePlugin.error('创建失败！')
    }
  }
}



const selectedStations = ref([])
const confirmStation = (val: string[] | any | Feature<Geometry>[]) => {
  // console.log('手动选择的站点', val);
  if (mode.value === 'nil') {
    position.value.lon = val.lon
    position.value.lat = val.lat
  } else if (mode.value === 'station') {
    selectedStations.value = val
  }
}

const cancelCreate = () => {
  router.back()
}

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 3;
  padding: var(--app-view-padding);
}

.model-header {
  display: flex;
  align-items: center;
  height: 54px;
  line-height: 54px;
  font-size: 16px;
  border-bottom: 1px solid #263D98;

  .back {
    color: var(--app-text-color-purple);
    margin-right: 4px;
    cursor: pointer;
  }
}

.model-content {
  position: relative;
  font-size: 16px;

  :deep(.stationRadioGroup) {
    margin-top: 10px;

    .radio-title {
      margin-right: 18px;
    }

    .t-radio {
      width: 300%;

      .t-radio__label {
        font-size: 16px;
        color: #fff;
      }

    }
  }


  .choose-box {
    display: flex;
    align-items: center;

    .title {
      color: #fff;
      line-height: 40px;
      white-space: nowrap;
    }
  }



  :deep(.ipt),
  :deep(.select) {
    width: 280px;

    .t-input {
      width: 280px;
      height: 40px;
      background: var(--app-border-color-dark);
    }
  }


  :deep(.choose-district) {
    .t-input {
      width: 140px;
      height: 40px;
      border-color: #2E3C8A !important;
      background: var(--app-border-color-dark);

      .t-input__prefix {
        color: var(--app-text-color-purple) !important;
      }

      .t-input__prefix:not(:empty) {
        margin-right: 0;
      }

      &.t-is-disabled {
        color: var(--app-text-color-purple) !important;
      }
    }

    .t-input__wrap:first-child {
      .t-input {
        border-right: none;
      }
    }

    .t-input__wrap:last-child {
      margin-right: 8px;

      .t-input {
        border-left: none;
      }
    }
  }


  :deep(.choose-btn) {
    width: 102px;
    height: 40px;
    background: var(--app-ui-bg-color);
    border-radius: 4px 4px 4px 4px;
    border: 1px solid #2E3C8A;

    .t-button__text {
      display: flex;
      align-items: center;
      color: var(--app-text-color-normal);

      >div {
        margin-right: 4px;
      }
    }

    &:hover {
      background: linear-gradient(188deg, #2692DF 0%, #3252D2 100%) !important;

      .t-button__text {
        color: #fff;
      }
    }
  }

  .extentStation {
    width: 280px;
    height: 40px;
    line-height: 40px;
    border: 1px solid #2E3C8A !important;
    background: var(--app-border-color-dark);
    color: var(--app-text-color-purple);
    border-radius: 4px;
    padding: 0 8px;
    white-space: nowrap;
    /* 不换行 */
    overflow: hidden;
    /* 超出隐藏 */
    text-overflow: ellipsis;
    /* 显示省略号 */
    cursor: auto;
  }
}

.iptTip {
  color: #b3a755;
  font-size: 10px;
  padding-left: 70px;
}
</style>