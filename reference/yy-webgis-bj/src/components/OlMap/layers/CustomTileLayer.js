import ImageTileSource from 'ol/source/ImageTile.js';
import { toLonLat, transformExtent } from 'ol/proj';

class ColorRange {
  constructor(start, end, color) {
    this.start = start;
    this.end = end;
    this.color = color;
  }
}

class CustomImageTile extends ImageTileSource {
  constructor(data, lat, lon, options, tileGrid, projection) {
    super({
      tileGrid: tileGrid,
      projection,
    })
    // super()
    this.curProjection = projection
    this.originalData = data;
    this.lats = lat;
    this.lons = lon;
    this.row = lat.length;
    this.col = lon.length;
    this.colorBar = {}
    this.map = {};
    this.imageDataMap = {}
    this.keyCount = 0;
    this.options = {
      colors: [],
      min: 0,
      max: 100,
      step: 0.25,
      n: 100,
    }
    this.interval = 10 // 每两个颜色值之间插值的个数
    this.colorRanges = [];
    this.setLoader((z, x, y) => {
      const canvas = this.getCanvas(z, x, y);
      return canvas;
    }
    )
    this.setKey(this.keyFromUrlLike());
    if (this.getState() !== 'ready') {
      this.setState('ready');
    }

    this.setOptions(options)
  }
  keyFromUrlLike(url) {
    if (Array.isArray(url)) {
      return url.join('\n');
    }

    if (typeof url === 'string') {
      return url;
    }

    ++this.keyCount;
    return 'url-function-key-' + this.keyCount;
  }

  intGetimageDataMap(key) {
    let v = this.imageDataMap[key];
    return v;
  }

  intimageDataMap(key, val) {
    let v = this.imageDataMap[key];
    if (v == undefined) {
      this.imageDataMap[key] = val;
    }
  }

  /**
   * 设置图例和颜色插值
  */
  setOptions(opt) {
    Object.assign(this.options, opt)

    this.configureColorRanges(this.options.min, this.options.max, this.options.values, this.options.colors)
    this.initInterpolatedColors()
  }

  configureColorRanges(min, max, values, colors) {
    this.colorRanges = [];
    const rangeSize = colors.length - 1;
    for (let i = 0; i < rangeSize; i++) {
      this.colorRanges.push(new ColorRange(values[i], values[i + 1], colors[i]));
    }
    this.colorRanges.push(new ColorRange(max, max, colors[rangeSize]));
  }

  initInterpolatedColors() {
    this.interpolateColors_fun(this.options.min, this.options.max, this.options.n);
  }

  interpolateColors_fun() {
    this.interpolatedColors = new Map();
    for (let i = 0; i < this.colorRanges.length; i++) {
      let start = this.colorRanges[i].start
      let end = this.colorRanges[i].end
      let step = (end - start) / this.interval
      for (let j = 0; j < this.interval; j++) {
        let value = start + step * j
        const roundedValue = parseFloat(value.toFixed(5));
        const interpolatedColor = this.interpolateColor(roundedValue);
        this.interpolatedColors.set(roundedValue, interpolatedColor)
      }
    }
    this.interpolatedColorskeys = [...this.interpolatedColors.keys()];
  }

  interpolateColor(value) {
    for (let i = 0; i < this.colorRanges.length - 1; i++) {
      const range1 = this.colorRanges[i];
      const range2 = this.colorRanges[i + 1];

      if (value >= range1.start && value <= range2.start) {
        const ratio = (value - range1.start) / (range2.start - range1.start);
        const r = this.interpolateComponent(range1.color.r, range2.color.r, ratio);
        const g = this.interpolateComponent(range1.color.g, range2.color.g, ratio);
        const b = this.interpolateComponent(range1.color.b, range2.color.b, ratio);
        const a = this.interpolateComponent(range1.color.a, range2.color.a, ratio);
        return { r, g, b, a };
      }
    }

    return this.colorRanges[this.colorRanges.length - 1].color;
  }
  interpolateComponent(c1, c2, ratio) {
    if (c1 === c2) {
      return c1;
    } else if (c1 < c2) {
      return Math.round(c1 + (c2 - c1) * ratio);
    } else {
      return Math.round(c1 - (c1 - c2) * ratio);
    }
  }
  intColorMap(key, value) {
    let v = this.map[key];
    if (v == undefined) {
      this.map[key] = value;
    }
  }
  intColorGetMap(key) {
    let v = this.map[key];
    return v;
  }

  // 创建一个绘制瓦片内容的函数
  createTileContent(z, x, y) {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    const tileSize = 256;  // 默认瓦片大小
    canvas.width = tileSize;
    canvas.height = tileSize;

    let { topLeft, bottomRight } = this.getTileBounds(z, x, y)
    this.getTileData(Math.round(topLeft[0]), Math.round(bottomRight[1]), Math.round(bottomRight[0]), Math.round(topLeft[1]), ctx)
    ctx.font = '16px Arial';
    ctx.fillText(`Tile Z${z} X${x} Y${y}`, 10, 20);

    return canvas;
  }

