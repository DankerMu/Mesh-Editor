import { Vector as VectorLayer } from 'ol/layer';
import { Vector as VectorSource } from 'ol/source';
import { Point } from 'ol/geom';
import { Feature } from 'ol';
import { Style, Circle, Fill, Stroke, Icon } from 'ol/style';
import { fromLonLat } from 'ol/proj';
import { Overlay } from 'ol';
// import currentStation from '@/assets/icon/current-station.png';
// import noStation from '@/assets/icon/nostation.png';
// import noModelStation from '@/assets/icon/nomodelstation.png';

const colorConfig = {
  0: '#FF312A',
  1: 'rgb(0, 153, 255)',
  2: 'rgb(255, 141, 0)'
}

class StationLayer extends VectorLayer {
  constructor(map, stations = [], type = 'station') {
    super({ source: new VectorSource(), zIndex: 5 });

    this.map = map
    this.type = type // station: 有站点, custom: 无站点
    this.lastStation = null
    this.selectStations = []
    this.mapMode = 0 // 0 单选，1 多选
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
      flag: data.flag
    });

    feature.setStyle(
      new Style({
        image: new Circle({
          radius: 5,
          fill: new Fill({ color: colorConfig[data.flag] }),
          stroke: new Stroke({ color: '#fff', width: 1 }),
        }),
      })
    );

    return feature;
  }

  addStations(stations) {
    stations.forEach((station) => {
      this.getSource().addFeature(this.createFeature(station));
    });
  }

  setMapMode(mode) {
    this.mapMode = mode;
  }

  setCanMapChoose(b) {
    this.canMapChoose = b
  }

  updateSource(name) {
    this.clearSingleStation()
    
    if (name) {
      const feature = this.getSource().getFeatures().find(f => f.get('stationIdD') === name);
      this.lastStation = feature
      feature && feature.setStyle(
        // new Style({
        //   image: new Icon({
        //     src: colorConfig[this.type].img, // 替换为你的图片路径
        //     scale: 0.9,
        //   }),
        // })
        new Style({
          image: new Circle({
            radius: 5,
            fill: new Fill({ color: colorConfig[feature.get('flag')] }),
            stroke: new Stroke({ color: 'black', width: 2 }),
          }),
        })
      );

      const geometry = feature.getGeometry()
      this.map.getView().fit(geometry, {
        maxZoom: 8
      })
    }

    // const gifUrl = '/image/sgif.gif';
    // const gif = gifler(gifUrl);
    // let that = this
    // gif.frames(
    //   document.createElement('canvas'),
    //   function (ctx, frame) {
    //     const feature = that.getSource().getFeatures().find(f => f.get('stationName') === name);
    //     that.lastStation = feature
    //     feature.setStyle(new Style({
    //       image: new Icon({
    //         img: ctx.canvas,
    //         scale: 0.4
    //       }),
    //     }),)
    //     ctx.clearRect(0, 0, frame.width, frame.height);
    //     ctx.drawImage(frame.buffer, frame.x, frame.y);
    //     that.map.render();
    //   },
    //   true,
    // );
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
  clearSingleStation() {
    this.lastStation && this.lastStation.setStyle(new Style({
      image: new Circle({
        radius: 5,
        fill: new Fill({ color: colorConfig[this.lastStation.get('flag')] }),
        stroke: new Stroke({ color: '#fff', width: 1 }),
      }),
    }));
    this.lastStation = null
  }

  // 清除多选站点的图片
  clearMultiStation() {
    this.selectStations.forEach(s => {
      s.setStyle(new Style({
        image: new Circle({
          radius: 5,
          fill: new Fill({ color: colorConfig[s.get('flag')] }),
          stroke: new Stroke({ color: '#fff', width: 1 }),
        }),
      }));
    })
    this.selectStations = []
  }

  onClick(event) {
    if (!this.canMapChoose) return
    const feature = this.map.getFeaturesAtPixel(event.pixel, { layerFilter: (layer) => layer === this })[0]
    if (feature) {
      if (this.mapMode === 0) {
        // 单选
        if (this.selectStations.length > 0) {
          this.clearMultiStation()
        }
        this.clearSingleStation()
        this.lastStation = feature
        $bus.emit('openTable', [feature]);
      } else {
        // 多选
        if (this.lastStation) {
          this.clearSingleStation()
        }
        this.selectStations.push(feature)
        $bus.emit('openTable', this.selectStations);
      }

      feature && feature.setStyle(
        // new Style({
        //   image: new Icon({
        //     src: colorConfig[this.type].img, // 替换为你的图片路径
        //     scale: 0.9,
        //   }),
        // })
        new Style({
          image: new Circle({
            radius: 5,
            fill: new Fill({ color: colorConfig[feature.get('flag')] }),
            stroke: new Stroke({ color: 'black', width: 2 }),
          }),
        })
      );
    }
  }

  onPointerMove(event) {
    const feature = this.map.forEachFeatureAtPixel(event.pixel, (f) => f, {
      layerFilter: (layer) => layer === this,
    });

    if (feature) {
      this.tooltipOverlay.getElement().innerHTML = feature.get('stationName') + feature.get('stationIdD');
      this.tooltipOverlay.setPosition(feature.getGeometry().getCoordinates());
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

export default StationLayer;
