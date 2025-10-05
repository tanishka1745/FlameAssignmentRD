/**
 * Simple TypeScript web viewer
 * - Renders a base64 sample image to canvas
 * - Shows overlay with FPS and resolution
 * - Can simulate a stream at ~15 FPS
 */

let canvas = document.getElementById("frameCanvas") as HTMLCanvasElement;
let overlay = document.getElementById("overlay") as HTMLDivElement;
let ctx = canvas.getContext("2d")!;
let btnLoad = document.getElementById("btnLoad") as HTMLButtonElement;
let btnSimFPS = document.getElementById("btnSimFPS") as HTMLButtonElement;
let btnStop = document.getElementById("btnStop") as HTMLButtonElement;

let img = new Image();
img.crossOrigin = "anonymous";

let lastTime = performance.now();
let frameCount = 0;   // renamed
let fps: number = 0;  // explicitly typed
let simHandle: number | null = null;

// draw image (resizes to canvas while keeping aspect ratio)
function drawImageToCanvas(image: HTMLImageElement) {
  const vw = canvas.width;
  const vh = canvas.height;
  const iw = image.naturalWidth;
  const ih = image.naturalHeight;

  const scale = Math.min(vw / iw, vh / ih);
  const w = Math.round(iw * scale);
  const h = Math.round(ih * scale);
  const x = Math.round((vw - w) / 2);
  const y = Math.round((vh - h) / 2);

  ctx.clearRect(0, 0, vw, vh);
  ctx.drawImage(image, x, y, w, h);

  // stats
  frameCount++;
  const now = performance.now();
  if (now - lastTime >= 500) {
    fps = Math.round((frameCount * 1000) / (now - lastTime));
    frameCount = 0;
    lastTime = now;
  }

  overlay.textContent = `FPS: ${fps} • ${iw}×${ih}`;
}