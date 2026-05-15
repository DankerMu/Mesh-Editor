<template>
  <div class="right-panel" :class="{ collapsed: !mapPanelOptions.rightPanel }">
    <div class="panel-content" :class="{ '!hidden': !mapPanelOptions.rightPanel }">
      <t-space direction="vertical">
        <t-button @click="addImgLayer">ImageLayer</t-button>
        <t-button @click="addImgLayerWorld">ImageLayer(world)</t-button>
        <t-button @click="addContour">addContour</t-button>
        <!-- <t-color-picker
          v-model="lineColor"
          :color-modes="['monochrome']"
          :swatch-colors="systemColors"
        />
        <t-slider v-model="lineWidth" :show-tooltip="true" :max="5" :min="0" :step="0.1" /> -->
        <t-button @click="addWindLayer">addWindLayer</t-button>
        <t-button @click="addContourPolygon">addContourPolygon</t-button>
        <t-button @click="addLmtLayer">addLmtLayer</t-button>
        <t-button @click="addCustomTileLayer">addCustomTileLayer</t-button>
        <t-button @click="addTileLayer">addTileLayer</t-button>
        <t-button @click="addTileLayerAllWorld">addTileLayer（all world）</t-button>
        <!-- <t-button @click="addTileLayerQd">addTileLayer（qd）</t-button> -->
      </t-space>
      <hr />
      <LayerManager />
    </div>
    <div
      class="toggle-icon cursor-pointer"
      @click="() => mapStore.togglePanel('rightPanel')"
    >
      <IndentLeftIcon v-if="!mapPanelOptions.rightPanel" />
      <IndentRightIcon v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
import { IndentLeftIcon, IndentRightIcon } from "tdesign-icons-vue-next";

import GeoJSON from "ol/format/GeoJSON";
import VectorSource from "ol/source/Vector";
import VectorLayer from "ol/layer/Vector";
import { Stroke, Style, Fill } from "ol/style";
import Text from "ol/style/Text";

import LayerManager from "@/components/LayerManager/index.vue";

const mapStore = useMapStore();
const mapPanelOptions = computed(() => mapStore.mapPanelOptions);

const projectionStore = useProjectionStore();

// ====================================================================================

import ImageLayer from "ol/layer/Image";
import { ImageStatic } from "ol/source";

import landTemperature_2024081208_part from "@/assets/fakeJson/imgs/landTemperature_2024081208_part.png";

function addImgLayer() {
  const extent = [0, -19.99999237060547, 180, 90.00000762939453];
  const layer = new ImageLayer({
    source: new ImageStatic({
      url: landTemperature_2024081208_part,
      imageExtent: extent,
      projection: "EPSG:4326",
    }),
    opacity: 0.8,
  });

  layerManagerStore.addLayer("imageLayer", layer);
}

import temperature_2024081208_world from "@/assets/fakeJson/imgs/temperature_2024081208_world.png";

function addImgLayerWorld() {
  const extent = [-180, -90, 180, 90];
  const layer = new ImageLayer({
    source: new ImageStatic({
      url: temperature_2024081208_world,
      imageExtent: extent,
      projection: "EPSG:4326",
    }),
    opacity: 0.8,
  });

  layerManagerStore.addLayer("imageLayer(world)", layer);
}

// ====================================================================================

const styleConfig = computed(() => {
  return {
    line: {
      color: lineColor.value || "#eee",
      width: lineWidth.value || 1,
    },
    text: {
      color: "#333",
      fontSize: "12px",
      offsetX: 0,
      offsetY: -10,
      visible: true,
    },
  };
});

const lineColor = ref("#eee");
const lineWidth = ref(1);
const systemColors = ref(["red", "green", "yellow", "blue", "purple"]);

async function addContour() {
  const contours = await import("@/assets/fakeJson/contours.json").then(
    (module) => module.default
  );

  const geoJsonFormat = new GeoJSON();
  const vectorSource = new VectorSource({
    features: geoJsonFormat.readFeatures(contours, {
      dataProjection: "EPSG:4326",
      featureProjection: "EPSG:3857",
    }),
  });

  // 创建等值线图层
  const layer = new VectorLayer({
    source: vectorSource,
    style: function (feature) {
      const value = feature.get("value");

      const textStyleConfig = styleConfig.value.text;
      const lineStyleConfig = styleConfig.value.line;

      const textStyle = textStyleConfig.visible
        ? new Text({
            text: value.toString(),
            font: `${textStyleConfig.fontSize} sans-serif`,
            fill: new Fill({
              color: textStyleConfig.color,
            }),
            stroke: new Stroke({
              color: "#ffffff",
              width: 3,
            }),
            offsetX: textStyleConfig.offsetX,
            offsetY: textStyleConfig.offsetY,
            placement: "point",
            overflow: true,
          })
        : new Text();

      return new Style({
        stroke: new Stroke({
          color: lineStyleConfig.color,
          width: lineStyleConfig.width,
        }),
        text: textStyle,
      });
    },
  });

  layerManagerStore.addLayer("等值线", layer);
}

