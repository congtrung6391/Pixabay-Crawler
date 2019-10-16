package webcrawler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class Crawler implements ActionListener, Runnable {
    private static JFrame mainFrame;

    private static JLabel headerLabel;

    private static JTextField dirText;
    private static JTextField keyText;
    private static JButton dirButton;
    private static JButton keyButton;
    private static JPanel dirPanel;
    private static JPanel keyPanel;
    private static JPanel statusPanel;
    private static JProgressBar progressBar;

    // Count the number of images will be crawled.
    private static int numimage = 0;
    private static int maximage = 0;
    // data in for crawling.
    private static String keyword;
    private static String dirToSaveImages;
    // 1 page means 100 images.
    // maximum pages can get in reality.
    final static int maxpage = 2;
    // maximum pages can get.
    private static int numpage = 0; //
    private static JSONArray listjson = new JSONArray();

    static Crawler _this;

    public Crawler() {
        _this = this;
    }

    /* set value of numpage variable. */
    static void setNumPage() throws IOException {
        /*
         * The title of each has the same kind (2,342(+) pictures of ...). The value of
         * numpage is the first number in title.
         */
        Document doc = Jsoup.connect("https://pixabay.com/images/search/" + keyword).userAgent("Mozilla").get();
        String title = doc.title();
        char[] titlechars = title.toCharArray();
        for (int i = 0; i < titlechars.length; ++i) {
            if (titlechars[i] <= '9' && titlechars[i] >= '0') {
                numpage = numpage * 10 + (int) (titlechars[i] - '0');
            } else if (titlechars[i] == ',') {
                continue;
            } else {
                break;
            }
        }
        maximage = numpage;
        numpage = (numpage % 100 == 0) ? (numpage / 100) : (numpage / 100 + 1);
        numpage = Math.min(numpage, maxpage);
        maximage = Math.min(maximage, numpage * 100);
    }

    /* Conver an interger to String */
    static String numToString(int num) {
        String strg = "";
        while (num > 0) {
            strg = (char) (num % 10 + (int) '0') + strg;
            num /= 10;
        }
        return strg;
    }

    /*
     * "linkpar" contains 2 links (*.jpg 1x *.jpg 2x). Method return the second
     * link.
     */
    static String fixLink(String linkpar) {
        String link = "";
        int begin = linkpar.indexOf(" ");
        int end = linkpar.lastIndexOf(" ");
        link = linkpar.substring(begin + 5, end);
        return link;
    }

    /*
     * Pages contain "data-lazy" attribute, so we cannot get link by "src" attribute
     * for all images. Instead, we can get "data-lazy-srcset" for images having
     * "data-lazy" and "srcset" or "src" for the other.
     */
    static String getLink(Element div) {
        Element element = div.getElementsByTag("img").first();
        String link = element.attr("data-lazy-srcset");
        if (link.length() == 0) {
            link = element.attr("srcset");
        }
        return fixLink(link);
    }

    /* Save image from "url" to file in the "keyword" directory */
    static void saveImage(String url) throws MalformedURLException, IOException {
        BufferedImage image = ImageIO.read(new URL(url));
        String imagetype = url.substring(url.length() - 3, url.length());
        try {
            File outputimage = new File(
                    dirToSaveImages + "/" + keyword + "/" + keyword + numToString(numimage) + imagetype);
            ImageIO.write(image, imagetype, outputimage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveLinkToJsonFile(JSONObject listjson) {
        try {
            // path to save list of image link
            String pathlinklist = dirToSaveImages + "/" + keyword + "/Links of image.json";
            FileWriter links = new FileWriter(pathlinklist, true);
            links.write(listjson.toJSONString());
            links.flush();
            links.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void updateProgress(int progress) {
        progressBar.setValue(progress);
    }

    /* Get images for the "idpage"th page */
    static void getImage(String url, int idpage) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla").get();
        Elements elements = doc.select("div.item");
        for (Element element : elements) {

            numimage++;
            saveImage(getLink(element));

            JSONObject linkDetails = new JSONObject();
            JSONObject linkJson = new JSONObject();
            linkDetails.put("Index of image", numToString(numimage));
            linkDetails.put("Link to image", getLink(element));
            linkJson.put(keyword, linkDetails);
            saveLinkToJsonFile(linkJson);

            System.out.println(numimage);
            int progress = numimage * 100 / maximage;
            updateProgress(progress);
        }
    }

    /* Creat directories and Set values for variables */
    static void prepareCrawl() throws IOException {
        try {
            Files.createDirectory(Paths.get(dirToSaveImages + "/" + keyword));
        } catch (Exception e) {
            System.out.println("Directory had already created.");
        }
        setNumPage();
        System.out.println(maximage);
    }

    public static void crawlImages() throws IOException {
        prepareCrawl();
        for (int idpage = 1; idpage <= Math.min(numpage, maxpage); ++idpage) {
            String linktocrawl = "https://pixabay.com/images/search/" + keyword + "/?pagi=" + idpage;
            getImage(linktocrawl, idpage);
        }
    }

    public static void prepareGUI() {
        mainFrame = new JFrame("Pixabay Crawler");
        mainFrame.setSize(500, 150);
        mainFrame.setLayout(new GridLayout(4, 1));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        headerLabel = new JLabel("", JLabel.CENTER);

        dirText = new JTextField("", 30);
        dirButton = new JButton("");
        dirPanel = new JPanel();
        dirPanel.setLayout(new FlowLayout());
        dirPanel.add(dirText);
        dirPanel.add(dirButton);

        keyText = new JTextField("", 30);
        keyButton = new JButton("");
        keyPanel = new JPanel();
        keyPanel.setLayout(new FlowLayout());
        keyPanel.add(keyText);
        keyPanel.add(keyButton);

        statusPanel = new JPanel();

        mainFrame.add(headerLabel);
        mainFrame.add(dirPanel);
        mainFrame.add(keyPanel);
        mainFrame.add(statusPanel);

        mainFrame.setVisible(true);
    }

    public static void showGUI() {
        headerLabel.setText("Welcome!!!");

        dirText.setText("Directory to save images");
        dirButton.setText("Open");

        final JFileChooser dialogDir = new JFileChooser();
        dialogDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (dialogDir.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File destinateDir = dialogDir.getSelectedFile();
                    dirToSaveImages = destinateDir.getAbsolutePath();
                    dirText.setText(dirToSaveImages);
                } else {
                    System.out.println("fuckk");
                }
            }
        });

        keyText.setText("");
        keyButton.setText("Crawl");
        keyButton.addActionListener(_this);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        JButton exitButton  = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.exit(0);
            }

        });
        statusPanel.add(progressBar);
        statusPanel.add(exitButton);

        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        new Thread(crawler).start();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                prepareGUI();
                showGUI();
            }
        });
    }

    public void actionPerformed(ActionEvent ae) {
        // signal the worker thread to get crackin
        synchronized (this) {
            notifyAll();
        }
    }

    // worker thread
    public void run() {
        while (true) {
            // wait for the signal from the GUI
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
            }
            // simulate some long-running process like parsing a large file
            keyword = keyText.getText();
            Crawler crawl = new Crawler();
            try {
                numimage = 0;
                progressBar.setValue(0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                crawl.crawlImages();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
