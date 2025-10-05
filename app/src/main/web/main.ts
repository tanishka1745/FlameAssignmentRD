/**
 * Simple TypeScript web viewer
 * - Renders a base64 sample image to canvas
 * - Shows overlay with FPS and resolution
 * - Can simulate a stream at ~15 FPS
 */

const SAMPLE_BASE64 =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAgAAAAIACAIAAAB7GkOtAAAACXBIWXMAAAsTAAALEwEAmpwYAAABU0lEQVR4nO3RMQ0AAAgDMO5fNBOwqYgQG1k4vJwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgO4GgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAYI8E0CwQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAYI8E0CwQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAYI8E0CwQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAYI8E0CwQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAYK4Bx5wAAAP//AwB5t3pH8WJMAAAAASUVORK5CYII=";

let canvas = document.getElementById("frameCanvas") as HTMLCanvasElement;
let overlay = document.getElementById("overlay") as HTMLDivElement;
let ctx = canvas.getContext("2d")!;
let btnLoad = document.getElementById("btnLoad") as HTMLButtonElement;
let btnSimFPS = document.getElementById("btnSimFPS") as HTMLButtonElement;
let btnStop = document.getElementById("btnStop") as HTMLButtonElement;

let img = new Image();
img.crossOrigin = "anonymous";

let lastTime = performance.now();
let frames = 0;
let fps = 0;
let simHandle: number | null = null;

// draw image (resizes to canvas while keeping aspect ratio)
function drawImageToCanvas(image: HTMLImageElement) {
  const vw = canvas.width;
  const vh = canvas.height;
  const iw = image.naturalWidth;
  const ih = image.naturalHeight;

  // fit image into canvas maintaining aspect
  const scale = Math.min(vw / iw, vh / ih);
  const w = Math.round(iw * scale);
  const h = Math.round(ih * scale);
  const x = Math.round((vw - w) / 2);
  const y = Math.round((vh - h) / 2);

  ctx.clearRect(0, 0, vw, vh);
  ctx.drawImage(image, x, y, w, h);

  // stats
  frames++;
  const now = performance.now();
  if (now - lastTime >= 500) {
    fps = Math.round((frames * 1000) / (now - lastTime));
    frames = 0;
    lastTime = now;
  }

  overlay.textContent = `FPS: ${fps} • ${iw}×${ih}`;
}

// load sample frame
btnLoad.addEventListener("click", () => {
  img.onload = () => {
    drawImageToCanvas(img);
  };
  img.onerror = (e) => {
    console.error("Image load error", e);
    alert("Failed to load sample image");
  };
  img.src = SAMPLE_BASE64;
});

// simulate a stream by repeatedly drawing the same image at ~15 FPS
btnSimFPS.addEventListener("click", () => {
  if (simHandle) return;
  btnLoad.disabled = true;
  btnSimFPS.disabled = true;
  btnStop.disabled = false;

  img.onload = () => {
    // start interval of 66ms ~ 15 FPS
    simHandle = window.setInterval(() => {
      drawImageToCanvas(img);
    }, 66);
  };
  img.src = SAMPLE_BASE64;
});

// stop sim
btnStop.addEventListener("click", () => {
  if (simHandle) {
    clearInterval(simHandle);
    simHandle = null;
  }
  btnLoad.disabled = false;
  btnSimFPS.disabled = false;
  btnStop.disabled = true;
});

// allow drop of an image file onto the canvas to preview a processed frame
canvas.addEventListener("dragover", (e) => { e.preventDefault(); });
canvas.addEventListener("drop", (e) => {
  e.preventDefault();
  const dt = e.dataTransfer!;
  if (!dt || !dt.files || dt.files.length === 0) return;
  const file = dt.files[0];
  const reader = new FileReader();
  reader.onload = () => {
    img.onload = () => drawImageToCanvas(img);
    img.src = reader.result as string;
  };
  reader.readAsDataURL(file);
});