watch([lineColor, lineWidth], updateContourStyle);

function updateContourStyle() {
  const contourLayer = layerManagerStore.layerInstances.get("等值线");

  contourLayer?.setStyle(function (feature) {
    const value = feature.get("value");

    const textStyleConfig = styleConfig.value.text;
    const lineStyleConfig = styleConfig.value.line;

    const textStyle = textStyleConfig.visible
      ? new Text({
          text: value.toString(),
          font: `${textStyleConfig.fontSize} sans-serif`,
          fill: new Fill({
            color: textStyleConfig.color,
          }),
          stroke: new Stroke({
            color: "#ffffff",
            width: 3,
          }),
          offsetX: textStyleConfig.offsetX,
          offsetY: textStyleConfig.offsetY,
          placement: "point",
          overflow: true,
        })
      : new Text();

    return new Style({
      stroke: new Stroke({
        color: lineStyleConfig.color,
        width: lineStyleConfig.width,
      }),
      text: textStyle,
    });
  });
}

// ====================================================================================

import { WindLayer } from "ol-wind";
async function addWindLayer() {
  const windData = await import("@/assets/fakeJson/wind.json").then(
    (module) => module.default
  );

  const layer = new WindLayer(windData, {
    forceRender: false,
    windOptions: {
      velocityScale: 1 / 150,
      paths: 4000,
      // eslint-disable-next-line no-unused-vars
      colorScale: [
        "rgb(0,0,250)",
        "rgb(0,100,250)",
        // 'rgb(0,100,200)',
        "rgb(36,104, 180)",
        // 'rgb(60,157, 194)',
        "rgb(128,205,193 )",
        "rgb(151,218,168 )",
        "rgb(198,231,181)",
        "rgb(238,247,217)",
        "rgb(255,238,159)",
        "rgb(252,217,125)",
        "rgb(255,182,100)",
        "rgb(252,150,75)",
        "rgb(250,112,52)",
        "rgb(245,64,32)",
        "rgb(237,45,28)",
        "rgb(220,24,32)",
        "rgb(180,0,35)",
      ],
      width: 3,
      generateParticleOption: false,
      lineWidth: 2,
      globalAlpha: 0.9,
    },
    fieldOptions: {
      wrapX: true,
    },
    // map: map,
    // projection: 'EPSG:4326'
  });

  layerManagerStore.addLayer("windLayer", layer);
}

// ====================================================================================

async function addContourPolygon() {
  // 1. 数据
  const contourPolygonData = await import("@/assets/fakeJson/isosurface.json").then(
    (module) => module.default
  );

  // 2. 从json数据中获取features
  let features = new GeoJSON({
    featureProjection: projectionStore.currentProjection, // 目标投影
    dataProjection: "EPSG:4326", // 数据源投影
  }).readFeatures(contourPolygonData);

  // 3. source
  const source = new VectorSource({
    features: features,
  });

  // 4. layer
  const layer = new VectorLayer({
    source: source,
    style: function (feature) {
      return new Style({
        fill: new Fill({
          color: feature.get("color") || "rgba(189, 178, 119, 0.8)",
        }),
        text: new Text({
          text: feature.get("val") || "",
          font: "16px Calibri,sans-serif",
          fill: new Fill({ color: feature.get("color") || "black" }),
          stroke: new Stroke({
            color: "white",
            width: 2,
          }),
        }),
      });
    },
  });

  // 5. addLayer
  layerManagerStore.addLayer("contourPolygon", layer);
}

// projection changed
async function updateContourPolygon() {
  const layer = layerManagerStore.layerInstances.get("contourPolygon");

  const newFeatures = new GeoJSON({
    featureProjection: projectionStore.currentProjection,
    dataProjection: "EPSG:4326",
  }).readFeatures(contourPolygonData);

  const source = layer.getSource();
  source.clear();
  source.addFeatures(newFeatures);
}

watch(() => projectionStore.currentProjection, updateContourPolygon);

// ====================================================================================

import dayjs from "dayjs";
const layerManagerStore = useLayerManagerStore();
const timelineStore = useTimelineStore();

async function addLmtLayer() {
  const params = {};
  const time = dayjs(timelineStore.current).format("YYYY-MM-DD HH:mm:ss");
  const data = await import("@/assets/fakeJson/isosurface.json").then(
    (module) => module.default
  );

  const layer = await generateLayerByData(data);

  layerManagerStore.addLayer("contour", layer);
}

