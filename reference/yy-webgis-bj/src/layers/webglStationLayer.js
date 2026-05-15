import WebGLPointsLayer from 'ol/layer/WebGLPoints'
import WebGLVectorLayer from 'ol/layer/WebGLVector'
import { Vector as VectorSource } from 'ol/source'
import { Point } from 'ol/geom'
import { Feature } from 'ol'
import { fromLonLat } from 'ol/proj'
import { Overlay } from 'ol'
import Cluster from 'ol/source/Cluster'


class WebGLPointsStationLayer extends WebGLVectorLayer {
  constructor(map, stations = []) {
    super({
      zIndex: 5,
      style: {
        // 'icon-src': ['case', ['==', ['get', 'selected'], '1'], '/image/current-station.png', '/image/green-selected.png'],
        // 'icon-src': currentStation,
        // 'icon-rotate-with-view': false,

        'circle-radius': 5,
        'circle-fill-color': 'rgb(68, 91, 209)',
        'circle-stroke-color': ['case', ['==', ['get', 'selected'], '1'], 'red', 'white'],
        'circle-stroke-width': ['case', ['==', ['get', 'selected'], '1'], 2, 1],
      }
    })


    this.map = map
    this.lastStation = null
    this.selectStations = []
    this.canMapChoose = true
    this.tooltipOverlay = new Overlay({
      element: document.createElement('div'),
      positioning: 'top-center',
      offset: [10, 10],
    });
    this.tooltipOverlay.getElement().className = 'station-tooltip';
    this.map.addOverlay(this.tooltipOverlay);

    this.addStations(stations);
    this.bindEvents();
  }

  createFeature(data) {
    const feature = new Feature({
      geometry: new Point(fromLonLat([data.lon, data.lat])),
      stationName: data.stationName,
      stationIdC: data.stationIdC,
      stationIdD: data.stationIdD,
      selected: '0'
    });
    feature.set('selected', '0')

    return feature;
  }

  addStations(stations) {
    // stations.forEach((station) => {
    //   this.getSource().addFeature(this.createFeature(station));
    // });
    const source = new VectorSource({})

    const features = stations.map(s => this.createFeature(s))
    source.addFeatures(features)

    const clusterSource = new Cluster({
      distance: 10,
      source: source
    })
    clusterSource.on('addfeature', e => {
      const clusterFeature = e.feature
      let length = clusterFeature.get('features').length
      clusterFeature.set('count', length)
      if (length === 1) {
        let feature = clusterFeature.get('features')[0]
        clusterFeature.set('stationName', feature.get('stationName'))
        clusterFeature.set('stationIdC', feature.get('stationIdC'))
        clusterFeature.set('stationIdD', feature.get('stationIdD'))
        clusterFeature.set('selected', feature.get('selected'))
      } else {
        let stationIdDList = clusterFeature.get('features').map(item => item.get('stationIdD'))
        clusterFeature.set('stationIdD', stationIdDList)
        clusterFeature.set('selected', '')
      }
    })
    this.setSource(clusterSource)
  }

  setCanMapChoose(b) {
    this.canMapChoose = b
  }

  updateSource(id) {
    if (id) {
      const feature = this.getSource().getFeatures().find(f => f.get('stationIdD') === id);
      if (feature) {
        // 该点没有聚合
        const geometry = feature.getGeometry()
        this.map.getView().fit(geometry, {
          maxZoom: 12
        })
        // 已有的不重复选择
        if (this.selectStations.findIndex(x => x.get('stationIdD') === id) !== -1) return


        this.lastStation = feature
        this.selectStations.push(feature)
        feature.set('selected', '1')
        feature.get('features')[0].set('selected', '1')
        $bus.emit('openModelTable', this.selectStations)
        this.changed() // 触发重新渲染
      } else {
        // 该点在聚合图形里
        const clusterfeatures = this.getSource().getFeatures().find(f => {
          if (typeof f.get('stationIdD') === 'object') {
            if (f.get('stationIdD').includes(id)) return f
          }
        })
        let index = clusterfeatures.get('stationIdD').findIndex(x => x === id)
        let feature = clusterfeatures.get('features')[index]
        // 已有的不重复选择
        const geometry = feature.getGeometry()
        this.map.getView().fit(geometry, {
          maxZoom: 12
        })
        if (this.selectStations.findIndex(x => x.get('stationIdD') === id) !== -1) return

        this.lastStation = feature
        this.selectStations.push(feature)
        feature.set('selected', '1')
        $bus.emit('openModelTable', this.selectStations)
        this.changed() // 触发重新渲染
        return
      }
    }
  }

  bindEvents() {
    this.layerRemoveListener = () => {
      if (!this.map.getLayers().getArray().includes(this)) this.onRemove();
    };

    this.handleClick = this.onClick.bind(this);
    this.handlePointerMove = this.onPointerMove.bind(this);

    this.map.getLayers().on('remove', this.layerRemoveListener);
    this.map.on('click', this.handleClick);
    this.map.on('pointermove', this.handlePointerMove);
  }

  unbindEvents() {
    this.map.getLayers().un('remove', this.layerRemoveListener);
    this.map.un('click', this.handleClick);
    this.map.un('pointermove', this.handlePointerMove);
  }

  // 清除单选站点的图片
  // clearSingleStation() {
  //   if (this.lastStation) {
  //     this.lastStation.set('selected', '0')
  //     this.lastStation = null
  //     this.changed()
  //   }
  // }

  // 清除多选站点的图片
  clearMultiStation() {
    this.selectStations.forEach(f => f.get('features')[0].set('selected', '0'))
    this.selectStations = []
    this.changed()
  }

  onClick(event) {
    if (!this.canMapChoose) return
    const feature = this.map.getFeaturesAtPixel(event.pixel, { layerFilter: (layer) => layer === this })[0]
    if (feature) {
      // 多选
      let length = feature.get('count')
      if (length > 1) return

      const dupfeature = this.selectStations.find(x => x.get('stationIdD') === feature.get('stationIdD'))
      if (!dupfeature) {
        this.selectStations.push(feature)
        feature.set('selected', '1')
        feature.get('features')[0].set('selected', '1')
        $bus.emit('openModelTable', this.selectStations);
      } else {
        // 再次点击取消选中
        let index = this.selectStations.findIndex(x => x.get('stationIdD') === feature.get('stationIdD'))
        this.selectStations.splice(index, 1)
        feature.set('selected', '0')
        feature.get('features')[0].set('selected', '0')
        $bus.emit('openModelTable', this.selectStations);
      }

      this.changed()
    }
  }

  onPointerMove(event) {
    const feature = this.map.forEachFeatureAtPixel(event.pixel, (f) => f, {
      layerFilter: (layer) => layer === this,
    });

    if (feature) {
      let length = feature.get('count')
      if (length === 1) {
        this.tooltipOverlay.getElement().innerHTML = feature.get('stationName') + feature.get('stationIdD');
        this.tooltipOverlay.setPosition(feature.getGeometry().getCoordinates());
      } else {
        this.tooltipOverlay.getElement().innerHTML = '共' + length + '个站'
        this.tooltipOverlay.setPosition(feature.getGeometry().getCoordinates());
      }
    } else {
      this.tooltipOverlay.setPosition(undefined);
    }
  }

  onRemove() {
    this.lastStation = null
    this.unbindEvents();
    this.map.removeOverlay(this.tooltipOverlay);
  }

}

export default WebGLPointsStationLayer