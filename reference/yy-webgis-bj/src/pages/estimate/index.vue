<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <!-- <div style="padding: 20px; font-size: 18px;">
            预报质量评估分析

            <t-time-picker format="HH" :disable-time="disableTime" default-value="20:00:00" />
            <t-date-picker :popupProps="{ overlayClassName: 'yy' }" format="YYYY-MM-DD HH时" enable-time-picker
              v-model="timeValue" allow-input clearable :timePickerProps="{
                format: 'HH时',
                defaultValue: '08时',
                disableTime: (h) => {
                  return {
                    hour: [0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23],
                    minute: range,
                    second: range
                  };
                }
              }" />
          </div> -->
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <t-tabs v-model="value" class="estimate-tabs">
              <t-tab-panel value="first" label="站点预报检验" :destroyOnHide="false">
                <StationPanel></StationPanel>
              </t-tab-panel>
              <t-tab-panel value="second" label="格点预报检验" :destroyOnHide="false">
                <GridPanel></GridPanel>
              </t-tab-panel>
            </t-tabs>
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import LayoutManager from "@/layouts/LayoutManager.vue"
import StationPanel from './components/StationPanel.vue'
import GridPanel from './components/GridPanel.vue'

// const disableTime = (h) => {
//   return {
//     hour: [0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23],
//   };
// };

const timeValue = ref('2025-03-13 20时')

const range = [...Array(59).keys()].map(i => i + 1);



let value = ref('first')

let selectedStation = ref('')
const stationList = ref([
  {
    label: '选项一',
    value: '1',
    children: [
      {
        label: '子选项一',
        value: '1.1',
      },
      {
        label: '子选项二',
        value: '1.2',
      },
      {
        label: '子选项三',
        value: '1.3',
      },
    ],
  },
  {
    label: '选项二',
    value: '2',
    children: [
      {
        label: '子选项一',
        value: '2.1',
      },
      {
        label: '子选项二',
        value: '2.2',
      },
    ],
  },
])

const options = [
  {
    value: 'optionA',
    label: '选项一',
  },
  {
    value: 'optionB',
    label: '选项二',
  },
  {
    value: 'optionC',
    label: '选项三',
  },
];

const timeList = [
  {
    value: '72',
    label: '0-72h',
  },
  {
    value: '240',
    label: '72-240h',
  },
];

const checkList = [
  {
    value: 'pre',
    label: '降水量',
  },
];

const checkMethod = [
  {
    value: 'mae',
    label: 'MAE',
  },
  {
    value: 'rmse',
    label: 'EMSE',
  },
  {
    value: 'corr',
    label: 'CORR',
  },
];

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 2;
  padding: var(--app-view-padding);
}

:deep(.t-tabs) {
  .t-tabs__nav-scroll {
    height: 5.0625rem;
  }
}

:deep(.t-tabs__nav-item-text-wrapper) {
  font-size: 1.5rem;
}

:deep(.t-date-range-picker) {
  width: 300px;

  .t-range-input {
    height: 38px;
    background-color: var(--app-border-color-dark);
    border-color: var(--app-ui-bg-color);

    .t-input {
      background: var(--app-border-color-dark);

      .t-input__inner {
        font-family: 'PingFang SC-Bold';
      }
    }
  }
}

:deep(.time-sel) {

  .t-input__wrap {
    // width: 100px;
  }

  .t-input {
    // width: 100px;
    height: 38px;
    background-color: transparent;
    border-radius: 0.375rem;
    border: 1px solid #2E3C8A;

    .t-fake-arrow {
      color: var(--app-text-color-purple);
    }
  }
}

:deep(.t-cascader) {
  width: 18rem;
  height: 38px;

  .t-input {
    height: 38px;

    .t-input__prefix {
      .t-tag {
        background: #2741A8;
        color: #fff;
      }
    }
  }

  .t-input--focused {
    box-shadow: none;
  }
}

:deep(.station-sel) {
  width: 18rem;
  height: 38px;

  .t-input {
    height: 38px;

    .t-input__prefix {
      .t-tag {
        background: #2741A8;
        color: #fff;
      }
    }
  }
}

:deep(.t-select-input) {
  .t-input {
    border-color: var(--app-ui-bg-color) !important;
    background: var(--app-border-color-dark);
  }
}

.operation-list {
  padding-left: 7.6875rem;
}

:deep(.divider) {
  border-top: 1px solid #263D98;
  margin: 1.3125rem 0;
}

.default-content {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;

  div {
    font-size: 1.5rem;
  }

  img {
    width: 17.625rem;
    height: 12.75rem;
  }
}
</style>
