<template>
  <div
    class="w-10 h-auto p-y-[0.75rem] bg-[#ffffffCC] absolute rounded-1 bottom-[0.75rem] z-999"
    :class="mapPanelOptions.rightPanel ? 'opened' : 'closed'"
  >
    <div class="item">
      <t-popup class="placement align" placement="left" trigger="click" show-arrow destroy-on-close>
        <t-tooltip content="切换底图" placement="right">
          <EarthIcon />
        </t-tooltip>
        <template #content>
          <SwitchBaseLayer />
        </template>
      </t-popup>
    </div>
    <div class="item">
      <t-popup class="placement align" placement="left" trigger="click" show-arrow destroy-on-close v-model:visible="changeProjectionVisible">
        <t-tooltip content="切换投影" placement="right">
          <EarthIcon />
        </t-tooltip>
        <template #content>
          <ChangeProjection @close="changeProjectionVisible = false" />
        </template>
      </t-popup>
    </div>
    <div class="item">
      <t-tooltip content="经纬网" placement="right">
        <EarthIcon :class="{ active: lonlatShow }" @click="toggleGraticule" />
      </t-tooltip>
    </div>
    <div class="item">
      <t-tooltip content="地图复位" placement="right">
        <MapAimingIcon @click="resetViewer" />
      </t-tooltip>
    </div>
    <div class="item">
      <t-tooltip content="放大" placement="right">
        <ZoomInIcon @click="mapToolsHandlers.zoomIn" />
      </t-tooltip>
    </div>
    <div class="item">
      <t-tooltip content="缩小" placement="right">
        <ZoomOutIcon @click="mapToolsHandlers.zoomOut" />
      </t-tooltip>
    </div>
  </div>
</template>

<script setup lang="tsx">
import { EarthIcon, MapAimingIcon, ZoomInIcon, ZoomOutIcon } from 'tdesign-icons-vue-next'
import { fromLonLat } from "ol/proj";
import SwitchBaseLayer from './switchBaseLayer.vue'
import ChangeProjection from './changeProjection.vue'
import { useGraticuleLayer } from '@/composables/mapTools/useGraticule';

const { lonlatShow, toggleGraticule } = useGraticuleLayer()
const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);

const changeProjectionVisible = ref(false)

const mapToolsHandlers = {
  zoomIn: () => olMap?.zoomIn(),
  zoomOut: () => olMap?.zoomOut(),
}


/**
 * 地图复位
 */
function resetViewer () {
  olMap?.setCenter(fromLonLat([116, 39]))
  olMap?.setZoom(4)
  olMap?.setRotation(0)
};

</script>

<style lang="less" scoped>
.opened {
  right: calc(24vw + 1.5rem);
}

.closed {
  right: 0.75rem;
}

.item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:not(:last-child) {
    margin-bottom: 0.75rem;
  }

  .t-icon {
    color: black;
    font-size: 1.6rem;

    &.active {
      color: var(--td-brand-color);
    }
  }
}

ul {
  display: flex;
  justify-content: space-between;

  li {
    margin-right: 5px;
    font-size: 12px;
    height: 60px;
    cursor: pointer;
    position: relative;

    img {
      width: 92px;
      height: 60px;
      object-fit: cover;
      position: relative;
    }

    &:hover {
      &::after {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(68, 65, 65, 0.6);
        z-index: 9;
      }
    }
  }

  li.active {
    background-color: rgb(68, 65, 65);
  }
}
</style>
