import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

public class Game {
    // constants
    final int TILE_LENGTH = 64;

    // global variables
    static int[][] map;

    public Game() {

    }

    public static int[][] getDefaultMap() {
        // change this later
        int[][] defaultMap = {
            {3, 3, 3, 3, 3},
            {3, 3, 1, 1, 3},
            {3, 2, 2, 1, 3},
            {3, 1, 1, 0, 3},
            {3, 3, 3, 3, 3}
        };
        return defaultMap;
    }

    public static int[][] loadMap() throws FileNotFoundException {
        File mapFile = new File("src/data/map.txt");
        Scanner inFile = new Scanner(mapFile);

        String firstLine = inFile.nextLine();
        // change split later
        String[] firstRow = firstLine.split(" ");
        int cols = firstRow.length;

        int rows = 1;
        while (inFile.hasNextLine()) {
            inFile.nextLine();
            rows++;
        }
        inFile.close();

        inFile = new Scanner(mapFile);
        map = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            String[] values = inFile.nextLine().split(" ");
            for (int col = 0; col < values.length; col++) {
                map[row][col] = Integer.parseInt(values[col]);
            }
        }
        inFile.close();
        return map;
    }

    public static boolean createFile(File file) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                return false;
            }
        }
        return true;
    }

    public static boolean initialize() throws IOException {
        File folder = new File("src/data");
        File generalFile = new File("src/data/general.txt");
        File inventoryFile = new File("src/data/inv.txt");
        File mapFile = new File("src/data/map.txt");

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                return false;
            }
        }
        boolean isGeneralCreated = createFile(generalFile);
        boolean isInvCreated = createFile(inventoryFile);
        boolean isMapCreated = createFile(mapFile);

        if (mapFile.length() == 0) {
            PrintWriter out = new PrintWriter(new FileWriter("src/data/map.txt"));
            int[][] defaultMap = getDefaultMap();
            map = defaultMap;
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    out.print(map[i][j] + " ");
                }
                out.println();
            }
            out.close();
        } else {
            int[][] loadedMap = loadMap();
            // remove this later
            for (int i = 0; i < loadedMap.length; i++) {
                for (int j = 0; j < loadedMap[i].length; j++) {
                    System.out.print(loadedMap[i][j] + " ");
                }
                System.out.println();
            }
        }

        return isGeneralCreated && isInvCreated && isMapCreated;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("hello world!");
        // do smth if it fails or smth or put this in constructor?? idk
        System.out.println(initialize());
    }
}
