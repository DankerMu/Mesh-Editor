import ImageLayer from 'ol/layer/Image.js';
import Static from 'ol/source/ImageStatic.js';

class GridLayer extends ImageLayer {
  constructor(map, path) {
    super({
      source: new Static({
        url: path,
        projection: 'EPSG:4326',
        imageExtent: [70, 25, 111, 50],
      }),
      opacity: 0.8,
      zIndex: 0,
    });

    this.map = map;
  }

  updateSource(path) {
    const imageExtent = this.getSource().getImageExtent();
    const newSource = new Static({
      url: path || 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAwAB/ep63DcAAAAASUVORK5CYII=', // 新图片 URL
      projection: 'EPSG:4326',
      imageExtent: imageExtent, // 保持原来的 extent
    });
    this.setSource(newSource);
  }
}

export default GridLayer