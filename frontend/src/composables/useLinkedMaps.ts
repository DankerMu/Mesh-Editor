import type Map from 'ol/Map'
import type { EventsKey } from 'ol/events'
import { unByKey } from 'ol/Observable'

type ViewEventName = 'change:center' | 'change:resolution' | 'change:rotation'
type RegisteredMap = {
  map: Map
  keys: EventsKey[]
}

const VIEW_EVENTS: ViewEventName[] = ['change:center', 'change:resolution', 'change:rotation']

export function useLinkedMaps() {
  const registeredMaps: RegisteredMap[] = []
  let syncing = false

  function syncFrom(sourceMap: Map): void {
    if (syncing) {
      return
    }

    syncing = true
    try {
      const sourceView = sourceMap.getView()
      const center = sourceView.getCenter()
      const resolution = sourceView.getResolution()
      const rotation = sourceView.getRotation()

      for (const item of registeredMaps) {
        if (item.map === sourceMap) {
          continue
        }

        const targetView = item.map.getView()
        targetView.setCenter(center)
        targetView.setResolution(resolution)
        targetView.setRotation(rotation)
      }
    } finally {
      syncing = false
    }
  }

  function registerMap(map: Map): void {
    if (registeredMaps.some((item) => item.map === map)) {
      return
    }

    const view = map.getView()
    const keys = VIEW_EVENTS.map((eventName) => view.on(eventName, () => syncFrom(map)))
    registeredMaps.push({ map, keys })

    if (registeredMaps.length > 1) {
      syncFrom(registeredMaps[0].map)
    }
  }

  function cleanup(): void {
    for (const item of registeredMaps) {
      unByKey(item.keys)
    }
    registeredMaps.length = 0
  }

  return {
    registerMap,
    cleanup,
  }
}
