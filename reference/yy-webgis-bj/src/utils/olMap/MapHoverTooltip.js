import { transform } from 'ol/proj';

class MapHoverTooltip {
  constructor(map, customImageTileLayer, originalData, lats, lons, values, colors) {
    this.map = map;
    this.layer = customImageTileLayer;
    this.originalData = originalData;
    this.lats = lats;
    this.lons = lons;
    this.values = values;
    this.colors = colors.map(rgbStr => {
      const matches = rgbStr.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
      if (!matches) {
        console.error('Invalid RGB string format:', rgbStr);
        return { r: 0, g: 0, b: 0, a: 0 };
      }
      return {
        r: parseInt(matches[1]),
        g: parseInt(matches[2]),
        b: parseInt(matches[3]),
        a: 255
      };
    });
    
    // 创建tooltip元素
    this.createTooltip();
    // 绑定事件
    this.bindEvents();
  }

  createTooltip() {
    this.tooltip = document.createElement('div');
    this.tooltip.className = 'map-hover-tooltip';
    this.tooltip.style.cssText = `
      position: absolute;
      display: none;
      background: rgba(255, 255, 255, 0.9);
      padding: 8px;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      font-size: 14px;
      pointer-events: none;
      z-index: 1000;
      min-width: 200px;
    `;
    
    this.map.getViewport().appendChild(this.tooltip);
  }

  bindEvents() {
    this.map.on('pointermove', (event) => {
      if (event.dragging) {
        this.hideTooltip();
        return;
      }

      const pixel = this.map.getEventPixel(event.originalEvent);
      const coordinate = this.map.getCoordinateFromPixel(pixel);
      const [lon, lat] = transform(coordinate, 'EPSG:3857', 'EPSG:4326');
      
      const info = this.getPointInfo(lon, lat);
      if (info) {
        this.showTooltip(event.originalEvent, info);
      } else {
        this.hideTooltip();
      }
    });

    this.map.getViewport().addEventListener('mouseout', () => {
      this.hideTooltip();
    });
  }

  findNearestIndex(value, array) {
    return array.reduce((nearest, current, index) => {
      return Math.abs(current - value) < Math.abs(array[nearest] - value) 
        ? index 
        : nearest;
    }, 0);
  }

  getPointInfo(lon, lat) {
    const latIndex = this.findNearestIndex(lat, this.lats);
    const lonIndex = this.findNearestIndex(lon, this.lons);

    const value = this.originalData[latIndex][lonIndex];
    if (value === null || value === undefined) return null;

    // 找到最近的值和对应的颜色
    const colorIndex = this.findNearestIndex(value, this.values);
    const color = this.colors[colorIndex];

    return {
      longitude: lon,
      latitude: lat,
      value: value,
      color: color
    };
  }

  showTooltip(event, info) {
    const colorBox = `
      <div style="
        display: inline-block;
        width: 20px;
        height: 20px;
        background-color: rgba(${info.color.r}, ${info.color.g}, ${info.color.b}, ${info.color.a/255});
        border-radius: 2px;
        vertical-align: middle;
        margin-left: 5px;
      "></div>
    `;

    this.tooltip.innerHTML = `
      <div style="margin-bottom: 4px">
        <span>经度: ${info.longitude.toFixed(6)}°</span>
      </div>
      <div style="margin-bottom: 4px">
        <span>纬度: ${info.latitude.toFixed(6)}°</span>
      </div>
      <div style="margin-bottom: 4px">
        <span>数值: ${info.value.toFixed(2)}</span>
      </div>
      <div>
        <span>颜色:</span>
        ${colorBox}
      </div>
    `;

    const tooltipWidth = this.tooltip.offsetWidth;
    const tooltipHeight = this.tooltip.offsetHeight;
    const mapSize = this.map.getSize();
    
    let left = event.clientX + 10;
    let top = event.clientY - tooltipHeight - 10;

    if (left + tooltipWidth > mapSize[0]) {
      left = event.clientX - tooltipWidth - 10;
    }
    if (top < 0) {
      top = event.clientY + 10;
    }

    this.tooltip.style.left = `${left}px`;
    this.tooltip.style.top = `${top}px`;
    this.tooltip.style.display = 'block';
  }

  hideTooltip() {
    this.tooltip.style.display = 'none';
  }

  destroy() {
    if (this.tooltip && this.tooltip.parentNode) {
      this.tooltip.parentNode.removeChild(this.tooltip);
    }
    this.map.un('pointermove', this.bindEvents);
  }
}

export default MapHoverTooltip;
