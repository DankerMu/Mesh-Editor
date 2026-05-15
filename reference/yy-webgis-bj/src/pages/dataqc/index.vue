<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <div class="operate-container">
              <!-- <t-input v-model="useIpt" class="idipt" :placeholder="'请输入文件名'">
                <template #prefixIcon>
                  <img src="@/assets/icon/search-station.png" alt="" style="width: 24px;">
                </template>
</t-input> -->
              <t-select v-model="dataType" class="select" :options="dataTypeList"
                :popupProps="{ overlayClassName: 'pure-select-popup' }" placeholder="请选择数据类型">
              </t-select>
              <t-date-range-picker v-model="dateRangeIpt" class="dateRangePicker" clearable
                :popupProps="{ overlayClassName: 'yy-date-picker-popup' }" allow-input style="margin-left: 1.40625rem;">
                <template #suffixIcon>
                  <img src="@/assets/icon/calendar.png" alt="">
                </template>
              </t-date-range-picker>
              <div class="operation-list" style="margin-left: 2.25rem">
                <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="newsearch">
                  <img src="@/assets/icon/search.png" alt="">
                  查询
                </t-button>
                <t-button type="reset" class="reset" @click="reset">
                  <img src="@/assets/icon/reset.png" alt="">
                  重置
                </t-button>
              </div>
              <div class="operation-list" style="position: absolute;right: 0;">
                <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="downloadWord">
                  <!-- <HiIcon size="16px" :src="uploadSvg" style="color: #fff;margin-right: 4px;"></HiIcon> -->
                  <!-- <Download /> -->
                  模板下载
                </t-button>
                <t-button theme="primary" class="search" style="margin-right: 1.125rem;" @click="openUpload">
                  <HiIcon size="16px" :src="uploadSvg" style="color: #fff;margin-right: 4px;"></HiIcon>
                  批量上传
                </t-button>
                <t-button theme="primary" class="search" @click="download">
                  <HiIcon size="16px" :src="logoutSvg" style="color: #fff;margin-right: 4px;"></HiIcon>
                  导出
                </t-button>
              </div>
            </div>
            <t-table row-key="index" :data="tableData" class="data-table" :columns="columns" bordered
              :max-height="height" :header-affix-props="{ offsetTop: 0 }" :scroll="{ type: 'virtual' }"
              :loading="isLoading" lazy-load>
              <template #rate="{ row }">
                <span :class="{ warning: getWarning(row.rate) }">{{ row.rate }}</span>
              </template>
              <!-- <template #fileName="{ row }">
                <div>
                  <img :src="getFilethumbnail(row.fileName)" alt="">
                  {{ row.fileName }}
                </div>
              </template> -->
              <!-- <template #status="{ row }">
                <div class="flex align-center">
                  <div style="width: 6px;height: 6px;border-radius: 3px;margin-right: 6px;"
                    :style="{ 'background-color': getStatus(row.status)?.circleColor }">
                  </div>
                  <span :style="{ 'color': getStatus(row.status)?.color }">
                    {{ getStatus(row.status)?.label }}
                  </span>
                </div>
              </template> -->
              <!-- <template #operation="{ row }">
                <div class="table-btn">
                  <t-button variant="text" class="replace-item" style="margin-right: 12px;" @click="openUpload">
                    <HiIcon size="16px" :src="uploadSvg"></HiIcon>
                    上传
                  </t-button>
                  <t-button variant="text" class="delete-item" @click="deleteData(row)">
                    <HiIcon size="16px" :src="deleteSvg"></HiIcon>删除
                  </t-button>
                </div>
              </template> -->
            </t-table>
            <t-pagination v-model="pagination.current" class="table-pagination" :total="pagination.total"
              :page-size.sync="pagination.pageSize" @page-size-change="onPageSizeChange"
              @current-change="onCurrentChange" />
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>

  <!-- 删除框 -->
  <dialog-vue :visible="delVisible" header="确认删除" :customClass="'delete-dialog custom-dialog-footer'" :close="delClose">
    <template #content>
      <div class="warning-container">
        <img src="@/assets/img/delwarning.png" alt="">
        <div>
          <p class="bold">确定要删除选中的数据吗？</p>
          <p class="normal">删除后数据将无法恢复，请谨慎操作</p>
        </div>
      </div>
    </template>
    <template #button>
      <div class="btn-box">
        <t-button class="cancel-btn" @click="delClose">取消</t-button>
        <t-button class="delete-btn" @click="delConfirm">确认删除</t-button>
      </div>
    </template>
  </dialog-vue>

  <!-- 导入数据 -->
  <dialog-vue :visible="uploadVisible" header="导入数据" :customClass="'upload-dialog custom-dialog-footer'"
    :close="uploadClose">
    <template #content>

      <t-upload v-show="true" v-model="files" ref="uploadRef" class="upload" theme='custom' :draggable="true"
        :disabled="fileStatus !== 'wait'" :auto-upload="false" multiple
        accept="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel,text/csv"
        @validate="onValidate">

        <div v-show="files.length > 0 && (fileStatus === 'wait' || fileStatus === 'progress')" class="file-item"
          @click.stop="">
          <div class="container  flex align-center">
            <!-- <img src="@/assets/icon/uploading.png" alt=""> -->
            <!-- <div class="file-box">
              <div class="file-name">
                <span>已上传 {{ percent }} %</span>
              </div>
              <t-progress :percentage="percent" :label="false" size="small" :stroke-width="4" theme="line" />
            </div> -->
            <div style="height: 100%;overflow-y: auto;">
              <ul>
                <li v-for="(item, index) in files" :key="index">{{ item.name }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div v-show="fileStatus === 'wait' && files.length === 0">
          <div class="open">
            <img src="@/assets/icon/upload-prefix.png" alt="">
            拖入或者点击
            <span class="open-file">打开文件</span>
            夹导入
          </div>
          <p class="desc">上传文件格式仅支持.xlsx、.xls、.csv，且总文件大小不得超过500M</p>
        </div>

        <!-- <div v-else-if="files.length > 0">
          <div class="open">
            <img src="@/assets/icon/upload-prefix.png" alt="">
            再次导入文件则替换在上传文件
          </div>
          <p class="desc">上传文件格式仅支持txt，且文件大小不得超过10M</p>
        </div> -->

        <div v-show="fileStatus === 'error'" class="upload-failed">
          <img src="@/assets/img/upload-failed.png" alt="">
          <p class="open">数据导入失败</p>
          <p class="desc">请检查文件再次重新上传</p>
        </div>

        <div v-show="fileStatus === 'success'" class="upload-failed">
          <img src="@/assets/img/upload-success.png" alt="">
          <p class="open">数据导入成功</p>
          <p class="desc">成功导入{{ files.length }}个文件，点击完成或继续上传</p>
        </div>

      </t-upload>
    </template>

    <template #button>
      <div v-show="fileStatus === 'wait'" key="wait" class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="uploadClose" :disabled="fileStatus !== 'wait'">取消</t-button>
        <t-button class="confirm-btn" @click="uploadConfirm" :disabled="fileStatus !== 'wait'">确认导入</t-button>
      </div>
      <div v-show="fileStatus === 'error'" key="error" class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="uploadAgain">再次上传</t-button>
        <t-button class="confirm-btn" @click="uploadClose">完成</t-button>
      </div>
      <div v-show="fileStatus === 'success'" key="success" class="btn-box" style="border-top: 1px solid #1b2c7c;">
        <t-button class="cancel-btn" @click="uploadAgain">继续上传</t-button>
        <t-button class="confirm-btn" @click="uploadClose">完成</t-button>
      </div>
    </template>
  </dialog-vue>

  <t-loading :loading="isLoadingUpload" :fullscreen="true"></t-loading>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue"
import LayoutManager from "@/layouts/LayoutManager.vue"
import SearchVue from '@/components/SearchVue.vue'
import DialogVue from '@/components/DialogVue.vue'
import { DataService } from "@/api"
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo, UploadInstanceFunctions, UploadFile, UploadValidateType } from 'tdesign-vue-next'
import { DownloadIcon } from 'tdesign-icons-vue-next'
import type { DataType, DataTable } from '@/types/PagesType'
import dayjs from 'dayjs'
import { HiIcon } from "hoci";
import csvPng from '@/assets/icon/CSV.png'
import uploadSvg from '@/assets/icon/upload-btn.svg'
import logoutSvg from '@/assets/icon/logout.svg'

