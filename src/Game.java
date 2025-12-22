import java.util.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

public class Game {
    public Game() {

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
        File general = new File("src/data/general.txt");
        File inventory = new File("src/data/inv.txt");
        File map = new File("src/data/map.txt");

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                return false;
            }
        }
        boolean isGeneralCreated = createFile(general);
        boolean isInvCreated = createFile(inventory);
        boolean isMapCreated = createFile(map);
        return isGeneralCreated && isInvCreated && isMapCreated;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("hello world!");
        // do smth if it fails or smth or put this in constructor?? idk
        System.out.println(initialize());
    }
}