function generateLayerByData(data) {
  return new Promise((resolve, reject) => {
    let features = new GeoJSON({
      featureProjection: projectionStore.currentProjection, // 目标投影
      dataProjection: "EPSG:4326", // 数据源投影
    }).readFeatures(data);

    const source = new VectorSource({
      features: features,
    });

    const layer = new VectorLayer({
      source: source,
      style: function (feature) {
        return new Style({
          fill: new Fill({
            color: feature.get("color") || "rgba(189, 178, 119, 0.8)",
          }),
          text: new Text({
            text: feature.get("val") || "",
            font: "16px Calibri,sans-serif",
            fill: new Fill({ color: feature.get("color") || "black" }),
            stroke: new Stroke({
              color: "white",
              width: 2,
            }),
          }),
        });
      },
    });

    resolve(layer);
  });
}

// ====================================================================================

import { transformExtent } from "ol/proj";
import TileLayer from "ol/layer/Tile";
import TileGrid from "ol/tilegrid/TileGrid.js";
import CustomImageTile from "@/components/OlMap/layers/CustomTileLayer.js";

async function addCustomTileLayer() {
  const data = await import("@/assets/fakeJson/customTile.json").then(
    (module) => module.default
  );

  // 设置色带和步长
  let lats = data.lat.map((item) => +item.toFixed(2));
  let step = lats[1] - lats[0];
  let options = {
    colors: [],
    values: [
      -50,
      -40,
      -30,
      -20,
      -10,
      -5,
      -2,
      0,
      3,
      6,
      9,
      12,
      14,
      16,
      18,
      20,
      22,
      24,
      26,
      28,
      30,
      32,
      34,
      36,
      38,
      40,
      42,
      60,
    ],
    min: -50,
    max: 60,
    step: step,
    n: 100,
  };
  const colors = [
    "rgb(3, 17, 126)",
    "rgb(5, 33, 176)",
    "rgb(4, 51, 203)",
    "rgb(0, 99, 252)",
    "rgb(0, 151, 252)",
    "rgb(48, 206, 253)",
    "rgb(153, 205, 253)",
    "rgb(142, 252, 249)",
    "rgb(191, 255, 218)",
    "rgb(156, 253, 158)",
    "rgb(203, 255, 155)",
    "rgb(155, 251, 3)",
    "rgb(1, 252, 149)",
    "rgb(0, 254, 104)",
    "rgb(253, 202, 199)",
    "rgb(255, 155, 205)",
    "rgb(254, 204, 155)",
    "rgb(253, 203, 104)",
    "rgb(254, 156, 7)",
    "rgb(255, 105, 102)",
    "rgb(234, 104, 20)",
    "rgb(255, 103, 102)",
    "rgb(255, 50, 99)",
    "rgb(253, 0, 3)",
    "rgb(205, 1, 50)",
    "rgb(201, 50, 205)",
    "rgb(204, 0, 151)",
    "rgb(150, 50, 100)",
  ];
  options.colors = colors.map((color) => {
    // 提取 RGB 数字
    const [r, g, b] = color.match(/\d+/g).map(Number);
    // 返回格式化对象
    return { r, g, b, a: 255 };
  });

  // 设置切片最大层级和分辨率列表
  let resolutionsList = [
    156543.0339,
    78271.51695,
    39135.758475,
    19567.8792375,
    9783.93961875,
    4891.969809375,
    2445.9849046875,
    1222.99245234375,
    611.496226171875,
    305.7481130859375,
    152.87405654296875,
  ];
  let maxZoom = 8;
  let curresolutions = resolutionsList.slice(0, maxZoom + 1);
  if (step >= 1) {
    maxZoom = 6;
    curresolutions = resolutionsList.slice(0, maxZoom + 1);
  } else if (step == 0.5) {
    maxZoom = 7;
    curresolutions = resolutionsList.slice(0, maxZoom + 1);
  }

  // 自定义tileGrid
  let extent = transformExtent(
    [data.lon[0], lats[0], data.lon[data.lon.length - 1], lats[lats.length - 1]],
    "EPSG:4326",
    "EPSG:3857"
  );
  const tileGrid = new TileGrid({
    // origin: [0, 0],
    minZoom: 0,
    // maxZoom: maxZoom,
    resolutions: curresolutions,
    maxResolution: 156543.0339,
    extent: extent,
    tileSize: [256, 256],
  });
  const source = new CustomImageTile(
    data.data,
    lats,
    data.lon,
    options,
    tileGrid,
    "EPSG:3857"
  );

  const layerOptions = {
    opacity: 0.8,
  };
  const layer = new TileLayer({
    source: source,
    ...layerOptions,
    opacity: 0.8,
  });

  layerManagerStore.addLayer("customImageTile", layer, layerOptions);
}

