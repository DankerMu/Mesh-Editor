import * as THREE from 'three';
import { Map as OLMap } from 'ol';
import { transform } from 'ol/proj';

class WindGLLayer {
  private scene: THREE.Scene;
  private camera: THREE.OrthographicCamera;
  private renderer: THREE.WebGLRenderer;
  private windMesh: THREE.LineSegments | null = null;
  private map: OLMap;
  private canvas: HTMLCanvasElement;
  private lastWindData: any = null;

  private readonly MAIN_LINE_LENGTH = 0.08;  // Main line length
  private readonly BARB_LENGTH_LOW = 0.02;   // Barb length for 5-14 knots
  private readonly BARB_LENGTH_HIGH = 0.04;  // Barb length for 15 knots and above

  constructor(map: OLMap) {
    this.map = map;
    this.canvas = document.createElement('canvas');
    this.canvas.style.position = 'absolute';
    this.canvas.style.top = '0';
    this.canvas.style.left = '0';
    this.canvas.style.pointerEvents = 'none';

    this.scene = new THREE.Scene();
    this.camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 1000);
    this.camera.position.z = 1;

    this.renderer = new THREE.WebGLRenderer({
      canvas: this.canvas,
      alpha: true,
      antialias: true
    });

    this.bindEvents();
    this.map.getViewport().appendChild(this.canvas);
    this.updateCanvasSize();
  }

  private createBarbPositions(startX: number, startY: number, direction: number, speed: number): number[] {
    const positions: number[] = [];
    const angle = direction * Math.PI / 180;

    // Create main line direction vector and rotate it
    const dirVector = new THREE.Vector3(0, 1, 0);
    dirVector.applyAxisAngle(new THREE.Vector3(0, 0, 1), angle);

    // Calculate end position of the main line
    const endX = startX + this.MAIN_LINE_LENGTH * dirVector.x;
    const endY = startY + this.MAIN_LINE_LENGTH * dirVector.y;
    positions.push(startX, startY, 0);
    positions.push(endX, endY, 0);

    // Create a perpendicular vector for barbs
    const barbDir = new THREE.Vector3(-dirVector.y, dirVector.x, 0);

    // Determine barb length based on speed
    if (speed >= 2 && speed < 4) {
      // Draw one low barb
      const barbX = endX + this.BARB_LENGTH_LOW * barbDir.x;
      const barbY = endY + this.BARB_LENGTH_LOW * barbDir.y;
      positions.push(endX, endY, 0);
      positions.push(barbX, barbY, 0);
    } else if (speed >= 4) {
      // Draw one high barb
      const barbX = endX + this.BARB_LENGTH_HIGH * barbDir.x;
      const barbY = endY + this.BARB_LENGTH_HIGH * barbDir.y;
      positions.push(endX, endY, 0);
      positions.push(barbX, barbY, 0);
    }

    return positions;
  }

  private latLonToWebGL(lon: number, lat: number): [number, number] | null {
    try {
      const mapProjection = this.map.getView().getProjection().getCode();
      const coordinates = transform([lon, lat], 'EPSG:4326', mapProjection);

      const pixel = this.map.getPixelFromCoordinate(coordinates);
      if (!pixel) return null;

      const size = this.map.getSize();
      if (!size) return null;

      const x = (pixel[0] / size[0]) * 2 - 1;
      const y = -(pixel[1] / size[1]) * 2 + 1;

      if (isNaN(x) || isNaN(y)) return null;

      return [x, y];
    } catch (error) {
      console.error('Coordinate transformation error:', error);
      return null;
    }
  }

  public updateWindData(windData: any): void {
    if (!windData) return;
    this.lastWindData = windData;
    const positions: number[] = [];
    const zoom = this.map.getView().getZoom() || 0;
    const sampleRate = Math.max(1, Math.floor(8 / Math.pow(2, zoom - 4)));

    for (let i = 0; i < windData.ysize; i += sampleRate) {
      for (let j = 0; j < windData.xsize; j += sampleRate) {
        let tempLon = windData.slon + windData.xdel * j;
        let lon = tempLon > 180 ? tempLon - 360 : tempLon;
        let lat = windData.slat + windData.ydel * i;

        if (lat > 90) continue;

        const speed = windData.data[i][j];
        const direction = windData.vdata[i][j];

        if (typeof speed !== 'number' || typeof direction !== 'number' ||
            isNaN(speed) || isNaN(direction)) {
          continue;
        }

        const webGLCoord = this.latLonToWebGL(lon, lat);
        if (webGLCoord) {
          const barbPositions = this.createBarbPositions(
            webGLCoord[0],
            webGLCoord[1],
            direction,
            speed
          );
          positions.push(...barbPositions);
        }
      }
    }

    if (positions.length === 0) {
      console.warn('No valid positions generated');
      return;
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));

    const material = new THREE.LineBasicMaterial({ 
      color: 0x000000,
      linewidth: 1
    });

    if (this.windMesh) {
      this.scene.remove(this.windMesh);
      this.windMesh.geometry.dispose();
      (this.windMesh.material as THREE.Material).dispose();
    }

    this.windMesh = new THREE.LineSegments(geometry, material);
    this.scene.add(this.windMesh);
    this.render();
  }

  private bindEvents(): void {
    this.map.on('postcompose', () => {
      if (this.lastWindData) {
        console.log('postcompose');
        this.updateWindData(this.lastWindData);
      }
    });

    this.map.on('change:size', () => {
      console.log('change:size');
      this.updateCanvasSize();
    });
  }

  private updateCanvasSize(): void {
    const size = this.map.getSize();
    if (size) {
      this.canvas.width = size[0];
      this.canvas.height = size[1];
      this.renderer.setSize(size[0], size[1], false);
      this.updateCamera();
    }
  }

  private updateCamera(): void {
  const size = this.map.getSize();
  if (!size) return;

  const width = size[0];
  const height = size[1];
  const aspectRatio = width / height;

  if (aspectRatio > 1) {
    this.camera.left = -aspectRatio;
    this.camera.right = aspectRatio;
    this.camera.top = 1;
    this.camera.bottom = -1;
  } else {
    this.camera.left = -1;
    this.camera.right = 1;
    this.camera.top = 1 / aspectRatio;
    this.camera.bottom = -1 / aspectRatio;
  }

  this.camera.near = 0.1;
  this.camera.far = 1000;
  this.camera.position.z = 1;
  this.camera.lookAt(0, 0, 0);
  this.camera.updateProjectionMatrix();
  }

  private render(): void {
    if (!this.windMesh) return;
    this.renderer.render(this.scene, this.camera);
  }

  public dispose(): void {
    if (this.windMesh) {
      this.scene.remove(this.windMesh);
      this.windMesh.geometry.dispose();
      (this.windMesh.material as THREE.Material).dispose();
    }
    this.renderer.dispose();
    this.map.getViewport().removeChild(this.canvas);
  }
}

export default WindGLLayer;



// import WindGLLayer from "./WindGLLayer.ts";
// let windBarbLayer = null;
// async function addWindBarbLayer() {
//   windBarbLayer = new WindGLLayer(olMap?.getMap());
//   const windBarb = await import("@/assets/fakeJson/windBarb.json").then(
//     (module) => module.default
//   );
//   windBarbLayer.updateWindData(windBarb);
// }
