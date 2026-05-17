import { defineStore } from 'pinia'
import {
  getVersionDetail,
  getVersions,
  releaseVersion as releaseVersionApi,
  reviewVersion as reviewVersionApi,
  saveVersion as saveVersionApi,
  submitVersion as submitVersionApi,
} from '@/api/version'
import type {
  ReviewAction,
  VersionDetail,
  VersionListItem,
  VersionListParams,
  VersionSaveResponse,
} from '@/api/version'

export interface VersionFilters {
  status?: string
  windowId?: string
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '版本操作失败'
}

function toApiFilters(filters: VersionFilters): VersionListParams {
  return {
    status: filters.status || undefined,
    window_id: filters.windowId || undefined,
  }
}

export const useVersionStore = defineStore('version', {
  state: () => ({
    versions: [] as VersionListItem[],
    currentVersion: null as VersionDetail | null,
    filters: {
      status: undefined,
      windowId: undefined,
    } as VersionFilters,
    loading: false,
    error: null as string | null,
    lastSavedVersion: null as VersionSaveResponse | null,
  }),
  actions: {
    async fetchVersions(filters?: VersionFilters) {
      if (filters) {
        this.filters = { ...this.filters, ...filters }
      }

      this.loading = true
      this.error = null

      try {
        this.versions = await getVersions(toApiFilters(this.filters))
        return this.versions
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchVersionDetail(versionId: string) {
      this.loading = true
      this.error = null

      try {
        this.currentVersion = await getVersionDetail(versionId)
        return this.currentVersion
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async saveVersion(sessionId: string) {
      this.loading = true
      this.error = null

      try {
        const response = await saveVersionApi(sessionId)
        this.lastSavedVersion = response
        await this.fetchVersions()
        return response
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async submitVersion(versionId: string) {
      this.loading = true
      this.error = null

      try {
        const response = await submitVersionApi(versionId)
        await this.fetchVersions()
        return response
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async reviewVersion(versionId: string, action: ReviewAction, comment?: string) {
      this.loading = true
      this.error = null

      try {
        const response = await reviewVersionApi(versionId, action, comment)
        await this.fetchVersions()
        if (this.currentVersion?.version_id === versionId) {
          await this.fetchVersionDetail(versionId)
        }
        return response
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
    async releaseVersion(versionId: string) {
      this.loading = true
      this.error = null

      try {
        const response = await releaseVersionApi(versionId)
        await this.fetchVersions()
        if (this.currentVersion?.version_id === versionId) {
          await this.fetchVersionDetail(versionId)
        }
        return response
      } catch (error) {
        this.error = getErrorMessage(error)
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
