import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

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
    Clip[] audioClips;
    int currentAudio;
    int money;
    int earned;
    int days;

    // map/image related
    int[][] map;
    int[][] objectsMap;
    int[][] wetTilesMap;

    int[][] houseTiles;
    int[][] houseObjects;
    boolean[][] houseHitboxes;

    int[][] cropDaysMap;
    boolean[][] hitboxes;
    
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
    int houseY = 220;

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
    boolean canTriggerSell;
    boolean isCreditsOpen = false;
    boolean isInstructionsOpen = false;
    int instructionPage = 0;
    String state = "title";
    
    // inventory related
    int hotbarSlot = 0;
    int[] hotbar = new int[9];
    int[][] inventory = new int[3][9];
    int[][] sellInventory = new int[2][9];
    int invX, invY;
    int draggedItemRow = -1;
    int draggedItemCol = -1;
    int draggedItem = -1;

    // store related
    int storeSelected = -1;
    int[][] storeItems = new int[6][6];

    // end screen
    int moneyAnimationFrames = 0;
    int duration = 30;
    int baseSpeed = 67;
    int displayedMoney;
    boolean isAnimatingMoney;

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
        addMouseMotionListener(this);
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

    public int[][][] getInventorySlotPositions(int shift) {
        Image inventoryImage = uiImages[2];
        int[] imageDimensions = getImageDim(inventoryImage, 3);
        int scalar = 3;
        int startX = SCREEN_WIDTH / 2 - imageDimensions[0] / 2 + 12 * scalar;
        int startY = SCREEN_HEIGHT / 2 - imageDimensions[1] / 2 + 10 * scalar + shift;
        
        int[][][] positions = new int[3][9][2];
        
        int[] drawY = new int[3];
        drawY[0] = startY;
        drawY[1] = startY + 20 * scalar;
        drawY[2] = startY + 44 * scalar;
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                positions[row][col][0] = startX + col * 20 * scalar;
                positions[row][col][1] = drawY[row];
            }
        }
        
        return positions;
    }

    public void resetArray(int[][] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = -1;
            }
        }
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
            itemInfo[5][3] = "3";
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
                "Drops 1-4 strawberries\n" +
                "when you pick it up"
            );
            itemInfo[11][3] = "6";
            itemInfo[11][4] = "1500";
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

    public int[] getItemSellPrices() {
        int[] array = new int[28];
        for (int i = 0; i < 12; i++) {
            int sellPrice = Integer.parseInt(getStoreItemInfo(i)[4]);
            int sellPriceSeed = (int) (Integer.parseInt(getStoreItemInfo(i)[5]) * 0.5);
            array[i] = sellPrice;
            array[i + 16] = sellPriceSeed;
        }
        return array;
    }

    // sound stuff
    public void loadAudio() {
        try {
            currentAudio = -1;
            audioClips = new Clip[10];

            AudioInputStream titleStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/title.wav")
            );
            audioClips[0] = AudioSystem.getClip();
            audioClips[0].open(titleStream);

            AudioInputStream mainStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/main.wav")
            );
            audioClips[1] = AudioSystem.getClip();
            audioClips[1].open(mainStream);

            AudioInputStream endStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/end.wav")
            );
            audioClips[2] = AudioSystem.getClip();
            audioClips[2].open(endStream);

            AudioInputStream hoeSoundEffectStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/hoe.wav")
            );
            audioClips[3] = AudioSystem.getClip();
            audioClips[3].open(hoeSoundEffectStream);

            AudioInputStream waterSoundEffectStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/water.wav")
            );
            audioClips[4] = AudioSystem.getClip();
            audioClips[4].open(waterSoundEffectStream);

            AudioInputStream placeSoundEffectStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/leafrustle.wav")
            );
            audioClips[5] = AudioSystem.getClip();
            audioClips[5].open(placeSoundEffectStream);

            AudioInputStream harvestSoundEffectStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/harvest.wav")
            );
            audioClips[6] = AudioSystem.getClip();
            audioClips[6].open(harvestSoundEffectStream);

            AudioInputStream buySoundEffectStream = AudioSystem.getAudioInputStream(
                new File("assets/audio/coins.wav")
            );
            audioClips[7] = AudioSystem.getClip();
            audioClips[7].open(buySoundEffectStream);
        } catch (Exception e) {

        }
    }

    public void playNewAudio(int index) {
        if (index == currentAudio) {
            System.out.println(currentAudio);
            return;
        }
        if (currentAudio != -1) {
            audioClips[currentAudio].stop();
        }
        currentAudio = index;
        audioClips[currentAudio].setFramePosition(0);
        audioClips[currentAudio].loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void playSoundEffect(int index) {
        Clip clip = audioClips[index];
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    // updating arrays
    public boolean isFarmland(int value) {
        return value >= 0 && value < 16;
    }

    public void updateTile(int col, int row, int itemId) {
        int[][] offsets;
        if (itemId == 32 || itemId == 33) {
            offsets = new int[][]{{0, 0}};
        } else if (itemId == 34 || itemId == 37) {
            offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        } else if (itemId == 35 || itemId == 38) {
            offsets = new int[][]{
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0},  {0, 0},  {1, 0},
                {-1, 1},  {0, 1},  {1, 1}
            };
        } else if (itemId == 36 || itemId == 39) {
            offsets = new int[][]{
                {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
                {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
                {-2, 0},  {-1, 0},  {0, 0},  {1, 0},  {2, 0},
                {-2, 1},  {-1, 1},  {0, 1},  {1, 1},  {2, 1},
                {-2, 2},  {-1, 2},  {0, 2},  {1, 2},  {2, 2}
            };
        } else {
            System.out.println("eeeeeee");
            return;
        }

        for (int i = 0; i < offsets.length; i++) {
            int targetCol = col + offsets[i][0];
            int targetRow = row + offsets[i][1];

            if (
                targetRow < 0 || targetRow >= map.length || 
                targetCol < 0 || targetCol >= map[0].length
            ) {
                System.out.println("eee");
                continue;
            }

            boolean isHoe = itemId == 32 || (itemId >= 34 && itemId <= 36);
            
            if (isHoe) {
                System.out.println(i);
                updateSingleTile(targetCol, targetRow);
            } else {
                updateWateredTile(targetCol, targetRow);
            }
        }
    }

    public void updateSingleTile(int col, int row) {
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

    public void updateWateredTile(int col, int row) {
        int num = map[row][col];
        if (!isFarmland(num)) {
            return;
        }

        int mask = 0;
        if (row > 0 && wetTilesMap[row - 1][col] > -1) {
            mask += 1;
        }
        if (col < map[0].length - 1 && wetTilesMap[row][col + 1] > -1) {
            mask += 2;
        }
        if (row < map.length - 1 && wetTilesMap[row + 1][col] > -1) {
            mask += 4;
        }
        if (col > 0 && wetTilesMap[row][col - 1] > -1) {
            mask += 8;
        }
        
        wetTilesMap[row][col] = mask;

        if (row > 0 && wetTilesMap[row - 1][col] > -1) {
            updateWateredTileMask(col, row - 1);
        }
        if (col < map[0].length - 1 && wetTilesMap[row][col + 1] > -1) {
            updateWateredTileMask(col + 1, row);
        }
        if (row < map.length - 1 && wetTilesMap[row + 1][col] > -1) {
            updateWateredTileMask(col, row + 1);
        }
        if (col > 0 && wetTilesMap[row][col - 1] > -1) {
            updateWateredTileMask(col - 1, row);
        }
    }

    public void updateWateredTileMask(int col, int row) {
        if (wetTilesMap[row][col] == -1) {
            return;
        }
        
        int mask = 0;
        if (row > 0 && wetTilesMap[row - 1][col] > -1) {
            mask += 1;
        }
        if (col < map[0].length - 1 && wetTilesMap[row][col + 1] > -1) {
            mask += 2;
        }
        if (row < map.length - 1 && wetTilesMap[row + 1][col] > -1) {
            mask += 4;
        }
        if (col > 0 && wetTilesMap[row][col - 1] > -1) {
            mask += 8;
        }
        
        wetTilesMap[row][col] = mask;
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
        int[] rowOrder = {2, 0, 1};
        if (itemId < 32) {
            System.out.println("buy crop");
            int[] coordinates = calculateNextSlot(inventory, itemId * 100 + 1, rowOrder);
            printArray(coordinates);
            if (coordinates[0] == -1 || coordinates[1] == -1) {
                return false;
            }
        } else {
            if (itemId >= 34 && itemId <= 36) {
                System.out.println("buy hoe");
            } else if (itemId >= 37 && itemId <= 39) {
                System.out.println("buy watering can");
            } else {
                return false;
            }
            int[] coordinates = calculateNextSlot(inventory, itemId * 100 + 1, rowOrder);
            if (coordinates[0] == -1 || coordinates[1] == -1) {
                return false;
            }
        }
        storeItems[row][col]--;
        money -= cost;
        playSoundEffect(7);

        return true;
    }

    public void updateCropPlaced(int col, int row) {
        if (!isFarmland(map[row][col])) {
            return;
        }
        if (objectsMap[row - 1][col] != -1) {
            return;
        }
        int item = getItemInfo(hotbar[hotbarSlot])[0];
        objectsMap[row - 1][col] = item * 100;
        cropDaysMap[row][col] = 0;
        System.out.println(
            "updated crops row " + row + " col " + col + " to " + cropDaysMap[row][col]
        );
        if (hotbar[hotbarSlot] % 100 <= 1) {
            hotbar[hotbarSlot] = -1;
            return;
        }
        hotbar[hotbarSlot] -= 1;
    }

    public void updateCropGrowth() {
        // keep track of the mechanisms each plant has
        for (int row = 0; row < wetTilesMap.length; row++) {
            for (int col = 0; col < wetTilesMap[0].length; col++) {
                if (wetTilesMap[row][col] < 0) {
                    continue;
                }
                if (
                    wetTilesMap[row][col] >= 0 && 
                    row > 0 && objectsMap[row - 1][col] == -1
                ) {
                    continue;
                }
                int num = objectsMap[row - 1][col];
                int[] cropInfo = getItemInfo(num);
                int cropType = cropInfo[0];
                int day = cropDaysMap[row][col];
                System.out.println(num + " " + cropType);
                String[] possibleDays = getStoreItemInfo(cropType - 16)[3].split("-");
                int maxDays = Integer.parseInt(possibleDays[possibleDays.length - 1]);
                if (day >= maxDays) {
                    continue;
                }
                double random = Math.random();
                if (cropType == 18) {
                    // 75% chance 2 days 25% chance 3 days
                    if (day == 0) {
                        if (random < 0.75) {
                            day = 2;
                        } else {
                            day = 1;
                        }
                    } else {
                        day += 1;
                    }
                } else if (cropType == 19) {
                    // 50% chance 2 days 50% chance 3 days
                    if (day == 0) {
                        if (random < 0.5) {
                            day = 2;
                        } else {
                            day = 1;
                        }
                    } else {
                        day += 1;
                    }
                } else if (cropType == 22) {
                    if (day == maxDays - 1) {
                        day += 1;
                    } else if (random > 0.5) {
                        day += 2;
                    } else {
                        day += 1;
                    }
                } else {
                    day += 1;
                }
                int stage;
                if (day >= maxDays) {
                    stage = 3;
                } else {
                    stage = (int) Math.floor((double) day / maxDays * 3);
                }
                System.out.println(
                    cropType + " " + day + " " + row + " " + col + " " + stage + " " + maxDays
                );
                objectsMap[row - 1][col] = cropType * 100 + stage;
                cropDaysMap[row][col] = day;
            }
        }
    }

    public void updateCropHarvest(int col, int row) {
        int realRow = row - 1;
        if (realRow < 0) {
            return;
        }
        int[] cropInfo = getItemInfo(objectsMap[realRow][col]);
        int amount;
        if (cropInfo[0] == 21) {
            double random = Math.random();
            if (random < 0.2) {
                amount = 0;
            } else {
                amount = 1;
            }
        } else if (cropInfo[0] == 27) {
            amount = (int) (Math.random() * 4) + 1;
        } else {
            amount = 1;
        }
        int item = cropInfo[0] * 100 - 1600 + amount;
        System.out.println("item thing id idk: " + item);
        calculateNextSlot(inventory, item, new int[]{2, 0, 1});
        objectsMap[realRow][col] = -1;
        cropDaysMap[row][col] = -1;
    }

    public void updateHitboxes(int col, int row, int width, int height) {
        for (int y = row; y < row + height; y++) {
            for (int x = col; x < col + width; x++) {
                if (y >= 0 && y < hitboxes.length &&
                    x >= 0 && x < hitboxes[0].length) {
                    hitboxes[y][x] = true;
                }
            }
        }
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

    public boolean canStack(int firstItem, int secondItem) {
        if (firstItem < 0 || secondItem < 0) {
            return false;
        }
        int[] firstInfo = getItemInfo(firstItem);
        int[] secondInfo = getItemInfo(secondItem);
        boolean stackable = (
            firstInfo[0] == secondInfo[0] && 
            firstInfo[0] < 32 && 
            firstInfo[1] + secondInfo[1] <= 99
        );
        return stackable;
    }

    public void drawDraggedItem(Graphics g) {
        int scalar = 3;
        if (draggedItemRow != -1) {
            int[] itemNumbers = getItemInfo(draggedItem);
            int[] otherNumbers = getTileParts(itemNumbers[0]);
            g.drawImage(
                itemImages[otherNumbers[0]][otherNumbers[1]], 
                invX - 8 * scalar, 
                invY - 8 * scalar, 
                16 * scalar, 16 * scalar, this
            );
            if (itemNumbers[0] < 32) {
                int[] numberPos = getNumberPos(
                    invX - 8 * scalar, 
                    invY - 8 * scalar, 
                    scalar
                );
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
                    if (itemNumbers[0] >= 32) {
                        continue;
                    }
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

    public void drawInventory(Graphics g, int shift) {
        Image inventoryImage = uiImages[2];
        int[] imageDimensions = getImageDim(inventoryImage, 3);
        int x = SCREEN_WIDTH / 2 - imageDimensions[0] / 2;
        int y = SCREEN_HEIGHT / 2 - imageDimensions[1] / 2 + shift;
        g.drawImage(
            inventoryImage, x, y, imageDimensions[0], imageDimensions[1], this
        );
        int[] drawX = new int[9];
        int[] drawY = new int[3];
        int scalar = 3;
        int[][][] slotPositions = getInventorySlotPositions(shift);

        for (int col = 0; col < 9; col++) {
            drawX[col] = slotPositions[0][col][0];
        }

        for (int row = 0; row < 3; row++) {
            drawY[row] = slotPositions[row][0][1];
        }
        drawItems(g, inventory, drawX, drawY, scalar);
    }

    public void drawSell(Graphics g) {
        int[] imageDimensions = getImageDim(uiImages[7], 3);
        int x = SCREEN_WIDTH / 2 - imageDimensions[0] / 2;
        int[] drawX = new int[9];
        int[] drawY = {121, 181};
        int[][][] slotPositions = getInventorySlotPositions(0);
        for (int col = 0; col < 9; col++) {
            drawX[col] = slotPositions[0][col][0];
        }
        g.drawImage(
            uiImages[7], x, 100, imageDimensions[0], imageDimensions[1], this
        );
        drawItems(g, sellInventory, drawX, drawY, 3);
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

    public void drawEnd(Graphics g) {
        g.setColor(new Color(87, 84, 74));
        g.drawImage(uiImages[10], 0, 0, this);
        Font newFont = font.deriveFont(Font.PLAIN, 40f);
        g.setFont(newFont);
        int x = 150;
        g.drawString("Today you earned..", x, 200);
        g.setColor(Color.YELLOW);
        g.drawString("$" + displayedMoney, x, 275);
        if (earned == 0) {
            g.drawString(":(", x + 65, 275);
        }
        g.setColor(new Color(87, 84, 74));
        if (!isAnimatingMoney) {
            Font anotherNewFont = font.deriveFont(Font.PLAIN, 20f);
            g.setFont(anotherNewFont);
            g.drawString("Total money: ", x, 320);
            g.setColor(Color.YELLOW);
            g.drawString("$" + money, x + 130, 320);
            int[] buttonDimensions = getImageDim(uiImages[6], 3);
            buttonDimensions[1] -= 20;
            g.drawImage(
                uiImages[6], x, 360, buttonDimensions[0], buttonDimensions[1], this
            );
            g.drawImage(
                uiImages[9], x + 175, 360, 
                buttonDimensions[0], buttonDimensions[1], this
            );
            g.setColor(new Color(87, 84, 74));
            g.drawString("Next Day", x + 25, 396);
            g.drawString("Back", x + 200, 396);
        }
        g.setColor(new Color(242, 236, 208));
        g.setFont(font);
    }

    public void drawTitle(Graphics g) {
        g.drawImage(uiImages[11], 0, 0, this);
        int[] buttonDimensions = getImageDim(uiImages[6], 5);
        g.drawImage(
            uiImages[6], 264, 
            350, buttonDimensions[0], buttonDimensions[1], this
        );
        Font newFont = font.deriveFont(Font.PLAIN, 40f);
        g.setFont(newFont);
        g.drawString("PLAY!!", 329, 430);
        int[] otherButtonDim = getImageDim(uiImages[9], 3);
        g.drawImage(
            uiImages[9], 100, 376, otherButtonDim[0], otherButtonDim[1], this
        );
        Font otherFont = font.deriveFont(Font.PLAIN, 16f);
        g.setFont(otherFont);
        g.drawString("Credits", 140, 420);
        g.drawImage(
            uiImages[9], SCREEN_WIDTH - 100 - otherButtonDim[0], 376, 
            otherButtonDim[0], otherButtonDim[1], this
        );
        g.drawString("Instructions", 542, 420);
        g.setColor(new Color(242, 236, 208));
        g.setFont(font);
    }

    public void drawCredits(Graphics g) {
        g.drawImage(uiImages[10], 0, 0, this);
        Font titleFont = font.deriveFont(Font.PLAIN, 40f);
        g.setFont(titleFont);
        g.setColor(new Color(87, 84, 74));
        g.drawString("Credits:", 290, 170);
        Font newFont = font.deriveFont(Font.PLAIN, 24f);
        g.setFont(newFont);
        int x = 150;
        g.drawString("Art: Kemni on itch.io and me", x, 220);
        g.drawString("Music: Pixabay, Concerned Ape, and C418", x, 260);
        g.drawString("Font: Stefie Justprince on Google Fonts", x, 300);
        g.drawString("Everything else: me", x, 340);
    }

    public void drawInstructionImage(Graphics g, Image image) {
        int[] imageDimensions = getImageDim(image, 0.25);
        int x = SCREEN_WIDTH / 2 - imageDimensions[0] / 2;
        g.drawImage(image, x, 200, imageDimensions[0], imageDimensions[1], this);
    }

    public void drawInstructions(Graphics g) {
        int x = 130;
        int titleY = 170;
        int[] y = {330, 350, 370, 390, 410, 430, 450};
        Font titleFont = font.deriveFont(Font.PLAIN, 40f);
        Font newFont = font.deriveFont(Font.PLAIN, 16f);
        g.setFont(titleFont);
        g.setColor(new Color(87, 84, 74));
        g.drawImage(uiImages[10], 0, 0, this);
        if (instructionPage == 0) {
            g.drawString("Controls:", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[12]);
            g.drawString("Movement: WASD", x, y[0]);
            g.drawString("Inventory: E", x, y[1]);
            g.drawString("Everything else: Mouse", x, y[2]);
            g.drawString("You are able to click escape to leave certain states (like ", x, y[3]);
            g.drawString("this one!)", x, y[4]);
            g.drawString("(Press left or right arrow keys to switch pages)", x, y[6]);
        } else if (instructionPage == 1) {
            g.drawString("Farming (page 1):", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[13]);
            g.drawString("The main goal of the game is to get as much money as possible!", x, y[0]);
            g.drawString("You mainly earn money through farming crops, which sell for", x, y[1]);
            g.drawString("money. Different crops sell for more or less, and some have", x, y[2]);
            g.drawString("different mechanisms. It takes a certain amount of days for a", x, y[3]);
            g.drawString("crop to finish growing, and then you are able to sell them.", x, y[4]);
        } else if (instructionPage == 2) {
            g.drawString("Farming (page 2):", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[14]);
            g.drawString("You first have to find a tile of farmable land (light brown),", x, y[0]);
            g.drawString("and use your hoe to create a tile that allows crops to be", x, y[1]);
            g.drawString("placed on (left click). Then, you can hold a seed, and click", x, y[2]);
            g.drawString("to place it down. After the seed has been placed, it has to", x, y[3]);
            g.drawString("be watered for it to grow (also left click). Aftering ", x, y[4]);
            g.drawString("watering the plant, it will grow on the next day.", x, y[5]);
        } else if (instructionPage == 3) {
            g.drawString("Day system (page 3):", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[15]);
            g.drawString("The day is seperated by days; you start on day 1, and then", x, y[0]);
            g.drawString("go onto the next day when you either go to your bed or pass", x, y[1]);
            g.drawString("out at midnight. After it is a new day, your progress is", x, y[2]);
            g.drawString("saved. There are also stuff that happens: the crops grow,", x, y[3]);
            g.drawString("the store restocks, and you earn your money.", x, y[4]);
        } else if (instructionPage == 4) {
            g.drawString("Store (page 4):", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[16]);
            g.drawString("The store sells the items that you might need. The first", x, y[0]);
            g.drawString("2 rows sell seeds; the other ones sell tools. Every seed", x, y[1]);
            g.drawString("can be bought 10 times every day, and each tool can only", x, y[2]);
            g.drawString("be bought once. The hoe and watering can increases the amount", x, y[3]);
            g.drawString("of tiles affected.", x, y[4]);
        } else if (instructionPage == 5) {
            g.drawString("Sell (page 5):", x, titleY);
            g.setFont(newFont);
            drawInstructionImage(g, uiImages[17]);
            g.drawString("You are able to go up to this person in order to sell your", x, y[0]);
            g.drawString("crops by clicking the items in your inventory. At the end", x, y[1]);
            g.drawString("of the day, the total amount of money you've earned will be", x, y[2]);
            g.drawString("added up.", x, y[3]);
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
        int nextCameraX = cameraX;
        if (left) {
            nextCameraX -= speed;
        } else if (right) {
            nextCameraX += speed;
        }
        int nextTileX = (nextCameraX + SCREEN_WIDTH / 2) / TILE_LENGTH;
        if (
            nextTileX >= 0 &&
            nextTileX < hitboxes[0].length &&
            !hitboxes[playerTileY][nextTileX]
        ) {
            cameraX = nextCameraX;
        }

        int nextCameraY = cameraY;
        if (up) {
            nextCameraY -= speed;
        } else if (down) {
            nextCameraY += speed;
        }
        int nextTileY = (nextCameraY + SCREEN_HEIGHT / 2) / TILE_LENGTH;
        if (
            nextTileY >= 0 &&
            nextTileY < hitboxes.length &&
            !hitboxes[nextTileY][playerTileX]
        ) {
            cameraY = nextCameraY;
        }

        playerTileX = (cameraX + SCREEN_WIDTH / 2) / TILE_LENGTH;
        playerTileY = (cameraY + SCREEN_HEIGHT / 2) / TILE_LENGTH;

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

    public void startMoneyAnimation() {
        if (earned == 0) {
            return;
        }
        money += earned;
        System.out.println("earned: " + earned + ", baseSpeed: " + baseSpeed);
        int estimatedFrames = earned / baseSpeed;
        duration = Math.max(Math.min(estimatedFrames, 60), 30);
        System.out.println("duration set to: " + duration);
        displayedMoney = 0;

        moneyAnimationFrames = 0;
        isAnimatingMoney = true;
    }

    public void updateMoneyAnimation() {
        if (!isAnimatingMoney) {
            return;
        }

        moneyAnimationFrames += 1;

        double time = (double) moneyAnimationFrames / duration;
        double actualTime = Math.min(time, 1.0);

        displayedMoney = earned - (int) ((1.0 - actualTime) * earned);

        if (actualTime >= 1.0) {
            displayedMoney = earned;
            isAnimatingMoney = false;
        }
    }

    // state related
    public void checkState() {
        if (state.equals("game")) {
            if (playerTileY != 18 || playerTileX < 20 || playerTileX > 22) {
                canTriggerSell = true;
            }
            if (playerTileX == 31 && playerTileY == 18) {
                state = "store";
            } else if (playerTileX == 13 && playerTileY == 16) {
                state = "house";
                lastDirection = "up";
            } else if (
                playerTileX >= 20 && playerTileX <= 22 && playerTileY == 18 && 
                canTriggerSell
            ) {
                state = "sell";
                canTriggerSell = false;
            } 
        } else if (state.equals("house")) {
            if (playerTileX >= 0 && playerTileY == 6) {
                state = "game";
                cameraX = 480;
                cameraY = 840;
                houseX = 67;
                houseY = 380;
                lastDirection = "down";
            } else if ((playerTileX == 1 || playerTileX == 2) && playerTileY == 2) {
                state = "end";
                playNewAudio(2);
                startMoneyAnimation();
                // call function to reset everything
            }
        }
    }

    public void calculateTime() {
        if (state.equals("game") || state.equals("house") || state.equals("inv")) {
            framesPassed += 1;
            if (framesPassed == FPS) {
                framesPassed = 0;
                secondsPassed += 1;
                if (secondsPassed >= 17 * 20) {
                    // calculate smth as the day is over
                    state = "end";
                    playNewAudio(2);
                    startMoneyAnimation();
                    secondsPassed = 0;
                }
                totalMinutes = 7 * 60 + secondsPassed * 3;

                hours = totalMinutes / 60 % 24;
                minutes = totalMinutes % 60;
            }
        } else if (state.equals("end")) {
            // reset these variables when the user clicks next day or back
            // to title screen
            // call function here
        }
    }

    public void resetDay() {
        days += 1;
        try {
            saveFilesNextDay();
            System.out.println("save files!!!");
        } catch (IOException e) {

        }
        earned = 0;
        up = false;
        down = false;
        left = false;
        right = false;
        lastDirection = "down";
        houseX = 67;
        houseY = 220;
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
        moneyAnimationFrames = 0;
        duration = 30;
        displayedMoney = 0;
        isAnimatingMoney = false;
        isInstructionsOpen = false;
        isCreditsOpen = false;
        int[][] defaultStore = getDefaultStore();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 6; j++) {
                storeItems[i][j] = defaultStore[i][j];
            }
        }
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] >= 0 && map[i][j] <= 15) {
                    if (wetTilesMap[i][j] == -1 && objectsMap[i - 1][j] == -1) {
                        map[i][j] = 32;
                        if (i > 0) {
                            updateTileMask(j, i - 1);
                        }
                        if (j < map[0].length - 1) {
                            updateTileMask(j + 1, i);
                        }
                        if (i < map.length - 1) {
                            updateTileMask(j, i + 1);
                        }
                        if (j > 0) {
                            updateTileMask(j - 1, i);
                        }
                    }
                }
            }
        }
        updateCropGrowth();
        resetArray(wetTilesMap);
    }

    // calcuations
    public int[] calculateNextSlot(int[][] values, int item, int[] rowOrder) {
        for (int row = 0; row < values.length; row++) {
            int realRow = rowOrder[row];
            for (int col = 0; col < values[realRow].length; col++) {
                if (
                    values[realRow][col] != -1 &&
                    item < 3200 &&
                    canStack(item, values[realRow][col])
                ) {

                    int[] firstInfo = getItemInfo(item);
                    int[] secondInfo = getItemInfo(values[realRow][col]);

                    if (secondInfo[1] < 99) {
                        int itemType = firstInfo[0];
                        int totalQuantity = secondInfo[1] + firstInfo[1];

                        values[realRow][col] = itemType * 100 + totalQuantity;

                        int[] coordinates = {realRow, col};
                        printArray(coordinates);
                        return coordinates;
                    }
                } else if (
                    (
                        values[realRow][col] == 3201 || 
                        (values[realRow][col] >= 3400 && values[realRow][col] <= 3601)
                    ) &&
                    item >= 3400 && item <= 3601
                ) {
                    if (item > values[realRow][col]) {
                        values[realRow][col] = item;
                        return new int[]{realRow, col};
                    }
                } else if (
                    (
                        values[realRow][col] == 3301 || 
                        (values[realRow][col] >= 3700 && values[realRow][col] <= 3901)
                    ) &&
                    item >= 3700 && item <= 3901
                ) {
                    if (item > values[realRow][col]) {
                        values[realRow][col] = item;
                        return new int[]{realRow, col};
                    }
                }
            }
        }
        if (item >= 3200) {
            return new int[]{-1, -1};
        }
        for (int row = 0; row < values.length; row++) {
            int realRow = rowOrder[row];
            for (int col = 0; col < values[realRow].length; col++) {

                if (values[realRow][col] == -1) {
                    values[realRow][col] = item;
                    int[] coordinates = {realRow, col};
                    printArray(coordinates);
                    return coordinates;
                }
            }
        }
        return new int[]{-1, -1};
    }

    public void calculateEarnedMoney() {
        System.out.println("earned before: " + earned);
        int[] sellPrices = getItemSellPrices();
        for (int i = 0; i < 18; i++) {
            int row = i / 9;
            int col = i % 9;
            int item = sellInventory[row][col];
            System.out.println(item + " " + row + " " + col);
            if (item == -1) {
                break;
            }
            int[] itemInfo = getItemInfo(item);
            earned += sellPrices[itemInfo[0]] * itemInfo[1];
        }
        System.out.println("earned after: " + earned);
        resetArray(sellInventory);
    }

    // saving
    public void saveFile(File file, int[][] values) throws IOException {
        PrintWriter out = new PrintWriter(file);
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                out.print(values[i][j] + " ");
            }
            out.println();
        }
        out.close();
    }

    public void saveFilesNextDay() throws IOException {
        File generalFile = new File("data/general.txt");
        File inventoryFile = new File("data/inv.txt");
        File mapFile = new File("data/map.txt");
        File objectFile = new File("data/objectMap.txt");
        File storeFile = new File("data/store.txt");
        File cropFile = new File("data/crop.txt");

        saveFile(generalFile, new int[][]{{money, days}});
        saveFile(inventoryFile, inventory);
        saveFile(mapFile, map);
        saveFile(objectFile, objectsMap);
        saveFile(storeFile, storeItems);
        saveFile(cropFile, cropDaysMap);

        System.out.println("done saving files!!!");
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
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 20, 33, 33, 27, 19, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19, 16, 22, 26, 33, 33, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 20, 33, 33, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 22, 23, 23, 24, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 20, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 21, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 22, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 24, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
            {48, 48, 48, 48, 48, 48, 48, 53, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 52, 48, 48, 48, 48, 48, 48, 48},
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
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, 32, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 16, -1, -1, -1, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7, -1, -1, -1, -1, -1, -1, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, 33, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 19, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 32, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
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
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
            };
        return map;
    }

    public boolean[][] getHitboxes() {
        int[][] objectMap = getDefaultObjectMap();
        boolean[][] array = new boolean[objectMap.length][objectMap[0].length];
        hitboxes = array;
        for (int i = 0; i < array.length; i++) {
            array[i][7] = true;
            array[i][41] = true;
        }
        for (int i = 0; i < array[0].length; i++) {
            array[7][i] = true;
            array[45][i] = true;
        }
        for (int row = 0; row < objectMap.length; row++) {
            for (int col = 0; col < objectMap[row].length; col++) {
                int object = objectMap[row][col];
                if (object >= 16 && object < 48) {
                    int[] indexes = getTileParts(object);
                    int[] imageDimensions = getImageDim(
                        objectImages[indexes[0]][indexes[1]], 4
                    );
                    int width = imageDimensions[0] / 64;
                    int height = imageDimensions[1] / 64;
                    System.out.println(object + " " + width + " " + height);
                    updateHitboxes(col, row, width, height - 1);
                }
            }
        }
        array[16][13] = false;
        array[18][31] = false;
        return array;
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

                int wetTile = wetTilesMap[row][col];
                if (wetTile >= 0 && isFarmland(map[row][col])) {
                    offScreenBuffer.drawImage(
                        objectImages[3][wetTile], 
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
        File cropFile = new File("data/crop.txt");

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
                int[][] defaultInventory = new int[3][9];
                resetArray(defaultInventory);
                defaultInventory[2][0] = 3201;
                defaultInventory[2][1] = 3301;
                defaultInventory[2][2] = 1610;
                saveFile(inventoryFile, defaultInventory);
                inventory = defaultInventory;
            } else {
                int[][] loadedInventory = loadMap(inventoryFile);
                inventory = loadedInventory;
            }
            if (mapFile.length() == 0) {
                int[][] defaultMap = getDefaultMap();
                map = defaultMap;
                saveFile(mapFile, map);
            } else {
                int[][] loadedMap = loadMap(mapFile);
                map = loadedMap;
            }
            if (objectFile.length() == 0) {
                int[][] defaultMap = getDefaultObjectMap();
                objectsMap = defaultMap;
                saveFile(objectFile, objectsMap);
            } else {
                int[][] loadedObjectsMap = loadMap(objectFile);
                objectsMap = loadedObjectsMap;
            }
            if (storeFile.length() == 0) {
                int[][] defaultStore = getDefaultStore();
                storeItems = defaultStore;
                saveFile(storeFile, storeItems);
            } else {
                int[][] loadedStoreItems = loadMap(storeFile);
                storeItems = loadedStoreItems;
            }
            if (cropFile.length() == 0) {
                cropDaysMap = new int[map.length][map[0].length];
                resetArray(cropDaysMap);
                saveFile(cropFile, cropDaysMap);
            } else {
                int[][] loadedCropDays = loadMap(cropFile);
                cropDaysMap = loadedCropDays;
            }
            hotbar = inventory[2];
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
            // houseHitboxes = new boolean[][]{
            //     {true, true, true, true, true, true, true, true, true, true, true, true},
            //     {true, true, true, true, true, true, true, true, true, true, true, true},
            //     {true, true, true, true, true, true, true, true, true, true, true, true},
            //     {true, false, false, false, true, true, true, true, true, true, true, true},
            //     {true, false, false, false, true, true, true, true, true, true, true, true},
            //     {true, false, false, true, true, true, true, true, true, true, true, true},
            //     {true, true, false, true, true, true, true, true, true, true, true, true},
            //     {true, true, false, true, true, true, true, true, true, true, true, true},
            //     {true, true, false, true, true, true, true, true, true, true, true, true},
            // };
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
            resetArray(wetTilesMap);
            resetArray(sellInventory);

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

            loadAudio();
            playNewAudio(0);
            hitboxes = getHitboxes();
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
        if (state.equals("game") || state.equals("inv") || state.equals("sell")) {
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
                drawInventory(g, 0);
                drawDraggedItem(g);
            } else if (state.equals("sell")) {
                drawSell(g);
                drawInventory(g, 100);
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
            drawEnd(g);
            cameraLocation.setText("");
            playerLocation.setText("");
        } else if (state.equals("title")) {
            drawTitle(g);
            if (isCreditsOpen) {
                drawCredits(g);
            } else if (isInstructionsOpen) {
                drawInstructions(g);
            }
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

            System.out.println("pressed at " + x + ", " + y);
            System.out.println("pressed tile: " + tileX + ", " + tileY);
            System.out.println(
                "crop day: " + cropDaysMap[tileY][tileX]
            );
            if (tileY > 0 && objectsMap[tileY - 1][tileX] % 100 == 3) {
                System.out.println("crop can be picked up");
                playSoundEffect(6);
                updateCropHarvest(tileX, tileY);
            } else if (item == 32 || item == 34 || item == 35 || item == 36) {
                System.out.println("hoe tool");
                startActionAnimation("hoeing");
                playSoundEffect(3);
                updateTile(tileX, tileY, item);
            } else if (item == 33 || item == 37 || item == 38 || item == 39) {
                System.out.println("water bucket");
                startActionAnimation("watering");
                playSoundEffect(4);
                updateTile(tileX, tileY, item);
            } else if (item >= 16 && item <= 27) {
                System.out.println("crop");
                playSoundEffect(5);
                updateCropPlaced(tileX, tileY);
            }
        } else if (state.equals("inv")) {
            int scalar = 3;
            int[][][] slotPositions = getInventorySlotPositions(0);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = slotPositions[row][col][0];
                    int slotY = slotPositions[row][col][1];
                    if (
                        actualX >= slotX && actualX < slotX + 16 * scalar &&
                        actualY >= slotY && actualY < slotY + 16 * scalar
                    ) {
                        if (inventory[row][col] < 0) {
                            continue;
                        }
                        draggedItemRow = row;
                        draggedItemCol = col;
                        draggedItem = inventory[row][col];
                        inventory[row][col] = -2;
                        invX = actualX;
                        invY = actualY;
                        return;
                    }
                }
            }
        } else if (state.equals("sell")) {
            int scalar = 3;
            int[][][] slotPositions = getInventorySlotPositions(100);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = slotPositions[row][col][0];
                    int slotY = slotPositions[row][col][1];
                    if (
                        actualX >= slotX && actualX < slotX + 16 * scalar &&
                        actualY >= slotY && actualY < slotY + 16 * scalar &&
                        inventory[row][col] >= 0 && inventory[row][col] < 3200
                    ) {
                        System.out.println(col + " " + row);
                        int item = inventory[row][col];
                        int[] sellRowOrder = {0, 1};
                        int[] coords = calculateNextSlot(
                            sellInventory, item, sellRowOrder
                        );
                        if (coords[0] != -1) {
                            inventory[row][col] = -1;
                        }
                        playSoundEffect(7);
                    }
                }
            }
            for (int i = 0; i < 18; i++) {
                int row = i / 9;
                int col = i % 9;
                if (
                    actualX >= slotPositions[0][col][0] && 
                    actualX < slotPositions[0][col][0] + 16 * scalar &&
                    actualY >= 120 + row * 20 * scalar && 
                    actualY < 120 + 16 * scalar + row * 20 * scalar &&
                    sellInventory[row][col] >= 0 && sellInventory[row][col] < 3200
                ) {
                    System.out.println(col + " " + row);
                    int item = sellInventory[row][col];

                    int[] invRowOrder = { 2, 0, 1 };
                    int[] coords = calculateNextSlot(
                        inventory, item, invRowOrder
                    );

                    if (coords[0] != -1) {
                        sellInventory[row][col] = -1;
                    }
                }
            }
            // also checks if they click smth in the sell place to get it back
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
        } else if (state.equals("end")) {
            int[] buttonDimensions = getImageDim(uiImages[6], 3);
            buttonDimensions[1] -= 20;
            if (
                actualX >= 150 && actualX <= 150 + buttonDimensions[0] &&
                actualY >= 360 && actualY <= 360 + buttonDimensions[1]
            ) {
                resetDay();
                state = "house";
                playNewAudio(1);
            } else if (
                actualX >= 325 && actualX <= 325 + buttonDimensions[0] &&
                actualY >= 360 && actualY <= 360 + buttonDimensions[1]
            ) {
                resetDay();
                state = "title";
                playNewAudio(0);
            }
        } else if (state.equals("title")) {
            int[] buttonDimensions = getImageDim(uiImages[6], 5);
            int[] otherButtonDim = getImageDim(uiImages[9], 3);
            if (
                actualX >= 264 && actualX <= 264 + buttonDimensions[0] && 
                actualY >= 350 && actualY <= 350 + buttonDimensions[1]
            ) {
                System.out.println("play button");
                state = "house";
                playNewAudio(1);
            } else if (
                actualX >= 100 && actualX <= 100 + otherButtonDim[0] &&
                actualY >= 376 && actualY <= 376 + otherButtonDim[1]
            ) {
                System.out.println("credits");
                isCreditsOpen = true;
            } else if (
                actualX >= 524 && actualX <= 524 + otherButtonDim[0] &&
                actualY >= 376 && actualY <= 376 + otherButtonDim[1]
            ) {
                System.out.println("instructions");
                isInstructionsOpen = true;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (state.equals("inv") && draggedItemRow != -1) {
            int actualX = e.getX();
            int actualY = e.getY();
            int[][][] slotPositions = getInventorySlotPositions(0);
            int scalar = 3;

            int targetRow = -1;
            int targetCol = -1;
            
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = slotPositions[row][col][0];
                    int slotY = slotPositions[row][col][1];
                    
                    if (actualX >= slotX && actualX < slotX + 16 * scalar &&
                        actualY >= slotY && actualY < slotY + 16 * scalar) {
                        targetRow = row;
                        targetCol = col;
                        break;
                    }
                }
                if (targetRow != -1) {
                    break;
                }
            }
            System.out.println("Dragged from: " + draggedItemRow + ", " + draggedItemCol);
            System.out.println("Target: " + targetRow + ", " + targetCol);

            if (targetRow == draggedItemRow && targetCol == draggedItemCol) {
                inventory[draggedItemRow][draggedItemCol] = draggedItem;
            } else if (targetRow != -1 && targetCol != -1) {
                int targetItem = inventory[targetRow][targetCol];
                if (targetItem >= 0 && canStack(draggedItem, targetItem)) {
                    int[] firstInfo = getItemInfo(draggedItem);
                    int[] secondInfo = getItemInfo(targetItem);
                    
                    int itemType = firstInfo[0];
                    int totalQuantity = firstInfo[1] + secondInfo[1];
                    inventory[targetRow][targetCol] = itemType * 100 + totalQuantity;
                    inventory[draggedItemRow][draggedItemCol] = -1;
                } else {
                    inventory[targetRow][targetCol] = draggedItem;
                    
                    if (targetItem >= 0) {
                        inventory[draggedItemRow][draggedItemCol] = targetItem;
                    } else {
                        inventory[draggedItemRow][draggedItemCol] = -1;
                        draggedItemCol = -1;
                        draggedItem = -1;
                    }
                }
            } else {
                inventory[draggedItemRow][draggedItemCol] = draggedItem;
            }
            draggedItemRow = -1;
            draggedItemCol = -1;
            draggedItem = -1;
        }
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
        if (state.equals("inv") && draggedItemRow != -1) {
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
            } else if (state.equals("game")) {
                if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_9) {
                    hotbarSlot = Integer.parseInt(KeyEvent.getKeyText(key)) - 1;
                } else if (key == KeyEvent.VK_L) {
                    // test key for now
                    System.out.println("clicked l");
                    updateCropGrowth();
                }
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
            } else if (state.equals("sell")) {
                state = "game";
                calculateEarnedMoney();
            } else if (state.equals("end")) {
                state = "title";
                playNewAudio(0);
            } else if (state.equals("title")) {
                if (isCreditsOpen) {
                    isCreditsOpen = false;
                } else if (isInstructionsOpen) {
                    instructionPage = 0;
                    isInstructionsOpen = false;
                }
            }
        }
        if (state.equals("title")) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) { 
                if (instructionPage >= 1) {
                    instructionPage -= 1;
                }
            } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                if (instructionPage < 5) {
                    instructionPage += 1;
                }
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
            } else if (state.equals("end")) {
                updateMoneyAnimation();
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