let height = ref(650 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 650 / window.devicePixelRatio
  })()
}

// let useIpt = ref('')
let dataType = ref('')
let dataTypeList = ref([])
let dateRangeIpt = ref([dayjs().subtract(3, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])

const isLoading = ref(false);
let isWarning = ref(false)
const tableData = ref<DataTable[]>([])
const columns = ref([
  { colKey: 'index', width: 70, title: '序号' },
  {
    colKey: 'dataSource',
    title: '数据类型',
  },
  {
    colKey: 'dataTime',
    title: '时间',
    width: 300,
    // className: 'fileName'
  },
  {
    colKey: 'total',
    title: '应到数量',
  },
  {
    colKey: 'arrived',
    title: '实到数量',
  },
  {
    colKey: 'unarrived',
    title: '缺失数量',
    // className: 'abnormalClass'
  },
  {
    colKey: 'rate',
    title: '到报率(%)',
  },
  // {
  //   colKey: 'status',
  //   title: '状态',
  // },
  // {
  //   colKey: 'operation',
  //   title: '操作',
  //   width: 200
  // },
]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const statusaList = [
  {
    value: '正常',
    label: '正常',
    circleColor: '#00B976',
    color: '#00D488'
  },
  {
    value: '异常',
    label: '异常',
    circleColor: '#F53F3F',
    color: '#FF4E34'
  },
  // {
  //   value: 'pending',
  //   label: '上传中',
  //   circleColor: '#F9AE00',
  //   color: '#F9AE00'
  // },
  // {
  //   value: 'wait',
  //   label: '未上传',
  //   circleColor: '#9CA6CE',
  //   color: '#E9ECFA'
  // },
]

let isLoadingUpload = ref(false)

const getDataType = async () => {
  try {
    const res = await DataService.getDataType({
      dataType: 'mde'
    })
    if (res) {
      dataTypeList.value = res.map((item: DataType) => {
        return {
          value: item.dataSource,
          label: item.dataSourceName
        }
      })
      dataType.value = dataTypeList.value[0].value
      search()
    }
  } catch (error) { }
}

// const getFilethumbnail = (fileName: string) => {
//   console.log(1);

//   let suffix = fileName.split('.')[1]
//   if (suffix) {
//     if (suffix === 'csv') {
//       return csvPng
//     }
//   } else {
//     return csvPng
//   }
// }
const getStatus = (status: string) => {
  return statusaList.find(x => x.value === status)
}

const getWarning = (val) => {
  let num = parseInt(val)
  if (val < 100) return true
  else return false
}

const checkParams = () => {
  if (!dataType.value) {
    MessagePlugin.warning('请选择数据类型')
    return false
  }
  if (dateRangeIpt.value.length === 0) {
    MessagePlugin.warning('请选择日期')
    return false
  }
  return true
}

const newsearch = async () => {
  pagination.current = 1;
  search();
}

const search = async () => {
  try {
    // if (!checkParams()) return
    isLoading.value = true;
    // pagination.current =1;
    let params = {
      dataSource: dataType.value,
      startTime: dayjs(dateRangeIpt.value[0]).format('YYYY-MM-DD 00:00:00'),
      endTime: dayjs(dateRangeIpt.value[1]).format('YYYY-MM-DD 23:59:00'),
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await DataService.getDataList(params)
    pagination.total = res.total || 0;
    if (res && res.data) {
      tableData.value = res.data.map((item: DataTable, index: number) => {
        return {
          ...item,
          index: index + 1
        }
      })
    }
  } catch (error) {
    tableData.value = []
    MessagePlugin.error('查询失败')
  } finally {
    isLoading.value = false;
  }
}

const onPageSizeChange = async (size: number, pageInfo: PageInfo) => {
  pagination.current = pageInfo.current;
  pagination.pageSize = size;
  await search()
};
const onCurrentChange = async (current: number) => {
  pagination.current = current;
  await search()
};

const reset = () => {
  // useIpt.value = ''
  dataType.value = dataTypeList.value[0].value
  dateRangeIpt.value = [dayjs().subtract(3, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  tableData.value = []
  pagination.current = 1
  pagination.pageSize = 10
  pagination.total = 0
  search()
}

let delVisible = ref(false)
let deleteDataTime: string = ''
let deleteDataType: string = ''
const deleteData = (row: DataTable) => {
  deleteDataTime = row.dataTime
  deleteDataType = row.dataSource ?? ''
  delVisible.value = true
}
const delClose = () => {
  deleteDataTime = ''
  deleteDataType = ''
  delVisible.value = false
}
const delConfirm = async () => {
  try {
    let params = {
      dataSource: deleteDataType,
      datatimes: [deleteDataTime]
    }
    await DataService.deleteFiles(params)
    await search()
    MessagePlugin.success('删除成功')
  } catch (error) {
    MessagePlugin.error('删除失败')
  } finally {
    delVisible.value = false
  }
}

let uploadVisible = ref(false)
let uploadRef = ref<UploadInstanceFunctions>()
let files = ref<UploadFile[]>([])
let percent = ref(0)
let fileStatus = ref('wait')
let fileStatusList = reactive(['wait', 'success', 'error', 'progress'])
const openUpload = () => {
  uploadVisible.value = true
}
const uploadClose = () => {
  files.value = []
  percent.value = 0
  fileStatus.value = 'wait'
  uploadVisible.value = false
}
const uploadConfirm = () => {
  if (files.value.length === 0) {
    MessagePlugin.error('请选择文件')
    return
  }

  upload(files.value)
}

const triggerUpload = () => {
  uploadRef.value?.triggerUpload?.()
}

const uploadAgain = () => {
  files.value = []
  percent.value = 0
  fileStatus.value = 'wait'
}

let interval: ReturnType<typeof setInterval> | undefined
const upload = async (files: UploadFile[]) => {
  try {
    isLoadingUpload.value = true
    // debugger
    const formdata = new FormData()
    files.forEach(file => {
      if (file.raw instanceof File) {
        formdata.append('files', file.raw);
      }
    })
    let totalSize = 0
    files.forEach(f => {
      totalSize = totalSize + f.size
    })

    // 文件大小限制
    if ((totalSize / 1024 / 1024) > 500) {
      MessagePlugin.warning('文件总大小不超过500M，请重新上传')
      isLoadingUpload.value = false
      uploadClose()
      return
    }

    let isFetching = false
    interval = setInterval(() => {
      if (isFetching) return

      if (percent.value < 100) {
        isFetching = true
        DataService.uploadStatus().then(res => {
          console.log('状态', res);
          percent.value = res;
          if (percent.value === 100) {
            console.log('清除定时器');

            fileStatus.value = 'success'
            clearInterval(interval);
            isLoadingUpload.value = false
          }
        }).finally(() => {
          isFetching = false
        });
      }
    }, 100)

    DataService.uploadFiles(formdata).then(res => {
      if (res.code === 200) {
        if (interval) {
          percent.value = 100
          isFetching = true
          fileStatus.value = 'success'
          clearInterval(interval);
          isLoadingUpload.value = false
        }
      }
      if (res.code === -1) {
        isLoadingUpload.value = false
        if (interval) {
          fileStatus.value = 'error'; // 出错时设置状态为 error
          clearInterval(interval); // 清除定时器
          MessagePlugin.error(res.msg)
        }
      }
      // if (!res.data) {
      //   isLoadingUpload.value = false
      //   throw new Error('上传失败');
      //   if (interval) {
      //     fileStatus.value = 'error'
      //     clearInterval(interval);
      //   }
      // }
    }).catch(() => {
      isLoadingUpload.value = false
      if (interval) {
        fileStatus.value = 'error'; // 出错时设置状态为 error
        clearInterval(interval); // 清除定时器
      }
    })
  } catch (error) {
    isLoadingUpload.value = false
    if (interval) {
      fileStatus.value = 'error'; // 出错时设置状态为 error
      clearInterval(interval); // 清除定时器
    }
  }
}

const onValidate = (params: { type: UploadValidateType, files: UploadFile[] }) => {
  const fileList = params.files
  const type = params.type
  const messageMap = {
    FILE_OVER_SIZE_LIMIT: '文件大小超出限制，已自动过滤',
    FILES_OVER_LENGTH_LIMIT: '文件数量超出限制，仅上传未超出数量的文件',
    FILTER_FILE_SAME_NAME: '不允许上传同名文件',
    BEFORE_ALL_FILES_UPLOAD: 'beforeAllFilesUpload 方法拦截了文件',
    CUSTOM_BEFORE_UPLOAD: 'beforeUpload 方法拦截了文件',
  };

  messageMap[type] && MessagePlugin.warning(messageMap[type]);
  if (files.value.length > 0) {
    fileStatus.value = 'progress'
  }
}

// 导出
const download = async () => {
  try {
    const res = await DataService.exportData({
      dataSource: dataType.value,
      startTime: dayjs(dateRangeIpt.value[0]).format('YYYY-MM-DD 00:00:00'),
      endTime: dayjs(dateRangeIpt.value[1]).format('YYYY-MM-DD 23:59:00'),
    })
    let name = dataTypeList.value.find(x => x.value === dataType.value)?.label || dataType.value

    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    let url = (window.URL || window.webkitURL).createObjectURL(blob)
    let link = document.createElement('a')
    link.style.display = 'none'
    link.href = url
    link.setAttribute('download', `${dayjs(dateRangeIpt.value[0]).format('YYYY-MM-DD')}至${dayjs(dateRangeIpt.value[1]).format('YYYY-MM-DD')} ${name}`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url);

  } catch (error) {

  }
}

// 模板下载 .xls
const downloadWord = async () => {
  try {
    const res = await DataService.downloadTemplate()
    const blob = new Blob([res], { type: 'application/vnd.ms-excel' })
    let url = (window.URL || window.webkitURL).createObjectURL(blob)
    let link = document.createElement('a')
    link.style.display = 'none'
    link.href = url
    link.setAttribute('download', '模板')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url);

  } catch (error) { }
}


getDataType()


onBeforeUnmount(() => {
  tableData.value = []
})

</script>

<style lang="less" scoped>
.content {
  height: 100%;
  position: relative;
  z-index: 2;
  padding: var(--app-view-padding);
}

.operate-container {
  position: relative;

  :deep(.idipt) {
    width: 26.25rem;

    .t-input {
      width: 26.25rem;
      height: 3.75rem;
    }
  }

  :deep(.select) {
    width: 205px;
    height: 40px;

    .t-input {
      height: 40px;
      background-color: var(--app-ui-bg-color);
      border-radius: 0.375rem;

      .t-fake-arrow {
        color: var(--app-text-color-purple);
      }
    }
  }

  :deep(.dateRangePicker) {
    .t-range-input {
      width: 280px;
      height: 40px;
    }
  }
}

:deep(.t-table) {
  // .t-table__header--fixed:not(.t-table__header--multiple)>tr>th {
  //   background-color: transparent !important;
  // }

  // .t-table__scroll-bar-divider {
  //   border: none;
  // }

  .fileName {
    >div {
      display: flex;
      align-items: center;
      height: 32px;

      >img {
        margin-right: 8px;
      }
    }
  }
}

:deep(.t-table--bordered) {
  // th {
  //   border-left: 1px solid var(--td-border-color) !important;
  // }

  .t-table__body {
    td {
      vertical-align: middle;
    }

    .abnormalClass {
      color: var(--app-text-color-error) !important;
    }
  }
}

// 表体滚动轴隐藏
:deep(.t-table__content::-webkit-scrollbar) {
  width: 0;
}

.warning {
  color: red;
}
</style>