  bilinearInterpolate(a, nn, mm) {
    const n = a.length;
    const m = a[0].length;
    const b = new Array(nn);
    for (let i = 0; i < nn; i++) {
      b[i] = new Array(mm);
    }
    const scaleX = (n - 1) / (nn - 1);
    const scaleY = (m - 1) / (mm - 1);
    for (let i = 0; i < nn; i++) {
      for (let j = 0; j < mm; j++) {
        // debugger
        const x = i * scaleX;
        const y = j * scaleY;
        const x0 = Math.floor(x);
        const y0 = Math.floor(y);
        const x1 = Math.min(x0 + 1, n - 1);
        const y1 = Math.min(y0 + 1, m - 1);
        const dx = x - x0;
        const dy = y - y0;
        const q00 = a[x0][y0];
        const q01 = a[x0][y1];
        const q10 = a[x1][y0];
        const q11 = a[x1][y1];
        // console.log(q00,q01,q10,q11);
        if (q00 == null || q01 == null || q10 == null || q11 == null) {
          // console.log(1);
          b[i][j] = null;
        } else {
          const interpolatedValue = (1 - dx) * (1 - dy) * q00 + dx * (1 - dy) * q10 + (1 - dx) * dy * q01 + dx * dy * q11;
          b[i][j] = Math.round(interpolatedValue);
        }
      }
    }
    return b;
  }
  findClosest(ds, x) {//arr, target
    let left = 0;
    let right = ds.length - 1;
    while (left <= right) {
      const mid = left + Math.round((right - left) / 2);

      if (ds[mid] === x) {
        return ds[mid];
      } else if (ds[mid] < x) {
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }
    // 当 left > right 时，意味着找不到与 x 相等的元素，
    // 此时 left 和 right 会错开一位，所以要根据与 x 距离来确定返回值
    if (left >= ds.length) {
      return ds[right];
    } else if (right < 0) {
      return ds[left];
    } else {
      const diffLeft = Math.abs(ds[left] - x);
      const diffRight = Math.abs(ds[right] - x);
      return (diffLeft < diffRight) ? ds[left] : ds[right];
    }
  }

  getTileBounds(z, x, y) {
    const tileGrid = this.getTileGrid();

    // 获取瓦片分辨率（像素/地理单位）
    const resolution = tileGrid.getResolution(z);

    // 获取瓦片的左上角（即最西北点）和右下角（即最东南点）坐标
    let tileExtent = tileGrid.getTileCoordExtent([z, x, y]);
    if (store.projection !== '1') {
      tileExtent = transformExtent(tileExtent, 'EPSG:3857', this.curProjection)
    }

    // 将坐标从 Web Mercator 转换为经纬度坐标
    const topLeftLonLat = toLonLat([tileExtent[0], tileExtent[1]], this.curProjection);
    const bottomRightLonLat = toLonLat([tileExtent[2], tileExtent[3]], this.curProjection);

    return {
      topLeft: topLeftLonLat,      // 左上角的经纬度
      bottomRight: bottomRightLonLat,  // 右下角的经纬度
    };
  }

  getTileData(lonleft, latleft, lonright, latright, ctx) {
    let newRight = lonright === -180 ? 180 : lonright;
    lonright = newRight;
    let imageData = this.intGetimageDataMap(lonleft + "_" + latleft + "_" + lonright + "_" + latright);

    if (imageData == undefined) {
      let ddd = []
      let toDatax = 0

      for (let lat = 90; lat >= -90; lat = lat - this.options.step) {
        if (lat >= latright && lat <= latleft) {
          // debugger
          for (let lon = -180; lon <= 180; lon = lon + this.options.step) {
            if (lon >= lonleft && lon <= lonright) {
              // debugger
              let lati = this.lats.indexOf(lat);
              let loni = this.lons.indexOf(lon)
              if (!ddd[toDatax]) {
                ddd[toDatax] = [];
              }
              if (lati == -1 || loni == -1) {
                ddd[toDatax].push(null)
                continue
              }
              ddd[toDatax].push(this.originalData[lati][loni])
            }
          }
          toDatax++
        }
      }
      // debugger
      if (ddd.length == 0) return
      let rrr = this.bilinearInterpolate(ddd, 256, 256)
      imageData = ctx.createImageData(256, 256);
      const pixels = imageData.data;

      let pixelsindex = 0;
      let closest;//邻近值
      //以下绘制瓦片每个像素值
      for (let i = 0; i < rrr.length; i++) {
        for (let j = 0; j < rrr[i].length; j++) {
          let color = this.intColorGetMap(rrr[i][j]);
           if (rrr[i][j] == null) {
            color = {
              r: 0,
              g: 0,
              b: 0,
              a: 0
            }
          }
          //取临数组中近点 1-2
          if (color == undefined) {
            closest = this.findClosest(this.interpolatedColorskeys, rrr[i][j]);
            //根据索引位置 给颜色赋值
            color = this.interpolatedColors.get(closest);//492
            this.intColorMap(rrr[i][j], color);
          }
          pixels[pixelsindex] = color.r; // R 值
          pixels[pixelsindex + 1] = color.g; // G 值
          pixels[pixelsindex + 2] = color.b; // B 值
          pixels[pixelsindex + 3] = color.a;// Alpha 值（不透明）
          pixelsindex = pixelsindex + 4;
        }
      }
      this.intimageDataMap(lonleft + "_" + latleft + "_" + lonright + "_" + latright, imageData);
    }
    ctx.putImageData(imageData, 0, 0);
  }

  getCanvas(z, x, y) {
    const canvas = this.createTileContent(z, x, y); // 获取自定义瓦片内容
    return canvas
  }
}

export default CustomImageTile
