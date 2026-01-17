import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Game extends JPanel implements 
    ActionListener,
    MouseListener, 
    MouseMotionListener,
    KeyListener, 
    Runnable {
    // constants
    final int TILE_LENGTH = 64;
    final int FPS = 30;
    final int SCREEN_WIDTH = 12 * TILE_LENGTH;
    final int SCREEN_HEIGHT = 9 * TILE_LENGTH;

    // global variables
    Thread thread;
    boolean initialized;
    static boolean wait;

    // general
    Font font;
    int money;
    int earned;
    int days;

    // map/image related
    int[][] map;
    int[][] objectsMap;
    int[][] wetTilesMap;

    int[][] houseTiles;
    int[][] houseObjects;
    
    BufferedImage[][] tileImages = new BufferedImage[5][16];
    BufferedImage[][] objectImages = new BufferedImage[5][16];
    BufferedImage[][] itemImages = new BufferedImage[3][16];
    BufferedImage[][] cropImages = new BufferedImage[12][4];
    BufferedImage[] playerImages;
    BufferedImage[] uiImages;
    BufferedImage[] stateImages;

    Image offScreenImage;
    Graphics offScreenBuffer;

    // movement
    boolean up, down, left, right;
    int speed = 10;
    int cameraX = 480;
    int cameraY = 840;
    int playerTileX, playerTileY;
    String lastDirection = "down";

    int houseX = 67;
    int houseY = 380;

    // animation
    int movementAnimationFrame = 0;
    int movementAnimationCounter = 0;
    int actionAnimationFrame = 0;
    int actionAnimationCounter = 0;
    boolean isPlayingActionAnimation = false;
    String currentAction = "";
    int animationSpeed = 4;

    // game state
    int framesPassed = 0;
    int secondsPassed = 0;
    int totalMinutes;
    int hours;
    int minutes;
    int increment = 30;
    String state = "game";
    
    // inventory related
    int hotbarSlot = 0;
    int[] hotbar = new int[9];
    int[][] inventory = new int[3][9];
    int invX, invY;
    int draggedItemSlot;

    // store related
    int storeSelected = -1;
    int[][] storeItems = new int[6][6];

    // remove later
    JLabel cameraLocation;
    JLabel playerLocation;

    // constructor
    public Game() throws IOException, FontFormatException {
        setBackground(Color.BLACK);
        setPreferredSize(
            new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT)
        );

        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);

        font = Font.createFont(
            Font.TRUETYPE_FONT, new File("assets/font/PixelifySans-Regular.ttf")
        );
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
        font = font.deriveFont(Font.PLAIN, 20f);

        // remove this later
        cameraLocation = new JLabel("camera: " + cameraX + " " + cameraY);
        cameraLocation.setFont(font);
        add(cameraLocation);
        
        playerLocation = new JLabel("player: " + playerTileX + " " + playerTileY);
        playerLocation.setFont(font);
        add(playerLocation);

        thread = new Thread(this);
		thread.start();
    }

    // helper functions
    public int[] getTileParts(int num) {
        int[] array = {num / 16, num % 16};
        return array;
    }

    public int[] getItemInfo(int num) {
        int[] array = {num / 100, num % 100};
        return array;
    }

    public int[] getStoreItem(int num) {
        int[] array = {num / 6, num % 6};
        return array;
    }

    public int[] getCropInfo(int num) {
        // make this for in the objects array, since the numbers have to be different
        int[] array = {};
        return array;
    }

    public int[] getImageDim(Image image) {
        int[] imageDimensions = {
            (int) Math.floor(image.getWidth(this)), 
            (int) Math.floor(image.getHeight(this))
        };
        return imageDimensions;
    }

    public int[] getImageDim(Image image, double scalar) {
        int[] imageDimensions = {
            (int) Math.floor(image.getWidth(this) * scalar), 
            (int) Math.floor(image.getHeight(this) * scalar)
        };
        return imageDimensions;
    }

    public void printArray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i] + " ");
        }
        System.out.println();
    }

    public void printArray(int[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                System.out.print(array[i][j] + " ");
            }
        }
        System.out.println();
    }

    public String[] getStoreItemInfo(int num) {
        String[][] itemInfo = new String[21][6];
        if (num == 0) {
            itemInfo[0][0] = "Wheat Seed";
            itemInfo[0][1] = "16";
            itemInfo[0][2] = "The starting crop";
            itemInfo[0][3] = "2";
            itemInfo[0][4] = "10";
            itemInfo[0][5] = "5";
        } else if (num == 1) {
            itemInfo[1][0] = "Carrot Seed";
            itemInfo[1][1] = "17";
            itemInfo[1][2] = "A simple plant";
            itemInfo[1][3] = "2";
            itemInfo[1][4] = "18";
            itemInfo[1][5] = "10";
        } else if (num == 2) {
            itemInfo[2][0] = "Celery Seed";
            itemInfo[2][1] = "18";
            itemInfo[2][2] = (
                "There is a 75% chance it \n" + 
                "grows in 2 days, and 25% \n" +  
                "chance it grows in 3 days"
            );
            itemInfo[2][3] = "2-3";
            itemInfo[2][4] = "35";
            itemInfo[2][5] = "20";
        } else if (num == 3) {
            itemInfo[3][0] = "Lettuce Seed";
            itemInfo[3][1] = "19";
            itemInfo[3][2] = (
                "There is a 50% chance it \n" +
                "grows in 2 days, and 50% \n" +
                "chance it grows in 3 days"
            );
            itemInfo[3][3] = "2-3";
            itemInfo[3][4] = "80";
            itemInfo[3][5] = "50";
        } else if (num == 4) {
            itemInfo[4][0] = "Pea Seed";
            itemInfo[4][1] = "20";
            itemInfo[4][2] = (
                "A consistent crop that\n" +
                "grows in 3 days"
            );
            itemInfo[4][3] = "3";
            itemInfo[4][4] = "150";
            itemInfo[4][5] = "100";
        } else if (num == 5) {
            itemInfo[5][0] = "Onion Seed";
            itemInfo[5][1] = "21";
            itemInfo[5][2] = (
                "There is only a 80% chance \n" + 
                "that you actually get the \n" +
                "crop"
            );
            itemInfo[5][3] = "2-4";
            itemInfo[5][4] = "300";
            itemInfo[5][5] = "200";
        } else if (num == 6) {
            itemInfo[6][0] = "Radish Seed";
            itemInfo[6][1] = "22";
            itemInfo[6][2] = (
                "There is a wide variance of\n" +
                "days it takes to grow"
            );
            itemInfo[6][3] = "2-4";
            itemInfo[6][4] = "300";
            itemInfo[6][5] = "220";
        } else if (num == 7) {
            itemInfo[7][0] = "Potato Seed";
            itemInfo[7][1] = "23";
            itemInfo[7][2] = (
                "A consistent crop that\n" +
                "grows in 4 days"
            );
            itemInfo[7][3] = "4";
            itemInfo[7][4] = "470";
            itemInfo[7][5] = "350";
        } else if (num == 8) {
            itemInfo[8][0] = "Corn Seed";
            itemInfo[8][1] = "24";
            itemInfo[8][2] = (
                "A crop that grows quickly,\n" +
                "but sells for less"
            );
            itemInfo[8][3] = "2";
            itemInfo[8][4] = "570";
            itemInfo[8][5] = "500";
        } else if (num == 9) {
            itemInfo[9][0] = "Grape Seed";
            itemInfo[9][1] = "25";
            itemInfo[9][2] = (
                "After they are planted,\n" +
                "they will stay and make\n" + 
                "grapes every 3 days"
            );
            itemInfo[9][3] = "3";
            itemInfo[9][4] = "200";
            itemInfo[9][5] = "1000";
        } else if (num == 10) {
            itemInfo[10][0] = "Pumpkin Seed";
            itemInfo[10][1] = "26";
            itemInfo[10][2] = (
                "A consistent crop that\n" +
                "grows in 5 days"
            );
            itemInfo[10][3] = "5";
            itemInfo[10][4] = "1800";
            itemInfo[10][5] = "1500";
        } else if (num == 11) {
            itemInfo[11][0] = "Strawberry Seed";
            itemInfo[11][1] = "27";
            itemInfo[11][2] = (
                "A consistent crop that\n" +
                "grows in 6 days"
            );
            itemInfo[11][3] = "6";
            itemInfo[11][4] = "3000";
            itemInfo[11][5] = "2500";
        } else if (num == 12) {
            // fix all of this laters
            itemInfo[12][0] = "Hoe Upgrade 1";
            itemInfo[12][1] = "34";
            itemInfo[12][2] = (
                "Increases the number of\n" +
                "tiles from 1 to 4"
            );
            itemInfo[12][3] = "-1";
            itemInfo[12][4] = "-1";
            itemInfo[12][5] = "1000";
        } else if (num == 13) {
            itemInfo[13][0] = "Hoe Upgrade 2";
            itemInfo[13][1] = "35";
            itemInfo[13][2] = (
                "Increases the number of\n" +
                "tiles from 4 to 9"
            );
            itemInfo[13][3] = "-1";
            itemInfo[13][4] = "-1";
            itemInfo[13][5] = "5000";
        } else if (num == 14) {
            itemInfo[14][0] = "Hoe Upgrade 3";
            itemInfo[14][1] = "36";
            itemInfo[14][2] = (
                "Increases the number of\n" +
                "tiles from 9 to 25"
            );
            itemInfo[14][3] = "-1";
            itemInfo[14][4] = "-1";
            itemInfo[14][5] = "20000";
        } else if (num == 18) {
            itemInfo[18][0] = "Watering Can 1";
            itemInfo[18][1] = "37";
            itemInfo[18][2] = (
                "Increases the number of\n" +
                "tiles from 1 to 4"
            );
            itemInfo[18][3] = "-1";
            itemInfo[18][4] = "-1";
            itemInfo[18][5] = "1000";
        } else if (num == 19) {
            itemInfo[19][0] = "Watering Can 2";
            itemInfo[19][1] = "38";
            itemInfo[19][2] = (
                "Increases the number of\n" +
                "tiles from 4 to 9"
            );
            itemInfo[19][3] = "-1";
            itemInfo[19][4] = "-1";
            itemInfo[19][5] = "5000";
        } else if (num == 20) {
            itemInfo[20][0] = "Watering Can 3";
            itemInfo[20][1] = "39";
            itemInfo[20][2] = (
                "Increases the number of\n" +
                "tiles from 9 to 25"
            );
            itemInfo[20][3] = "-1";
            itemInfo[20][4] = "-1";
            itemInfo[20][5] = "20000";
        }
        return itemInfo[num];
    }

    // updating arrays
    public boolean isFarmland(int value) {
        return value >= 0 && value < 16;
    }

    public void updateTile(int col, int row) {
        int num = map[row][col];
        
        if (!isFarmland(num) && num != 32) {
            return;
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

    // change this to also use update tile
    public void updateWateredTile(int col, int row) {
        int num = map[row][col];
        if (!isFarmland(num)) {
            return;
        }
        wetTilesMap[row][col] = 1;
    }

    public boolean updateBoughtItem(int col, int row) {
        int num = storeItems[row][col];
        int[] info = getItemInfo(num);
        String[] moreInfo = getStoreItemInfo(storeSelected);
        int cost = Integer.parseInt(moreInfo[5]);
        int itemId = info[0];
        if (info[1] == 0) {
            return false;
        }
        if (money - cost < 0) {
            return false;
        }
        
        for (int i = 0; i < 3; i++) {
            int invRow;
            if (i == 0) {
                invRow = 2;
            } else if (i == 1) {
                invRow = 0;
            } else {
                invRow = 1;
            }
            for (int invCol = 0; invCol < 9; invCol++) {
                if (inventory[invRow][invCol] != -1) {
                    int[] invInfo = getItemInfo(inventory[invRow][invCol]);
                    if (itemId == invInfo[0] && invInfo[1] < 99) {
                        inventory[invRow][invCol] = invInfo[0] * 100 + invInfo[1] + 1;
                        storeItems[row][col]--;
                        money -= cost;
                        return true;
                    }
                }
            }
            for (int invCol = 0; invCol < 9; invCol++) {
                if (inventory[invRow][invCol] == -1) {
                    inventory[invRow][invCol] = itemId * 100 + 1;
                    storeItems[row][col]--;
                    money -= cost;
                    return true;
                }
            }
        }
        return false;
    }

    // move this later
    public int[] getCropType(int row, int col) {
        if (row > 0) {
            int num = objectsMap[row - 1][col];
            if (num >= 100) {
                int[] array = getItemInfo(num);
                return array;
            }
        }
        int[] array = {-1, -1};
        return array;
    }

    public void updateCrop(int col, int row) {
        if (!isFarmland(map[row][col])) {
            return;
        }
        if (objectsMap[row - 1][col] != -1) {
            return;
        }
        int item = getItemInfo(hotbar[hotbarSlot])[0];
        objectsMap[row - 1][col] = item * 100;
        if (hotbar[hotbarSlot] % 100 <= 1) {
            hotbar[hotbarSlot] = -1;
            return;
        }
        hotbar[hotbarSlot] -= 1;
    }

    // drawing stuff
    public void drawPlayer(Graphics g, int index, boolean isGame) {
        int frameToDraw;
  
        if (isPlayingActionAnimation) {
            int startIndex;
            if (lastDirection.equals("down")) {
                startIndex = index;
            } else if (lastDirection.equals("right")) {
                startIndex = index + 6;
            } else if (lastDirection.equals("up")) {
                startIndex = index + 12;
            } else {
                startIndex = index + 18;
            }
            frameToDraw = startIndex + actionAnimationFrame;
            if (isGame) {
                g.drawImage(playerImages[frameToDraw], 288, 192, 192, 192, this);
            }
        } else if (up || down || left || right) {
            int startIndex;
            if (right) {
                startIndex = index + 6;
            } else if (left) {
                startIndex = index + 18;
            } else if (up) {
                startIndex = index + 12;
            } else {
                startIndex = index;
            }
            frameToDraw = startIndex + (movementAnimationFrame % 6);
        } else {
            if (lastDirection.equals("down")) {
                frameToDraw = 0;
            } else if (lastDirection.equals("right")) {
                frameToDraw = 1;
            } else if (lastDirection.equals("up")) {
                frameToDraw = 2;
            } else {
                frameToDraw = 3;
            }
        }
        if (isGame) {
            g.drawImage(playerImages[frameToDraw], 288, 192, 192, 192, this);
        } else {
            g.drawImage(playerImages[frameToDraw], houseX, houseY, 192, 192, this);
        }
    }

    public int[] getNumberPos(int x, int y, double scalar) {
        int position = (int) (16 * scalar);
        int[] array = {
            (int) (x + position - 6 * scalar), 
            y + position, 
            (int) (x + position - 3 * scalar)
        };
        return array;
    }

    public void drawItems(
        Graphics g, 
        int[][] values, 
        int[] drawX, 
        int[] drawY, 
        double scalar
    ) {
        for (int i = 0; i < drawY.length; i++) {
            for (int j = 0; j < drawX.length; j++) {
                if (values[i][j] >= 0) {
                    int[] itemNumbers = getItemInfo(values[i][j]);
                    int[] otherNumbers = getTileParts(itemNumbers[0]);
                    g.drawImage(
                        itemImages[otherNumbers[0]][otherNumbers[1]], 
                        drawX[j], drawY[i], 
                        (int) (16 * scalar), (int) (16 * scalar), this
                    );
                    if (itemNumbers[0] < 32) {
                        int[] numberPos = getNumberPos(drawX[j], drawY[i], scalar);
                        if (itemNumbers[1] >= 10) {
                            g.drawString(
                                String.valueOf(itemNumbers[1]), 
                                numberPos[0], numberPos[1]
                            );
                        } else {
                            g.drawString(
                                String.valueOf(itemNumbers[1]), 
                                numberPos[2], numberPos[1]
                            );
                        }
                    }
                }
            }
        }
    }

    public void drawUi(Graphics g) {
        Image hotbarImage = uiImages[0];
        int[] hotbarDimensions = getImageDim(hotbarImage, 2.5);
        Image boardImage = uiImages[1];
        int[] boardDimensions = getImageDim(boardImage, 2.5);
        g.drawImage(
            hotbarImage, SCREEN_WIDTH / 2 - hotbarDimensions[0] / 2, 
            SCREEN_HEIGHT - hotbarDimensions[1], hotbarDimensions[0], 
            hotbarDimensions[1], this
        );
        g.drawImage(
            boardImage, SCREEN_WIDTH - boardDimensions[0], 0, 
            boardDimensions[0], boardDimensions[1], this
        );
        double scalar = 2.5;
        int[] x = new int[9];
        int startX = (int) Math.ceil(
            SCREEN_WIDTH / 2 - hotbarDimensions[0] / 2 + 3 * scalar
        );
        int startY = (int) Math.floor(SCREEN_HEIGHT - 19 * scalar);
        int[] y = {startY};
        for (int i = 0; i < 9; i++) {
            x[i] = (int) Math.floor(startX + i * 20 * scalar);
        }
        int[][] unsqueeze = {hotbar};
        drawItems(g, unsqueeze, x, y, scalar);
        g.drawImage(uiImages[3], x[hotbarSlot] - 8, startY - 8, 55, 55, this);
        int stringX = SCREEN_WIDTH - boardDimensions[0] + 20;
        int month = days / 30 + 1;
        int day = days % 30 + 1;
        int displayMinutes = (minutes / 30) * 30;
        Font newFont = font.deriveFont(Font.PLAIN, 16f);
        g.setFont(newFont);
        g.drawString("Month " + month + " Day " + day, stringX, 73);
        g.drawString(String.format("%02d:%02d", hours, displayMinutes), stringX, 110);
        g.drawString(String.format("$%,d", money), stringX, 147);
        g.setFont(font);
    }

    public void drawInventory(Graphics g) {
        Image inventoryImage = uiImages[2];
        int[] imageDimensions = getImageDim(inventoryImage, 3);
        int x = SCREEN_WIDTH / 2 - imageDimensions[0] / 2;
        int y = SCREEN_HEIGHT / 2 - imageDimensions[1] / 2;
        g.drawImage(
            inventoryImage, x, y, imageDimensions[0], imageDimensions[1], this
        );
        int[] drawX = new int[9];
        int[] drawY = new int[3];
        int scalar = 3;
        int startX = SCREEN_WIDTH / 2 - imageDimensions[0] / 2 + 12 * scalar;
        int startY = SCREEN_HEIGHT / 2 - imageDimensions[1] / 2 + 10 * scalar;
        drawY[0] = startY;
        drawY[1] = startY + 20 * scalar;
        drawY[2] = startY + 44 * scalar;
        for (int i = 0; i < 9; i++) {
            drawX[i] = startX + i * 20 * scalar;
        }
        drawItems(g, inventory, drawX, drawY, scalar);
    }

    public void drawStore(Graphics g) {
        int scalar = 3;
        g.drawImage(stateImages[0], 0, 0, this);
        int[] storeInvDim = getImageDim(uiImages[4], scalar);
        g.drawImage(uiImages[4], 30, 90, storeInvDim[0], storeInvDim[1], this);
        int startX = 30 + 7 * scalar;
        int startY = 90 + 7 * scalar;
        int drawX[] = new int[6];
        int drawY[] = new int[6];
        for (int i = 0; i < 6; i++) {
            drawX[i] = startX + i * 20 * scalar;
            drawY[i] = startY + i * 20 * scalar;
        }
        drawItems(g, storeItems, drawX, drawY, 3);
        if (storeSelected != -1) {
            Font fontTitle = font.deriveFont(Font.PLAIN, 24f);
            g.setFont(fontTitle);
            int[] storeItemPosition = getStoreItem(storeSelected);
            if (storeItems[storeItemPosition[0]][storeItemPosition[1]] == -1) {
                return;
            }
            String[] info = getStoreItemInfo(storeSelected);
            int[] indexInImages = getTileParts(Integer.parseInt(info[1]));
            int[] textboxDim = getImageDim(uiImages[5]);
            int stringX = 485;
            int middleX = 463 + textboxDim[0] / 2 - 32;
            g.drawImage(
                uiImages[3], drawX[storeItemPosition[1]] - 3 * scalar, 
                drawY[storeItemPosition[0]] - 3 * scalar, 66, 66, this
            );
            g.drawImage(uiImages[5], 463, 90, this);
            g.drawString(info[0], stringX, 125);
            g.drawImage(
                itemImages[indexInImages[0]][indexInImages[1]], 
                middleX, 130, 64, 64, this
            );
            Font newFont = font.deriveFont(Font.PLAIN, 16f);
            g.setFont(newFont);
            String[] description = info[2].split("\n");
            for (int i = 0; i < description.length; i++) {
                g.drawString(description[i], stringX, 220 + i * 20);
            }
            int newY = 220 + description.length * 20 + 20;
            if (storeSelected <= 11) {
                g.drawString(
                    "Days it takes to grow: " + info[3], stringX, newY
                );
                g.drawString("Sell price: " + info[4], stringX, newY + 20);
            }
            g.setColor(Color.YELLOW);
            g.drawString("Cost: $" + info[5], stringX, 350);
            g.setColor(new Color(242, 236, 208));
            g.setFont(font);
            int[] buttonImageDim = getImageDim(uiImages[6], 2);
            g.drawImage(
                uiImages[6], 463, 395, buttonImageDim[0], buttonImageDim[1], this
            );
            g.drawString(
                "Buy", 463 + buttonImageDim[0] / 2 - 18, 395 + buttonImageDim[1] / 2 + 6
            );
        }
    }

    public void drawHouse(Graphics g) {
        // tiles
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 12; col++) {
                int x = col * TILE_LENGTH;
                int y = row * TILE_LENGTH;
                int tile = houseTiles[row][col];
                int[] tileNumbers = getTileParts(tile);
                if (tile >= 0) {
                    Image image = tileImages[tileNumbers[0]][tileNumbers[1]];
                    g.drawImage(image, x, y, TILE_LENGTH, TILE_LENGTH, this);
                }
            }
        }
        // objects
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 12; col++) {
                int x = col * TILE_LENGTH;
                int y = row * TILE_LENGTH;
                int houseObject = houseObjects[row][col];
                int[] tileNumbers = getTileParts(houseObject);
                if (houseObject >= 0) {
                    Image image = objectImages[tileNumbers[0]][tileNumbers[1]];
                    int[] imageDim = getImageDim(image, 4);
                    g.drawImage(image, x, y, imageDim[0], imageDim[1], this);
                }
            }
        }
    }

    // clicking related
    public int getTileClicked(
        int clickX, 
        int clickY, 
        int startX, 
        int startY, 
        int length,
        double scalar
    ) {
        int tileSize = (int) (18 * scalar);
        int spacing = (int) (2 * scalar);
        int totalTileWidth = tileSize + spacing;
        int relativeX = clickX - startX;
        int relativeY = clickY - startY;
        if (relativeX < 0 || relativeY < 0 || relativeY >= tileSize) {
            return -1;
        }
        int slot = relativeX / totalTileWidth;
        int positionInSlot = relativeX % totalTileWidth;
        if (positionInSlot >= tileSize || slot >= length) {
            return -1;
        }
        return slot;
    }

    // movement/keyboard related
    public void move() {
        // change this later so its only in bounds for all directions
		if (left) {
            // if (cameraX > 0) {
                cameraX -= speed;
            // }
        } else if (right) {
            cameraX += speed;
        }
        
        if (up) {
            // if (cameraY > 0) {
                cameraY -= speed;
            // }
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

    public void moveStationaryCamera() {
        if (left) {
            houseX -= speed;
            lastDirection = "left";
        } else if (right) {
            houseX += speed;
            lastDirection = "right";
        }

        if (up) {
            houseY -= speed;
            lastDirection = "up";
        } else if (down) {
            houseY += speed;
            lastDirection = "down";
        }

        playerTileX = houseX / TILE_LENGTH;
        playerTileY = houseY / TILE_LENGTH;
    }

    // animation
    public void animatePlayer() {
        if (up || down || left || right) {
            movementAnimationCounter++;
            if (movementAnimationCounter >= animationSpeed) {
                movementAnimationFrame++;
                movementAnimationCounter = 0;
            }
            if (isPlayingActionAnimation) {
                isPlayingActionAnimation = false;
                actionAnimationFrame = 0;
                actionAnimationCounter = 0;
            }
        } else {
            movementAnimationFrame = 0;
        }

        if (isPlayingActionAnimation) {
            actionAnimationCounter++;
            if (actionAnimationCounter >= animationSpeed) {
                actionAnimationFrame++;
                actionAnimationCounter = 0;

                if (actionAnimationFrame >= 6) {
                    isPlayingActionAnimation = false;
                    actionAnimationFrame = 0;
                }
            }
        }
    }

    public void startActionAnimation(String action) {
        isPlayingActionAnimation = true;
        currentAction = action;
        actionAnimationFrame = 0;
        actionAnimationCounter = 0;
    }

    // state related
    public void checkState() {
        if (playerTileX == 31 && playerTileY == 18) {
            state = "store";
        } else if (playerTileX == 13 && playerTileY == 16) {
            state = "house";
            lastDirection = "up";
        } else if (state.equals("house")) {
            if (playerTileX == 1 && playerTileY == 6) {
                state = "game";
                cameraX = 480;
                cameraY = 840;
                houseX = 67;
                houseY = 380;
                lastDirection = "down";
            } else if ((playerTileX == 1 || playerTileY == 2) && playerTileY == 2) {
                state = "end";
                // call function to reset everything
            }
        }
    }

    public void calculateTime() {
        if (state.equals("game") || state.equals("house")) {
            framesPassed += 1;
            if (framesPassed == FPS) {
                framesPassed = 0;
                secondsPassed += 1;
                if (secondsPassed >= 17 * 20) {
                    // calculate smth as the day is over
                    secondsPassed = 0;
                }
                totalMinutes = 7 * 60 + secondsPassed * 3;

                hours = totalMinutes / 60 % 24;
                minutes = totalMinutes % 60;
            }
        }
    }

    // use a different function to save to files or no idk???
    public void resetDay() {
        money += earned;
        // reset earned after showing it, maybe call this when right before
        // the user leaves the end screen?
        // earned = 0;
        days += 1;
        up = false;
        down = false;
        left = false;
        right = false;
        lastDirection = "down";
        // change houseX to find the best spot
        houseX = 300;
        houseY = 380;
        movementAnimationFrame = 0;
        movementAnimationCounter = 0;
        actionAnimationFrame = 0;
        actionAnimationCounter = 0;
        isPlayingActionAnimation = false;
        currentAction = "";
        framesPassed = 0;
        secondsPassed = 0;
        totalMinutes = 0;
        hours = 0;
        minutes = 0;
        hotbarSlot = 0;
        // load shop, reset all seeds but not the tools, only check rows 0 and 1?
        // make all the crops grow
        for (int i = 0; i < wetTilesMap.length; i++) {
            for (int j = 0; j < wetTilesMap[0].length; j++) {
                wetTilesMap[i][j] = -1;
            }
        }
    }

    // initializing the game
    public int[][] getDefaultMap() {
        // change this later
        int[][] map = {
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 57, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 55, 58, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 17, 18, 18, 19, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 20, 33, 33, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 20, 33, 33, 27, 18, 18, 18, 18, 18, 18, 18, 19, 16, 16, 16, 16, 16, 16, 17, 18, 18, 19, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 20, 33, 33, 33, 33, 33, 33, 33, 33, 33, 33, 27, 19, 16, 16, 16, 16, 16, 20, 33, 33, 21, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 22, 23, 23, 23, 23, 23, 23, 26, 33, 33, 33, 33, 27, 18, 18, 19, 16, 16, 20, 33, 33, 27, 19, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 22, 23, 26, 33, 33, 33, 33, 33, 27, 18, 18, 28, 33, 33, 33, 21, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 22, 23, 26, 33, 33, 33, 33, 33, 33, 33, 25, 23, 23, 24, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 22, 23, 23, 23, 26, 33, 33, 25, 24, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 22, 23, 23, 24, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 17, 18, 18, 18, 18, 18, 19, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 20, 32, 32, 32, 32, 32, 21, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 22, 23, 23, 23, 23, 23, 24, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 59, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 60, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48}
        };
        return map;
    }

    public int[][] getDefaultObjectMap() {
        // probably change this later, too many -1s, 
        // iterate over a list of just objects instead?
        int[][] map = {
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 16, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 18, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
        };
        return map;
    }

    public int[][] getDefaultStore() {
        int[][] store = {
            {1610, 1710, 1810, 1910, 2010, 2110},
            {2210, 2310, 2410, 2510, 2610, 2710},
            {3401, 3501, 3601, -1, -1, -1},
            {3701, 3801, 3901, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1}
        };
        return store;
    }

    public void loadInventory() throws IOException {
        Scanner inFile = new Scanner(new File("data/inv.txt"));
        inventory[2] = hotbar;
        if (!inFile.hasNextLine()) {
            hotbar[0] = 3201;
            hotbar[1] = 3301;
            hotbar[2] = 1610;
            PrintWriter out = new PrintWriter(new FileWriter("data/inv.txt"));
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 9; j++) {
                    if (inventory[i][j] != 0) {
                        out.print(inventory[i][j] + " ");
                    } else {
                        inventory[i][j] = -1;
                        out.print(-1 + " ");
                    }
                }
                out.println();
            }
            inFile.close();
            out.close();
            return;
        }
        for (int i = 0; i < 3; i++) {
            String[] values = inFile.nextLine().split(" ");
            for (int j = 0; j < values.length; j++) {
                inventory[i][j] = Integer.parseInt(values[j]);
            }
        }
        inFile.close();
    }

    public int[][] loadMap(File mapFile) throws FileNotFoundException {
        Scanner inFile = new Scanner(mapFile);
        int[][] changedMap;

        String firstLine = inFile.nextLine();
        String[] firstRow = firstLine.split(" ");
        int cols = firstRow.length;

        int rows = 1;
        while (inFile.hasNextLine()) {
            inFile.nextLine();
            rows++;
        }
        inFile.close();

        inFile = new Scanner(mapFile);
        changedMap = new int[rows][cols];

        for (int row = 0; row < rows; row++) {
            String[] values = inFile.nextLine().split(" ");
            for (int col = 0; col < values.length; col++) {
                changedMap[row][col] = Integer.parseInt(values[col]);
            }
        }
        inFile.close();
        return changedMap;
    }

    public BufferedImage[] loadImages(
        File folder, 
        boolean mod
    ) throws IOException {
        File[] files = folder.listFiles();
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

    public void drawMap(Graphics g) {
        if (offScreenBuffer == null) {
            offScreenImage = createImage(SCREEN_WIDTH, SCREEN_HEIGHT);
            offScreenBuffer = offScreenImage.getGraphics();
            offScreenBuffer.setFont(font);
            // set colour here if needed
        }
        offScreenBuffer.clearRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int startCol = Math.max(0, cameraX / TILE_LENGTH);
        int endCol = Math.min(
            map[0].length - 1, (cameraX + 12 * TILE_LENGTH) / TILE_LENGTH
        );
        int startRow = Math.max(0, cameraY / TILE_LENGTH);
        int endRow = Math.min(
            map.length - 1, (cameraY + 9 * TILE_LENGTH) / TILE_LENGTH
        );
        // for drawing tiles
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int x = col * TILE_LENGTH - cameraX;
                int y = row * TILE_LENGTH - cameraY;
                int tile = map[row][col];
                int[] tileNumbers = getTileParts(tile);
                if (tile >= 0) {
                    offScreenBuffer.drawImage(
                        tileImages[tileNumbers[0]][tileNumbers[1]], x, y, 
                        TILE_LENGTH, TILE_LENGTH, this
                    );
                }
            }
        }
        // for drawing watered tiles
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int x = col * TILE_LENGTH - cameraX;
                int y = row * TILE_LENGTH - cameraY;
                int tile = wetTilesMap[row][col];
                if (tile == 1 && isFarmland(map[row][col])) {
                    offScreenBuffer.drawImage(
                        objectImages[3][map[row][col]], 
                        x, y, TILE_LENGTH, TILE_LENGTH, this
                    );
                }
            }
        }

        int maxObjectWidth;
        int maxObjectHeight;
        if (cameraY <= 1260) {
            maxObjectWidth = 10;
            maxObjectHeight = 8;
        } else {
            maxObjectWidth = 3;
            maxObjectHeight = 3;
        }

        startCol = Math.max(0, cameraX / TILE_LENGTH - maxObjectWidth);
        endCol = Math.min(
            map[0].length - 1, (cameraX + 12 * TILE_LENGTH) / TILE_LENGTH
        );
        startRow = Math.max(0, cameraY / TILE_LENGTH - maxObjectHeight);
        endRow = Math.min(
            map.length - 1, (cameraY + 9 * TILE_LENGTH) / TILE_LENGTH
        );
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int x = col * TILE_LENGTH - cameraX;
                int y = row * TILE_LENGTH - cameraY;
                int objectTile = objectsMap[row][col];
                int[] objectNumbers;
                Image image;
                int scaledWidth, scaledHeight;
                if (objectTile >= 0) {
                    if (
                        objectTile >= 0 && 
                        (
                            ((map[row][col] >= 16 || map[row][col] < 30) || 
                            map[row][col] == 32) || 
                            isFarmland(map[row][col])
                        )
                    ) {
                        if (objectTile > 100) {
                            int[] cropInfo = getItemInfo(objectTile);
                            image = cropImages[cropInfo[0] - 16][cropInfo[1]];
                            scaledWidth = image.getWidth(this) * 4;
                            scaledHeight = image.getHeight(this) * 4;
                            offScreenBuffer.drawImage(
                                image, x, y, scaledWidth, scaledHeight, this
                            );
                        } else {
                            objectNumbers = getTileParts(objectTile);
                            image = objectImages[objectNumbers[0]][objectNumbers[1]];
                            scaledWidth = image.getWidth(this) * 4;
                            scaledHeight = image.getHeight(this) * 4;
                            offScreenBuffer.drawImage(
                                image, x, y, scaledWidth, scaledHeight, this
                            );
                        }
                    }
                }
            }
        }
        g.drawImage(offScreenImage, 0, 0, this);
    }    

    public boolean initialize() {
        File folder = new File("data/");
        File generalFile = new File("data/general.txt");
        File inventoryFile = new File("data/inv.txt");
        File mapFile = new File("data/map.txt");
        File objectFile = new File("data/objectMap.txt");
        File storeFile = new File("data/store.txt");

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                return false;
            }
        }

        try {
            if (generalFile.length() == 0) {
                PrintWriter out = new PrintWriter(new FileWriter("data/general.txt"));
                out.print("100 1");
                money = 100;
                days = 1;
                out.close();
            } else {
                int[] generalArray = loadMap(generalFile)[0];
                money = generalArray[0];
                days = generalArray[1];
            }
            if (inventoryFile.length() == 0) {
                PrintWriter out = new PrintWriter(new FileWriter("data/inv.txt"));
                out.close();
            }
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
                int[][] loadedMap = loadMap(mapFile);
                map = loadedMap;
            }
            if (objectFile.length() == 0) {
                PrintWriter out = new PrintWriter(new FileWriter("data/objectMap.txt"));
                int[][] defaultMap = getDefaultObjectMap();
                objectsMap = defaultMap;
                for (int i = 0; i < objectsMap.length; i++) {
                    for (int j = 0; j < objectsMap[i].length; j++) {
                        out.print(objectsMap[i][j] + " ");
                    }
                    out.println();
                }
                out.close();
            } else {
                int[][] loadedObjectsMap = loadMap(objectFile);
                objectsMap = loadedObjectsMap;
            }
            if (storeFile.length() == 0) {
                PrintWriter out = new PrintWriter(new FileWriter("data/store.txt"));
                int[][] defaultStore = getDefaultStore();
                storeItems = defaultStore;
                for (int i = 0; i < storeItems.length; i++) {
                    for (int j = 0; j < storeItems[i].length; j++) {
                        out.print(storeItems[i][j] + " ");
                    }
                    out.println();
                }
                out.close();
            } else {
                int[][] loadedStoreItems = loadMap(storeFile);
                storeItems = loadedStoreItems;
            }
            int[][] houseMap = {
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                {-1, 65, 66, 66, 66, 66, 66, 66, 66, 66, 67, -1},
                {-1, 68, 73, 74, 74, 74, 74, 74, 74, 75, 69, -1},
                {-1, 68, 76, 77, 77, 77, 77, 77, 77, 78, 69, -1},
                {-1, 68, 64, 64, 64, 64, 64, 64, 64, 64, 69, -1},
                {-1, 68, 64, 64, 64, 64, 64, 64, 64, 64, 69, -1},
                {-1, 68, 64, 64, 64, 64, 64, 64, 64, 64, 69, -1},
                {-1, 68, 64, 79, 71, 71, 71, 71, 71, 71, 72, -1},
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
            };
            houseTiles = houseMap;
            int[][] objectHouseMap = {
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, 65, -1, -1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, 69, -1, -1},
                {-1, -1, 66, -1, -1, -1, -1, 71, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1, 70, -1, -1, 68, -1, -1},
                {-1, -1, -1, -1, -1, -1, -1, 72, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
                {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
            };
            houseObjects = objectHouseMap;
            // can add trophy here if the user has one??
            // check if this is correct later
            String[] cropFolders = (new File("assets/object-crops")).list();
            for (int i = 0; i < 12; i++) {
                String directory = null;
                for (int j = 0; j < cropFolders.length; j++) {
                    if (Integer.parseInt(cropFolders[j].substring(0, 2)) == i) {
                        directory = cropFolders[j];
                        break;
                    }
                }
                if (directory == null) {
                    directory = "error";
                }
                cropImages[i] = loadImages(
                    new File("assets/object-crops/" + directory), false
                );
            }

            wetTilesMap = new int[map.length][map[0].length];
            loadInventory();

            tileImages[0] = loadImages(new File("assets/tiles/farmland"), true);
            tileImages[1] = loadImages(new File("assets/tiles/grass"), true);
            tileImages[2] = loadImages(new File("assets/tiles/misc"), true);
            tileImages[3] = loadImages(new File("assets/tiles/water"), true);
            tileImages[4] = loadImages(new File("assets/tiles/house"), true);

            objectImages[0] = loadImages(new File("assets/objects/extra"), true);
            objectImages[1] = loadImages(new File("assets/objects/structures"), true);
            objectImages[2] = loadImages(new File("assets/objects/trees"), true);
            objectImages[3] = loadImages(
                new File("assets/objects/wet-farmland"), true
            );
            objectImages[4] = loadImages(new File("assets/objects/interior"), true);

            itemImages[0] = loadImages(new File("assets/items/crops"), true);
            itemImages[1] = loadImages(new File("assets/items/seeds"), true);
            itemImages[2] = loadImages(new File("assets/items/tools"), true);

            playerImages = loadImages(new File("assets/player"), false);
            uiImages = loadImages(new File("assets/ui"), false);
            stateImages = loadImages(new File("assets/state"), false);
        } catch (IOException e) {

        }
        return true;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!initialized) {
            return;
        }
        g.setColor(new Color(242, 236, 208));
        g.setFont(font);
        if (state.equals("game") || state.equals("inv")) {
            drawMap(g);
            if (isPlayingActionAnimation) {
                if (currentAction.equals("hoeing")) {
                    drawPlayer(g, 28, true);
                } else {
                    drawPlayer(g, 52, true);
                }
            } else {
                drawPlayer(g, 4, true);
            }
            drawUi(g);
            if (state.equals("inv")) {
                drawInventory(g);
            }
            cameraLocation.setText("camera: " + cameraX + " " + cameraY);
            playerLocation.setText("player: " + playerTileX + " " + playerTileY);
        } else if (state.equals("store")) {
            drawStore(g);
            cameraLocation.setText("");
            playerLocation.setText("");
        } else if (state.equals("house")) {
            drawHouse(g);
            drawPlayer(g, 4, false);
            cameraLocation.setText("camera: " + cameraX + " " + cameraY);
            playerLocation.setText("player: " + playerTileX + " " + playerTileY);
        } else if (state.equals("end")) {
            // add stuff
        }
    }

    // mouse related methods
    @Override
    public void mousePressed(MouseEvent e) {
        int actualX = e.getX();
        int actualY = e.getY();
        int x = actualX + cameraX;
        int y = actualY + cameraY;
        int tileX = x / TILE_LENGTH;
        int tileY = y / TILE_LENGTH;
        if (state.equals("game")) {
            // hotbar is drawn at 157 & 521 at top left corner
            if (
                actualX > 159 && actualX < 159 + 455 && 
                actualY > 523 && actualY < SCREEN_HEIGHT
            ) {
                int checkHotbarSlot = getTileClicked(
                    actualX, actualY, 159 + 3, 521 + 3, 9, 2.5
                );
                if (checkHotbarSlot >= 0) {
                    hotbarSlot = checkHotbarSlot;
                }
                System.out.println(
                    "hotbar clicked at " + actualX + " " + 
                    actualY + " slot: " + hotbarSlot
                );
                return;
            }
            int item = hotbar[hotbarSlot] / 100;

            System.out.println("hotbar slot: " + hotbarSlot);
            System.out.println("metadata value: " + hotbar[hotbarSlot]);

            if (item == 32) {
                System.out.println("pressed at " + x + ", " + y);
                System.out.println("pressed tile: " + tileX + ", " + tileY);
                System.out.println("hoe tool");
                startActionAnimation("hoeing");
                updateTile(tileX, tileY);
            } else if (item == 33) {
                System.out.println("pressed at " + x + ", " + y);
                System.out.println("pressed tile: " + tileX + ", " + tileY);
                System.out.println("water bucket");
                startActionAnimation("watering");
                updateWateredTile(tileX, tileY);
            } else if (item >= 16 && item <= 27) {
                System.out.println("pressed at " + x + ", " + y);
                System.out.println("pressed tile: " + tileX + ", " + tileY);
                System.out.println("crop");
                updateCrop(tileX, tileY);
            }
        } else if (state.equals("inv")) {
            // do stuff, like dragging stuff etc
        } else if (state.equals("store")) {
            int scalar = 3;
            for (int i = 0; i < 5; i++) {
                int clicked = getTileClicked(
                    actualX, actualY, 
                    30 + (7 * scalar), 
                    90 + (7 * scalar) + (i * 20 * scalar), 
                    6, scalar
                );
                if (clicked != -1) {
                    storeSelected = i * 6 + clicked;
                    break;
                }
            }
            int[] buttonImageDim = getImageDim(uiImages[6], 2);
            // 463, 395
            if (
                actualX >= 463 && actualX <= 463 + buttonImageDim[0] &&
                actualY >= 395 && actualY <= 395 + buttonImageDim[1] &&
                storeSelected != -1
            ) {
                int[] info = getStoreItem(storeSelected);
                System.out.println(updateBoughtItem(info[1], info[0]));
            }
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

    @Override
    public void mouseDragged(MouseEvent e) {
        if (state.equals("inv") && draggedItemSlot != -1) {
            invX = e.getX();
            invY = e.getY();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    // key related methods
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        // continue to add other states where this shouldnt apply to and stuff
        if (state.equals("game") || state.equals("house")) {
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
            } else if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_9) {
                hotbarSlot = Integer.parseInt(KeyEvent.getKeyText(key)) - 1;
            }
        }
        if (key == KeyEvent.VK_E) {
            if (state.equals("inv")) {
                state = "game";
            } else if (state.equals("game")) {
                state = "inv";
            }
        }
        if (key == KeyEvent.VK_ESCAPE) {
            if (state.equals("store")) {
                state = "game";
                cameraY = 960;
                lastDirection = "down";
                storeSelected = -1;
            } else if (state.equals("inv")) {
                state = "game";
            }
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
    public void actionPerformed(ActionEvent e) {

    }

    @Override
	public void run() {
        initialized = initialize();
        wait = false;
		while (true) {
			// main game loop
            if (state.equals("game")) {
                move();
            } else if (state.equals("house")) {
                moveStationaryCamera();
            }
            animatePlayer();
            calculateTime();
            checkState();
			this.repaint();
			try {
				Thread.sleep(1000/FPS);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    public static void main(String[] args) throws IOException, FontFormatException {
        System.out.println("hello world!");
        JFrame frame = new JFrame("Game");
        Game panel = new Game();
        wait = true;

        while (wait) {
            System.out.print("");
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
