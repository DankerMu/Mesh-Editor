<template>
  <div class="h-full">
    <LayoutManager>
      <template #main>
        <div class="content layout-bg">
          <div class="h-full content-bg pl-9 pr-9 pb-9">
            <div class="operate-container">
              <SearchUser v-model:useIdIpt="useIdIpt"></SearchUser>
              <t-date-range-picker v-model="dateRangeIpt" :popupProps="{ overlayClassName: 'yy-date-picker-popup' }"
                clearable allow-input style="margin-left: 1.40625rem;">
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

            </div>
            <t-table row-key="index" :data="tableData" :columns="columns" bordered :max-height="height"
              :header-affix-props="{ offsetTop: 0 }" :scroll="{ type: 'virtual' }" :loading="isLoading"
              lazy-load></t-table>
            <t-pagination v-model="pagination.current" class="table-pagination" :total="pagination.total"
              :page-size.sync="pagination.pageSize" @page-size-change="onPageSizeChange"
              @current-change="onCurrentChange" />
            <!-- <div class="mt-5" style="color: #B9C5FF;font-size: 1.3125rem;">显示第1到第6条记录，总共 6 条记录</div> -->
          </div>
        </div>
      </template>
    </LayoutManager>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue"
import LayoutManager from "@/layouts/LayoutManager.vue"
import SearchUser from './SearchUser.vue'
import type { LogRecords } from '@/types/PagesType'
import { LogService } from "@/api"
import { MessagePlugin } from 'tdesign-vue-next'
import type { PageInfo } from 'tdesign-vue-next'
import dayjs from 'dayjs'

let height = ref(650 / window.devicePixelRatio)
window.onresize = () => {
  return (() => {
    height.value = 650 / window.devicePixelRatio
  })()
}

let useIdIpt = ref('')
let dateRangeIpt = ref([dayjs().subtract(3, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const isLoading = ref(false);
const tableData = ref<LogRecords[]>([])
const columns = ref([
  {
    colKey: 'index',
    title: '序号',
    width: 80,
    className: () => "first-col"
  },
  {
    colKey: 'username',
    title: '操作者',
    className: () => "second-col"

  },
  {
    colKey: 'optiontype',
    title: '操作类型',
    className: () => "third-col"

  },
  {
    colKey: 'optioncontent',
    title: '操作内容',
    className: () => "fourth-col-col"

  },
  {
    colKey: 'optiontime',
    title: '操作时间',
  },
]);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
});
const newsearch = ()=>{
  pagination.current = 1
  search()
}
const search = async () => {
  try {
    isLoading.value = true;
    let params = {
      userName: useIdIpt.value,
      startTime: dayjs(dateRangeIpt.value[0]).format('YYYY-MM-DD 00:00:00'),
      endTime: dayjs(dateRangeIpt.value[1]).format('YYYY-MM-DD 23:59:00'),
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await LogService.getLog(params)
    pagination.total = res.total || 0;
    if (res && res.data) {
      tableData.value = res.data.map((item: LogRecords, index: number) => {
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
  useIdIpt.value = ''
  dateRangeIpt.value = [dayjs().subtract(3, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
  tableData.value = []
  pagination.current = 1
  pagination.pageSize = 10
  pagination.total = 0
  search()
}

search()

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
  display: flex;
  align-items: center;
  padding-top: 2.25rem;
  padding-bottom: 1.5rem;

  :deep(.idipt) {
    width: 26.25rem;

    .t-input {
      width: 26.25rem;
      height: 3.75rem;
      border-color: #2E3C8A !important;
    }

    .t-input--focused {
      box-shadow: none;
    }
  }

  :deep(.t-date-range-picker) {

    .t-range-input-popup--visible .t-range-input {
      box-shadow: none;
    }

    .t-range-input {
      width: 26.25rem;
      height: 3.75rem;
      border-color: #2E3C8A !important;

      .t-input__wrap {
        .t-input {
          background-color: transparent;
        }
      }
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

  .first-col {
    width: 33.45rem;
  }

  .second-col {
    width: 32.8rem;
  }

  .third-col {
    width: 37.3rem;
  }

  .fourth-col {
    width: 50.8rem;
  }
}

// :deep(.t-table--bordered) {
//   th {
//     border-left: 1px solid var(--td-border-color) !important;
//   }
// }

// 表体滚动轴隐藏
:deep(.t-table__content::-webkit-scrollbar) {
  width: 0;
}
</style>
