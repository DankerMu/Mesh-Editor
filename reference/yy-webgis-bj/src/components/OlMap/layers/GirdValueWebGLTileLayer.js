import WebGLTileLayer from "ol/layer/WebGLTile";
import DataTileSource from "ol/source/DataTile";
import { get as getProjection, transform } from "ol/proj";
import TileGrid from "ol/tilegrid/TileGrid";

export class GirdValueWebGLTileLayer extends WebGLTileLayer {
  constructor(data, options, values, colors) {
    super({
      source: null,
      opacity: 0.8,
      style: {
        color: [
          "color",
          ["*", ["band", 1], 255],
          ["*", ["band", 2], 255],
          ["*", ["band", 3], 255],
          ["*", ["band", 4], 1],
        ],
      },
    });

    this.tileCache = new Map();
    this.data = data;
    this.options = options;
    this.values = values;

    this.rows = data.length;
    this.cols = data[0].length;

    this.colors = this.parseColors(colors);

    const source = this.createSource();
    this.set("source", source);

    this.debugMode = options.debug || false;
    if (this.debugMode) {
      this.createDebugLayer();
      window.colors = this.colors;
      window.values = this.values;
      window.tileCache = this.tileCache;
    }
  }

  parseColors(colors) {
    return colors.map((rgbStr) => {
      const matches = rgbStr.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
      if (!matches) {
        console.error("Invalid RGB string format:", rgbStr);
        return { r: 0, g: 0, b: 0, a: 0 };
      }
      return {
        r: parseInt(matches[1]),
        g: parseInt(matches[2]),
        b: parseInt(matches[3]),
        a: 255,
      };
    });
  }

  getDataValue(lon, lat) {
    lon = ((lon % 360) + 360) % 360;

    if (lat < this.options.lat1 || lat > this.options.lat2) {
      return null;
    }

    let lonIndex = Math.floor((lon - this.options.lon1) / this.options.xdelta);
    lonIndex = lonIndex % this.cols;
    if (lonIndex < 0) lonIndex += this.cols;
    const latIndex = Math.floor((lat - this.options.lat1) / this.options.ydelta);

    if (latIndex < 0 || latIndex >= this.rows) {
      return null;
    }

    return this.data[latIndex][lonIndex];
  }

  createDebugLayer() {
    const debugContainer = document.createElement('div');
    debugContainer.style.position = 'absolute';
    debugContainer.style.top = '0';
    debugContainer.style.left = '0';
    debugContainer.style.zIndex = '1000';
    debugContainer.style.pointerEvents = 'none';
    debugContainer.style.backgroundColor = 'rgba(255, 255, 255, 0.8)';
    debugContainer.style.padding = '5px';
    document.body.appendChild(debugContainer);

    olMap.getMap().on('pointermove', (event) => {
      const coordinate = olMap.getMap().getCoordinateFromPixel(event.pixel);
      const [lon, lat] = transform(coordinate, 'EPSG:3857', 'EPSG:4326');
      const value = this.getDataValue(lon, lat);
      const colorIndex = this.findNearestColorIndex(value);
      
      let valueRange = '';
      if (colorIndex >= 0 && colorIndex < this.values.length - 1) {
        valueRange = `[${this.values[colorIndex]} to ${this.values[colorIndex + 1]})`;
      }

      const color = colorIndex >= 0 ? this.colors[colorIndex] : null;
      const colorRGB = color ? `rgb(${color.r}, ${color.g}, ${color.b})` : 'none';
      
      debugContainer.innerHTML = `
        经度: ${lon.toFixed(4)}°
        纬度: ${lat.toFixed(4)}°
        原始值: ${value !== null ? value.toFixed(4) : 'null'}
        颜色索引: ${colorIndex}
        值范围: ${valueRange}
        RGB颜色: <span class="w-[10px] h-[10px]" style="background-color: ${colorRGB}">${colorRGB}</span></span>
      `;
      debugContainer.style.left = (event.pixel[0] + 10) + 'px';
      debugContainer.style.top = (event.pixel[1] + 10) + 'px';
    });
  }

  findNearestColorIndex(value) {
    if (value === null || value === undefined || !this.values.length) {
      return 0;
    }

    if (value < this.values[0]) {
      return 0;
    }

    for (let i = 0; i < this.values.length - 1; i++) {
      if (value >= this.values[i] && value < this.values[i + 1]) {
        return i + 1;
      }
    }

    if (value >= this.values[this.values.length - 1]) {
      return this.values.length - 1;
    }
    
    return 0;
  }

  updateData(data) {
    this.data = data;
    this.tileCache.clear();
    const source = this.get("source");
    if (source) {
      source.clear();
      source.refresh();
    }
  }


createSource() {
  const tileSize = [360, 180];
  const projection = getProjection("EPSG:4326");

  if (!projection) {
    throw new Error("Failed to get EPSG:4326 projection");
  }

  const { lat1, lat2, lon1, lon2 } = this.options
  let xdelta = this.options.xdelta;
  let ydelta = this.options.ydelta;
  // TODO: 临时处理
  const extent = [
    lon1 - xdelta/2,
    lat1 - ydelta/2,
    lon2 + xdelta/2,
    lat2 + ydelta/2
  ];

  const resolution = this.options.xdelta;

  const tileGrid = new TileGrid({
    extent: extent,
    resolutions: [resolution],
    tileSize: tileSize,
    origin: [extent[0], extent[3]],
  });

  const source = new DataTileSource({
    projection: projection,
    tileGrid: tileGrid,
    tileSize: tileSize,
    wrapX: true,
    loader: (z, x, y) => {
      const cacheKey = `${z}-${x}-${y}`;
      if (this.tileCache.has(cacheKey)) {
        return this.tileCache.get(cacheKey);
      }

      const data = new Uint8Array(tileSize[0] * tileSize[1] * 4);
      const tileExtent = tileGrid.getTileCoordExtent([z, x, y]);

      const minLon = tileExtent[0];
      const minLat = tileExtent[1];
      const maxLon = tileExtent[2];
      const maxLat = tileExtent[3];

      const lonStep = (maxLon - minLon) / tileSize[0];
      const latStep = (maxLat - minLat) / tileSize[1];

      for (let i = 0; i < tileSize[1]; i++) {
        const lat = maxLat - i * latStep - latStep / 2;
        for (let j = 0; j < tileSize[0]; j++) {
          const lon = minLon + j * lonStep + lonStep / 2;

          const value = this.getDataValue(lon, lat);
          const idx = (i * tileSize[0] + j) * 4;

          if (value === null || value === undefined) {
            data[idx] = 0;
            data[idx + 1] = 0;
            data[idx + 2] = 0;
            data[idx + 3] = 0;
          } else {
            const colorIndex = this.findNearestColorIndex(value);
            const color = colorIndex >= 0 ? this.colors[colorIndex] : null;
            if (color) {
              data[idx] = color.r;
              data[idx + 1] = color.g;
              data[idx + 2] = color.b;
              data[idx + 3] = color.a;
            } else {
              data[idx] = 0;
              data[idx + 1] = 0;
              data[idx + 2] = 0;
              data[idx + 3] = 0;
            }
          }
        }
      }

      this.tileCache.set(cacheKey, data);
      return data;
    }
  });

  return source;
}
}
