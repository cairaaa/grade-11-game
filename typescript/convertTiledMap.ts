import mapJson from "./map.json";
import fs from "fs";

const data = mapJson.map;

function getMapNumbers(): Set<number> {
  const numbers = new Set<number>();
  for (let i = 0; i < data.length; i++) {
    numbers.add(data[i]);
  }
  return numbers;
}

// const numbersArray = Array.from(getMapNumbers()).sort((n1, n2) => n1 - n2);
// console.log(numbersArray);
// change this array when there are more numbers
const numbersArray = [
  81,  82,  83,  97,  98,  99,
  113, 114, 115, 129, 130, 145,
  146, 162, 164, 165, 166, 168,
  170, 171, 173, 174, 176, 177
] as const;
type numbers = typeof numbersArray[number];

const tilesMap: Record<numbers, number> = {
  81: 17,
  82: 18,
  83: 19,
  97: 20,
  98: 32,
  99: 21,
  113: 22,
  114: 23,
  115: 24,
  129: 25,
  130: 26,
  145: 27,
  146: 28,
  162: 50,
  164: 52,
  165: 48,
  166: 53,
  168: 55,
  170: 57,
  171: 58,
  173: 59,
  174: 60,
  176: 16,
  177: 33
}

function convertMap() {
  const width = 49;
  const height = data.length / width;

  const result: number[][] = [];

  for (let i = 0; i < height; i++) {
    result[i] = data.slice(i * width, (i + 1) * width);
  }
  for (let i = 0; i < result.length; i++) {
    for (let j = 0; j < result[i].length; j++) {
      const mapped = tilesMap[result[i][j] as keyof typeof tilesMap];
      result[i][j] = mapped;
    }
  }

  console.log(result);

  fs.writeFileSync(
    // resolve this path later
    "../data/map.txt",
    result.map(r => r.join(" ")).join("\n"),
    "utf-8"
  );
}

convertMap();