package Almog;

import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class VA {

    boolean uniqueness;
    private final int maxDepth;
    private final int maxPerPage;
    ReentrantLock lock = new ReentrantLock();
    private final ConcurrentHashMap<String, ArrayList<Integer>> concurrentHashMap;

    public VA(int maxPerPage, int maxDepth, boolean uniqueness) {
        this.maxPerPage = maxPerPage;
        this.maxDepth = maxDepth;
        this.uniqueness = uniqueness;
        concurrentHashMap = new ConcurrentHashMap<>();
    }

    private void createFile(String URL, String depth){

        String filename = URL + ".html";

        filename = filename.replaceAll("[^a-zA-Z0-9.\\-()]", "_");

        try {
            File myObj = new File("files/" + depth + File.separator + filename);

            myObj.getParentFile().mkdirs();

            if (!myObj.createNewFile()) {
                System.out.println("File already exists.");
            }

            PrintWriter outputFile = new PrintWriter("files/" + depth + File.separator + filename);

            URL url = new URL(URL);

            URLConnection con = url.openConnection();
            InputStream is = con.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line = null;

            while (true) {
                try {
                    if ((line = br.readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                outputFile.println(line);
            }

            br.close();
            outputFile.close();


        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    public void getUrlLinks(String URL, int depth) {

        // For main URL only (no locks needed as no threads were created)
        if(depth == 0){
            ArrayList<Integer> depthList = new ArrayList<>();
            depthList.add(depth);
            concurrentHashMap.put(URL, depthList);
        }

        // Crawl for sons urls
        int sons_counter = 0;

        if(depth < maxDepth)
        {
            try {

                Document document = Jsoup.connect(URL).get();
                Elements linksOnPage = document.select("a[href]");

                for (Element page : linksOnPage) {

                    if(sons_counter == maxPerPage){
                        break;
                    }

                    String sonURL = page.attr("abs:href");
                    if(sonURL.equals("")){
                        continue;
                    }

                    int sonDepth = depth + 1;
                    ArrayList<Integer> son_list;

                    lock.lock();

                    if(!concurrentHashMap.containsKey(sonURL)) {
                        son_list = new ArrayList<>();
                        son_list.add(sonDepth);
                        concurrentHashMap.put(sonURL, son_list);
                        sons_counter++;

                    } else
                    {
                        son_list = concurrentHashMap.get(sonURL);
                        if(uniqueness || son_list.contains(sonDepth))
                        {
                            lock.unlock();
                            continue;
                        }

                        if(!son_list.contains(sonDepth)){
                            son_list.add(sonDepth);
                            concurrentHashMap.put(sonURL, son_list);
                            sons_counter++;
                        }
                    }

                    lock.unlock();

                    Runnable runnable = () -> getUrlLinks(sonURL, sonDepth);
                    Thread thread = new Thread(runnable);
                    thread.start();
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                System.err.println("Problem with '" + URL + "': " + e.getMessage());
            }

        }
    }

    public void createFiles(){
        concurrentHashMap.forEach((url,urlDepthsList) -> {
            for(int i = 0; i < urlDepthsList.size(); i++){
                int finalI = i;
                Runnable runnable = () -> createFile(url, String.valueOf(urlDepthsList.get(finalI)));;
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });
    }

    public static void main(String[] args) {

        // URL
        String url = JOptionPane.showInputDialog("Please enter url");
        if(url == null) return;
        UrlValidator urlValidator = new UrlValidator();
        while(!urlValidator.isValid(url)){
            url = JOptionPane.showInputDialog("Please enter a valid url :)");
            if(url == null) return;
        }

        // MAX PER PAGE
        String ans = JOptionPane.showInputDialog("Please enter max links per page");
        if(ans == null)  return;
        while(!StringUtil.isNumeric(ans) || Integer.parseInt(ans) < 0){
            ans = JOptionPane.showInputDialog("Please enter max links per page, a valid NUMBER, bigger than 0 :)");
            if(ans == null)  return;
        }
        int maxPerPage = Integer.parseInt(ans);

        // DEPTH FACTOR
        ans = JOptionPane.showInputDialog("Please enter depth factor");
        if(ans == null)  return;
        while(!StringUtil.isNumeric(ans) || Integer.parseInt(ans) < 0){
            ans = JOptionPane.showInputDialog("Please enter depth factor, a valid NUMBER, bigger than 0 :)");
            if(ans == null)  return;
        }
        int maxDepth = Integer.parseInt(ans);

        // UNIQUENESS
        boolean uniqueness;
        int answer = JOptionPane.showOptionDialog(null,
                "Keep files uniqueness?",
                "Files choice ?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, null, null);
        if(answer == -1) return;
        uniqueness = answer == JOptionPane.YES_OPTION;

        // RUN
        VA va = new VA(maxPerPage, maxDepth, uniqueness);
        va.getUrlLinks(url, 0);

        va.createFiles();

        // my site for testing duplicates
        //https://almogb001.wixsite.com/my-site", 0);
    }

}
