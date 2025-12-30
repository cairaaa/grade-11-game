import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function loadMap(): number[][] {
  const absolutePath = path.resolve(__dirname, "../data/map.txt");
  const data = fs.readFileSync(absolutePath, "utf-8");
  const map = data
    .trim()
    .split(/\r?\n/)
    .map(line => line
      .trim()
      .split(" ")
      .map(Number)
    );
  return map;
}

export function saveMap(map: number[][]): void {
  const absolutePath = path.resolve(__dirname, "../data/map.txt");
  const data = map.map(row => row.join(" ")).join("\n");
  fs.writeFileSync(absolutePath, data, "utf-8");
}

export function addRow(map: number[][], index: number, value: number): void {
  map.splice(index, 0, Array(map[0].length).fill(value));
}

export function addColumn(map: number[][], index: number, value: number): void {
  for (let i = 0; i < map.length; i++) {
    map[i].splice(index, 0, value);
  }
}

export function deleteRow(map: number[][], index: number): void {
  map.splice(index, 1);
}

export function deleteColumn(map: number[][], index: number): void {
  for (let i = 0; i < map.length; i++) {
      map[i].splice(index, 1);
  }
}