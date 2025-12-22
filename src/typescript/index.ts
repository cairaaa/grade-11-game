import sharp from "sharp";
import path from "path";
import fs from "fs";

const getDirname = (importMetaUrl: string) => {
  return path.dirname(new URL(importMetaUrl).pathname);
};

async function splitImage(imagePath: string, tileSize: number = 16): Promise<void> {
  const currentDir = getDirname(import.meta.url);
  const image = sharp(imagePath);
  
  const metadata = await image.metadata();
  const { width, height } = metadata;
  
  console.log(`image dimensions: ${width}x${height}`);
  
  const baseName = path.basename(imagePath, path.extname(imagePath));
  
  const outputFolder = path.join(currentDir, `../assets/${baseName}`);
  if (!fs.existsSync(outputFolder)) {
    fs.mkdirSync(outputFolder, { recursive: true });
  }
  
  let tileIndex = 0;
  
  for (let y = 0; y < height!; y += tileSize) {
    for (let x = 0; x < width!; x += tileSize) {
      const tileWidth = Math.min(tileSize, width! - x);
      const tileHeight = Math.min(tileSize, height! - y);
      
      if (tileWidth <= 0 || tileHeight <= 0) {
        continue;
      }
      
      try {
        const tileBuffer = await sharp(imagePath)
          .extract({ left: x, top: y, width: tileWidth, height: tileHeight })
          .toBuffer();
        
        const isTransparent = await isTileTransparent(tileBuffer);
        
        if (!isTransparent) {
          const tileFileName = `${baseName}${tileIndex}.png`;
          const outputPath = path.join(outputFolder, tileFileName);
          
          await sharp(tileBuffer).toFile(outputPath);
          console.log(`saved: ${tileFileName}`);
          
          tileIndex++;
        }
      } catch (error) {
        console.error(`error extracting tile at (${x}, ${y}):`, error);
      }
    }
  }
  console.log(`total: ${tileIndex}`);
}

async function isTileTransparent(tileBuffer: Buffer): Promise<boolean> {
  const image = sharp(tileBuffer);
  const { data, info } = await image.raw().toBuffer({ resolveWithObject: true });
  for (let i = 3; i < data.length; i += 4) {
    if (data[i] > 0) {
      return false;
    }
  }
  return true;
}

const imagePath = "cave.png";
splitImage(imagePath);
