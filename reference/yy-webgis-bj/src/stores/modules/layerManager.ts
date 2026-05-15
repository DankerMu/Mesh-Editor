import { defineStore } from "pinia"

interface LayerOptions{
  visible: boolean
  zIndex: number
  opacity: number
}

export const useLayerManagerStore = defineStore("layerManager", () => {
  const layerInstances = new Map<string, any>()
  const layerStates = reactive<Record<string, LayerOptions>>({})

  function addLayer(id: string, layerInstance: any, opts?: Partial<LayerOptions>){
    // TODO: 
    const isExist = layerInstances.has(id)
    if(isExist){
      console.warn(`Layer with id ${id} already exists`)
      return
    }
    
    olMap?.addLayer(layerInstance)
    
    layerInstances.set(id, layerInstance)
    layerStates[id] = {
      visible: true,
      zIndex: 0,
      opacity: 1,
      ...opts
    }
  }

  function removeLayerById(id: string){
    const layer = layerInstances.get(id)
    olMap?.removeLayer(layer)

    layerInstances.delete(id)
    delete layerStates[id]
  }

  function removeAllLayers(){
    layerInstances.forEach((layer: any) => {
      olMap?.removeLayer(layer)
    })

    layerInstances.clear()
    Object.keys(layerStates).forEach((id: string) => {
      delete layerStates[id]
    })
  }

  /**
   * 设置layer的可见性
   * @param ids 传空数组表示setAll
   * @param visible 
   */
  function setVisible(ids: string | string[], visible: boolean){
    if (Array.isArray(ids)) {
      if(ids.length === 0){
        Object.keys(layerStates).forEach((id: string) => {
          _setVisibleById(id, visible)
        })
      }else{
        ids.forEach(id => {
          _setVisibleById(id, visible)
        });
      }
    }else{
      _setVisibleById(ids, visible)
    }
  }

  function _setVisibleById(id: string, visible: boolean){
    const layer = layerInstances.get(id)
    if(layer){
      layer.setVisible(visible)

      layerStates[id].visible = visible
    }else{
      console.warn(`Layer with id ${id} not found`)
    }
  }

  function setOpacity(id: string, opacity: number){
    const layer = layerInstances.get(id)
    layer.setOpacity(opacity)
  }

  return {
    layerInstances,
    layerStates,
    addLayer,
    removeLayerById,
    removeAllLayers,
    setVisible,
    setOpacity
  }
})
