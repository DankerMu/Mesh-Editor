import { defineStore } from "pinia"
import { reactive } from "vue"

export const useMapStore = defineStore("map", () => {
    const mapPanelOptions = reactive({
      rightPanel: false,
      leftPanel: false,
      timeline: false,
      mapTools: false
    }) as { [key: string]: boolean }

    function setAllPanels(value: boolean) {
      Object.keys(mapPanelOptions).forEach(key => {
        mapPanelOptions[key as keyof typeof mapPanelOptions] = value
      })
    }

    function setPanelVisibility(panelKey: keyof typeof mapPanelOptions , isVisible: boolean) {
      if (panelKey in mapPanelOptions) {
        mapPanelOptions[panelKey] = isVisible
      }
    }

    function togglePanel(panel: keyof typeof mapPanelOptions) {
      mapPanelOptions[panel] = !mapPanelOptions[panel]
    }


    function initCurComponentMapOptions(options: Partial<typeof mapPanelOptions> = {}) {
      Object.assign(mapPanelOptions, Object.keys(mapPanelOptions).reduce((acc: typeof mapPanelOptions, key) => {
        acc[key] = false;
        return acc;
      }, {}), options);
    }

    return {
      mapPanelOptions,
      setAllPanels,
      togglePanel,
      setPanelVisibility,
      initCurComponentMapOptions
    }
})
