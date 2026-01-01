import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Game extends JPanel implements MouseListener, KeyListener, Runnable {
    // constants
    final int TILE_LENGTH = 64;
    final int FPS = 60;
    final int SCREEN_WIDTH = 12 * TILE_LENGTH;
    final int SCREEN_HEIGHT = 9 * TILE_LENGTH;

    // global variables
    Thread thread;
    JPanel panel;

    // map/image related
    static int[][] map;
    // change to actual num later
    static BufferedImage[][] tileImages = new BufferedImage[100][16];
    static BufferedImage[] playerImages;

    // movement
    boolean up, down, left, right;
    int speed = 4;
    // change these later
    int cameraX = 200;
    int cameraY = 200;
    int playerTileX, playerTileY;
    String lastDirection = "down";

    // animation
    int animationFrame = 0;
    int animationCounter = 0;
    int animationSpeed = 10;

    // remove later
    JLabel cameraLocation;
    JLabel playerLocation;

    // constructor
    public Game() {
        setBackground(Color.BLACK);
        setPreferredSize(
            new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT)
        );
        thread = new Thread(this);
		thread.start();

        // remove this later
        cameraLocation = new JLabel("camera: " + cameraX + " " + cameraY);
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        add(cameraLocation);
        
        playerLocation = new JLabel("player: " + playerTileX + " " + playerTileY);
        add(playerLocation);

        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
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

    public void drawPlayer(Graphics g) {
        int startIndex = 0;
        int frameToDraw;

        if (down) {
            startIndex = 4;
        } else if (up) {
            startIndex = 16;
        } else if (right) {
            startIndex = 10;
        } else if (left) {
            startIndex = 22;
        } else {
            if (lastDirection.equals("down")) {
                frameToDraw = 0;
            } else if (lastDirection.equals("right")) {
                frameToDraw = 1;
            } else if (lastDirection.equals("up")) {
                frameToDraw = 2;
            } else if (lastDirection.equals("left")) {
                frameToDraw = 3;
            } else {
                System.out.println("eee");
                frameToDraw = 0;
            }
            g.drawImage(playerImages[frameToDraw], 288, 192, 192, 192, null);
            return;
        }
        frameToDraw = startIndex + (animationFrame % 6);
        g.drawImage(playerImages[frameToDraw], 288, 192, 192, 192, null);
    }

    // movement related
    public void move() {
        // change this later so its only in bounds for all directions
		if (left) {
            if (cameraX > 0) {
                cameraX -= speed;
            }
        } else if (right) {
            cameraX += speed;
        }
        
        if (up) {
            if (cameraY > 0) {
                cameraY -= speed;
            }
        } else if (down) {
            cameraY += speed;
        }
        playerTileX = (cameraX + SCREEN_WIDTH / 2) / TILE_LENGTH;
        playerTileY = (cameraY + SCREEN_HEIGHT / 2) / TILE_LENGTH;

        // maybe add this with the other if statements later
        if (up) {
            lastDirection = "up";
        } else if (down) {
            lastDirection = "down";
        } else if (left) {
            lastDirection = "left";
        } else if (right) {
            lastDirection = "right";
        }
	}

    // animation
    public void animatePlayer() {
        if (up || down || left || right) {
            animationCounter++;
            if (animationCounter >= animationSpeed) {
                animationFrame++;
                animationCounter = 0;
            }
        } else {
            animationFrame = 0;
        }
    }

    // initializing the game
    public static int[][] getDefaultMap() {
        // change this later
        int[][] defaultMap = {
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 17, 18, 18, 18, 18, 18, 18, 18, 18, 19, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16},
            {16, 22, 23, 23, 23, 23, 23, 23, 23, 23, 24, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
            {16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16},
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
        int startCol = Math.max(0, cameraX / TILE_LENGTH);
        int endCol = Math.min(
            map[0].length - 1, (cameraX + 12 * TILE_LENGTH) / TILE_LENGTH
        );
        int startRow = Math.max(0, cameraY / TILE_LENGTH);
        int endRow = Math.min(
            map.length - 1, (cameraY + 9 * TILE_LENGTH) / TILE_LENGTH
        );

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int x = col * TILE_LENGTH - cameraX;
                int y = row * TILE_LENGTH - cameraY;
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

    public static BufferedImage[] loadImages(File folder, boolean mod) throws IOException {
        File[] files = folder.listFiles();
        System.out.println(folder.getAbsolutePath());
        System.out.println(folder.exists());
        System.out.println(folder.isDirectory());
        int number = files.length;
        BufferedImage[] images = new BufferedImage[number];
        for (int i = 0; i < number; i++) {
            String name = files[i].getName();
            BufferedImage image = ImageIO.read(files[i]);
            int index;
            if (mod) {
                index = Integer.parseInt(name.substring(0, 2)) % 16;
            } else {
                index = Integer.parseInt(name.substring(0, 2));
            }
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

        tileImages[0] = loadImages(new File("assets/tiles/farmland"), true);
        tileImages[1] = loadImages(new File("assets/tiles/grass"), true);
        tileImages[2] = loadImages(new File("assets/tiles/misc"), true);
        tileImages[3] = loadImages(new File("assets/tiles/water"), true);

        playerImages = loadImages(new File("assets/player"), false);

        return isGeneralCreated && isInvCreated && isMapCreated;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawMap(g);
        drawPlayer(g);
        cameraLocation.setText("camera: " + cameraX + " " + cameraY);
        playerLocation.setText("player: " + playerTileX + " " + playerTileY);
    }

    // mouse related methods
    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX() + cameraX;
        int y = e.getY() + cameraY;
        int tileX = x / TILE_LENGTH;
        int tileY = y / TILE_LENGTH;

        System.out.println("pressed at " + x + ", " + y);
        System.out.println("pressed tile: " + tileX + ", " + tileY);
        boolean result = updateTile(tileX, tileY);
        if (result) {
            repaint();
        }
    }

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

    // key related methods
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A) {
			left = true;
			right = false;
		} else if (key == KeyEvent.VK_D) {
			right = true;
			left = false;
		} else if (key == KeyEvent.VK_W) {
			up = true;
			down = false;
		} else if (key == KeyEvent.VK_S) {
			down = true;
			up = false;
		}
    }

    @Override 
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
		if (key == KeyEvent.VK_A) {
			left = false;
		} else if (key == KeyEvent.VK_D) {
			right = false;
		} else if (key == KeyEvent.VK_W) {
			up = false;
		} else if (key == KeyEvent.VK_S) {
			down = false;
		}
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
	public void run() {
		while(true) {
			// main game loop
            move();
            animatePlayer();
			this.repaint();
			try {
				Thread.sleep(1000/FPS);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

    public static void main(String[] args) throws IOException {
        System.out.println("hello world!");
        // do smth if it fails or smth or put this in constructor?? idk
        System.out.println("initialize: " + initialize());
        JFrame frame = new JFrame("Game");
        Game panel = new Game();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
