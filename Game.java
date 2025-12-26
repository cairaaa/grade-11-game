import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Game extends JPanel implements MouseListener {
    // constants
    final int TILE_LENGTH = 64;

    // global variables
    static int[][] map;
    // change to actual num later
    static BufferedImage[][] tileImages = new BufferedImage[100][16];

    // constructor
    public Game() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(map[0].length * TILE_LENGTH, map.length * TILE_LENGTH));
    }

    // helper functions
    public static int[] getTileParts(int num) {
        // change this if the number stuff changes which it probably will
        int[] array = {num / 16, num % 16};
        return array;
    }

    // drawing stuff
    public boolean isFarmland(int value) {
        return value >= 0 && value < 16;
    }

    public boolean updateTile(int col, int row) {
        int num = map[row][col];
        
        if (!isFarmland(num) && num != 32) {
            return false;
        }
        
        if (num == 32) {
            int mask = 0;
            if (row > 0 && isFarmland(map[row - 1][col])) {
                mask += 1;  
            }
            if (col < map[0].length - 1 && isFarmland(map[row][col + 1])) {
                mask += 2;
            }
            if (row < map.length - 1 && isFarmland(map[row + 1][col])) {
                mask += 4;
            }
            if (col > 0 && isFarmland(map[row][col - 1])) {
                mask += 8;
            }
            map[row][col] = mask;
        } else {
            map[row][col] = 32;
        }
        
        System.out.println(
            "update tile at (" + col + ", " + row + "): " + num + " to " + map[row][col]
        );
        if (row > 0) {
            updateTileMask(col, row - 1);
        }
        if (col < map[0].length - 1) {
            updateTileMask(col + 1, row);
        }
        if (row < map.length - 1) {
            updateTileMask(col, row + 1);
        }
        if (col > 0) {
            updateTileMask(col - 1, row);
        }
        return true;
    }

    public void updateTileMask(int col, int row) {
        int num = map[row][col];
        
        if (!isFarmland(num)) {
            return;
        }
        
        int mask = 0;
        if (row > 0 && isFarmland(map[row - 1][col])) {
            mask += 1;
        }
        if (col < map[0].length - 1 && isFarmland(map[row][col + 1])) {
            mask += 2;
        }
        if (row < map.length - 1 && isFarmland(map[row + 1][col])) {
            mask += 4;
        }
        if (col > 0 && isFarmland(map[row][col - 1])) {
            mask += 8;
        }
        
        map[row][col] = mask;
        System.out.println("update edge to: " + map[row][col]);
    }

    // initializing the game
    public static int[][] getDefaultMap() {
        // change this later
        int[][] defaultMap = {
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 17, 18, 18, 18, 18, 18, 18, 19, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 22, 23, 23, 23, 23, 23, 23, 24, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16}
        };
        return defaultMap;
    }

    public static int[][] loadMap() throws FileNotFoundException {
        File mapFile = new File("data/map.txt");
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

    public void drawMap(Graphics g) {
        // change the method later so it only draws the visible parts
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                int x = col * TILE_LENGTH;
                int y = row * TILE_LENGTH;
                int tile = map[row][col];
                int[] tileNumbers = getTileParts(tile);
                if (tile >= 0) {
                    g.drawImage(
                        tileImages[tileNumbers[0]][tileNumbers[1]], x, y, 
                        TILE_LENGTH, TILE_LENGTH, null
                    );
                }
            }
        }
    }

    public static BufferedImage[] loadImages(File folder) throws IOException {
        File[] files = folder.listFiles();
        System.out.println(folder.getAbsolutePath());
        System.out.println(folder.exists());
        System.out.println(folder.isDirectory());
        int number = files.length;
        BufferedImage[] images = new BufferedImage[number];
        for (int i = 0; i < number; i++) {
            String name = files[i].getName();
            BufferedImage image = ImageIO.read(files[i]);
            int index = Integer.parseInt(name.substring(0, 2)) % 16;
            images[index] = image;
        }
        return images;
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
        File folder = new File("data/");
        File generalFile = new File("data/general.txt");
        File inventoryFile = new File("data/inv.txt");
        File mapFile = new File("data/map.txt");

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                return false;
            }
        }
        boolean isGeneralCreated = createFile(generalFile);
        boolean isInvCreated = createFile(inventoryFile);
        boolean isMapCreated = createFile(mapFile);

        if (mapFile.length() == 0) {
            PrintWriter out = new PrintWriter(new FileWriter("data/map.txt"));
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

        tileImages[0] = loadImages(new File("assets/tiles/farmland"));
        tileImages[1] = loadImages(new File("assets/tiles/grass"));
        tileImages[2] = loadImages(new File("assets/tiles/misc"));
        tileImages[3] = loadImages(new File("assets/tiles/water"));

        return isGeneralCreated && isInvCreated && isMapCreated;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        System.out.println("running paint component :)");
        drawMap(g);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int tileX = x / TILE_LENGTH;
        int tileY = y / TILE_LENGTH;

        System.out.println("pressed at " + x + ", " + y);
        System.out.println("pressed tile: " + tileX + ", " + tileY);
        boolean result = updateTile(tileX, tileY);
        if (result) {
            repaint();
        }
    }

    // mouse related methods
    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public static void main(String[] args) throws IOException {
        System.out.println("hello world!");
        // do smth if it fails or smth or put this in constructor?? idk
        System.out.println("initialize: " + initialize());

        JFrame frame = new JFrame ("Game");
        Game panel = new Game();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        panel.addMouseListener(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
