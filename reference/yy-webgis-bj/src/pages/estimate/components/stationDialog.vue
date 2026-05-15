<template>
  <dialog-vue :visible="stationVisible" header="站点选择" :customClass="'station-dialog custom-dialog-footer'"
    :close="close">
    <template #content>
      <t-radio-group v-model="mode" class="modelist">
        <t-radio value="station"><span style="margin-right: 8px;">站点</span>
          <t-auto-complete v-model="keyword" clearable class="tac" :options="filterOptions" placeholder="请输入站点名称或编号"
            :popupProps="{ overlayClassName: 't-demo-autocomplete-option-list', visible: showDropdown }"
            @change="handleInput" @select="handleChange" @clear="handleClear">
            <template #prefixIcon>
              <img src="@/assets/icon/search-station.png" alt="" style="width: 24px;">
            </template>
            <template #option="{ option }">
              {{ option.label }}
            </template>
          </t-auto-complete>
        </t-radio>
        <t-radio value="rect"><span style="margin-right: 8px;">区域</span>
          <t-select v-model="selectedArea" :options="areaList" class="tac mr-18" style="width: 200px;"
            :popupProps="{ overlayClassName: 'pure-select-popup' }" valueType="object"></t-select>
        </t-radio>
      </t-radio-group>

      <div style="overflow-y: auto;flex: 1;">
        <t-radio-group v-model="selStation" @change="updateValue" class="radiolist" :disabled="mode === 'rect'">
          <t-radio v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</t-radio>
        </t-radio-group>
      </div>

      <!-- <t-pagination v-model="current" v-model:pageSize="pageSize" :total="total" @change="onPageChange"></t-pagination> -->
    </template>
    <template #button>
      <div class="btn-box" style="margin-top: 0;">
        <t-button class="cancel-btn" @click="newClose">取消</t-button>
        <t-button class="confirm-btn" style="margin-right: 15px;" @click="confirm">确定</t-button>
        <!-- <t-button class="confirm-btn" @click="confirmAll">选择全部</t-button> -->
      </div>
    </template>
  </dialog-vue>
</template>

<script setup>
import DialogVue from '@/components/DialogVue.vue'
import { DisplayService } from '@/api'
import { EstimateService } from '@/api'
import { string } from 'three/tsl';

const emit = defineEmits(["update:selectedStation", "update:selectedArea"]);

let stationVisible = ref(false)

let mode = ref('rect')
let selectedArea = ref('')
const areaList = ref([])

const getZone = async () => {
  try {
    const res = await EstimateService.getZone()
    if (res && res.length > 0) {
      areaList.value = res.map(item => {
        return {
          value: item.id.toString(),
          label: item.dataZone
        }
      })
      selectedArea.value = areaList.value[0]
      emit("update:selectedArea", selectedArea.value)
    } else {
      areaList.value = []
    }
  } catch (error) {
    areaList.value = []
  }
}
getZone()

let selStation = ref('')

const open = () => {
  stationVisible.value = true
}
const close = () => {
  stationVisible.value = false
  showDropdown.value = false
}

const updateValue = (value) => {
  selStation.value = value;
};

const newClose = () => {
  stationVisible.value = false
  showDropdown.value = false
}

const confirm = () => {
  if (mode.value === 'station') {
    let label = options.value.filter(x => x.value === selStation.value)?.[0].label
    if (label) {
      emit("update:selectedStation", {
        label,
        value: selStation.value
      });
      emit("update:selectedArea", '');
    } else {
      emit("update:selectedStation", {
        label: '',
        value: ''
      });
      emit("update:selectedArea", '');
    }
  } else {
    emit("update:selectedArea", selectedArea.value);
    emit("update:selectedStation", '');
  }
  stationVisible.value = false
  showDropdown.value = false
}

const clear = () => {
  selStation.value = ''
  selectedArea.value = areaList.value[0]
  emit("update:selectedArea", selectedArea.value);
  emit("update:selectedStation", '');
  mode.value = 'rect'
}

// const confirmAll = () => {
//   emit("update:selectedStation", allOptions.map(item => item.stationIdD).join(','));
//   stationVisible.value = false
//   showDropdown.value = false
// }

const options = ref([])
let allOptions = []
const getAllStations = async () => {
  const res = await DisplayService.getStations()

  if (res && res.length > 0) {
    allOptions = res.map(item => {
      let name = item.stationName
      if (!item.stationName || item.stationName == " ") name = item.stationIdD
      return {
        value: item.stationIdD,
        label: name,
        ...item
      }
    })
    // options.value = allOptions.slice(0, 20)
    options.value = allOptions
  }
}

const keyword = ref("");
let filterOptions = ref([])

const showDropdown = ref(false);

const handleInput = async (val) => {
  if (val.length > 0) {
    showDropdown.value = true
    try {
      const res = await DisplayService.SearchStation({
        stationName: val
      })
      if (res.length > 0) {
        filterOptions.value = res.map(item => {
          let label = item.stationName + item.stationIdD
          if (!item.stationName || item.stationName == ' ') {
            label = item.stationIdD.toString()
          }
          return {
            label: label,
            ...item
          }
        })
      }
    } catch (error) { }
  } else {
    showDropdown.value = false
    options.value = allOptions
  }
};

const handleChange = (val) => {
  let result = filterOptions.value.find(x => x.label === val)
  let selectStation = allOptions.find(item => item.stationIdD === result.stationIdD)
  if (selectStation) {
    // options.value = []
    options.value = [selectStation]
  }

  showDropdown.value = false
}

const handleClear = () => {
  options.value = allOptions
}

let current = ref(0)
let pageSize = ref(50)
let total = ref(1000)
const onPageChange = () => {

}

getAllStations()

defineExpose({
  open,
  clear
})

</script>

<style lang="less">
.station-dialog {
  width: 800px;
  height: 500px;

  .t-dialog__body {
    display: flex;
    flex-direction: column;
    height: calc(100% - 53px);

    ul {
      height: 100%;
      overflow-y: auto;
    }
  }

  .radiolist {
    width: 100%;

    .t-radio {
      width: 21%;
      margin-right: 16px;

      .t-radio__label {
        width: 80%;
        color: var(--app-text-color-purple);
        white-space: pre-line;
      }
    }
  }

  .modelist {
    width: 100%;
    padding-bottom: 10px;
    margin-bottom: 10px;
    border-bottom: 1px solid var(--app-text-color-purple);

    .t-radio {
      display: flex;
      align-items: center;
      width: 41%;
      margin-right: 16px;

      .t-radio__label {
        display: flex;
        align-items: center;
        width: 80%;
        color: var(--app-text-color-purple);
        white-space: nowraps;
      }
    }
  }

  .tac {
    width: 223px;
    height: 38px;

    .t-input {
      width: 223px;
      height: 38px;
      margin-bottom: 8px;
      border-radius: 8px;
      border: 1px solid rgba(71, 80, 126, 0.9);
      background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
    }

    .t-select-input {
      .t-input {
        width: 223px;
        height: 38px;
        margin-bottom: 8px;
        border-radius: 8px;
        border: 1px solid rgba(71, 80, 126, 0.9) !important;
        background: linear-gradient(225deg, rgba(27, 44, 124, 0.98) 0%, rgba(21, 31, 96, 0.98) 100%);
      }
    }
  }
}

.noti-btn {
  color: #fff;
  cursor: pointer;

  &:hover {
    color: var(--app-text-color-normal);
  }
}
</style>