import { GirdValueWebGLTileLayer } from "@/components/OlMap/layers/GirdValueWebGLTileLayer.js";
import MapHoverTooltip from "@/utils/olMap/MapHoverTooltip.js";

const values = [
  -50,
  -40,
  -30,
  -20,
  -10,
  -5,
  -2,
  0,
  3,
  6,
  9,
  12,
  14,
  16,
  18,
  20,
  22,
  24,
  26,
  28,
  30,
  32,
  34,
  36,
  38,
  40,
  42,
  60,
];

const colors = [
  "rgb(3, 17, 126)",
  "rgb(5, 33, 176)",
  "rgb(4, 51, 203)",
  "rgb(0, 99, 252)",
  "rgb(0, 151, 252)",
  "rgb(48, 206, 253)",
  "rgb(153, 205, 253)",
  "rgb(142, 252, 249)",
  "rgb(191, 255, 218)",
  "rgb(156, 253, 158)",
  "rgb(203, 255, 155)",
  "rgb(155, 251, 3)",
  "rgb(1, 252, 149)",
  "rgb(0, 254, 104)",
  "rgb(253, 202, 199)",
  "rgb(255, 155, 205)",
  "rgb(254, 204, 155)",
  "rgb(253, 203, 104)",
  "rgb(254, 156, 7)",
  "rgb(255, 105, 102)",
  "rgb(234, 104, 20)",
  "rgb(255, 103, 102)",
  "rgb(255, 50, 99)",
  "rgb(253, 0, 3)",
  "rgb(205, 1, 50)",
  "rgb(201, 50, 205)",
  "rgb(204, 0, 151)",
  "rgb(150, 50, 100)",
];

async function addTileLayer() {
  const time = dayjs(timelineStore.current).format("YYYYMMDDHH");
  const dataModule = await import(
    /* @vite-ignore */
    `../../assets/fakeJson/customTile/${time}.json`
  );
  const data = dataModule.default.data;
  // delete dataModule.default.data;

  const layer = new GirdValueWebGLTileLayer(data, dataModule.default, values, colors);
  layer.setOpacity(0.8);

  layerManagerStore.addLayer("customTile", layer, {
    opacity: 0.8,
  });

  // // 辅助检查
  // let lats = dataModule.lat.map((item) => +item.toFixed(2));
  // const hoverTooltip = new MapHoverTooltip(
  //   olMap.getMap(), // OpenLayers map 实例
  //   layer, // 你的自定义图层
  //   data, // 原始数据
  //   lats, // 纬度数组
  //   dataModule.lon, // 经度数组
  //   values,
  //   colors // 颜色映射
  // );
}

async function addTileLayerAllWorld() {
  const dataModule = await import(
    /* @vite-ignore */
    `../../assets/fakeJson/customTile/temperature_2024081208_world.json`
  );
  const data = dataModule.default.data;

  const layer = new GirdValueWebGLTileLayer(
    data,
    {
      ...dataModule.default,
      debug: false,
    },
    values,
    colors
  );
  layer.setOpacity(0.8);

  layerManagerStore.addLayer("customTile(allWorld)", layer, {
    opacity: 0.8,
  });
}

async function addTileLayerQd() {
  const dataModule = await import(
    /* @vite-ignore */
    `../../assets/fakeJson/qd/base.json`
  );
  const data = dataModule.default.data;

  const layer = new GirdValueWebGLTileLayer(data, dataModule.default, values, colors);
  layer.setOpacity(0.8);

  layerManagerStore.addLayer("customTile(allWorld)", layer, {
    debug: false,
    opacity: 0.8,
  });
}

watch(
  () => timelineStore.current,
  async (val) => {
    const time = dayjs(val).format("YYYYMMDDHH");
    const dataModule = await import(
      /* @vite-ignore */
      `../../assets/fakeJson/customTile/${time}.json`
    );
    const data = dataModule.default.data;

    const layer = layerManagerStore.layerInstances.get("customTile");
    layer?.updateData(data);
  }
);

onMounted(() => {});
</script>

<style scoped lang="less">
.right-panel {
  height: calc(100vh - 4.375rem - 1.5rem);
  background-color: #ffffffcc;
  position: relative;
  margin: 0.75rem;

  &.collapsed {
    margin-right: 0;
  }
}

.panel-content {
  width: 24vw;
}

.toggle-icon {
  position: absolute;
  left: -1.6rem;
  top: 22rem;
  background-color: #ffffffcc;
  padding: 4px;
  border-radius: 4px 0 0 4px;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  z-index: 10;
  display: flex;
  padding: 0.7rem 0.3rem;
}
</style>
