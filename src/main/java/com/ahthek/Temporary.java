package com.ahthek;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
// import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Temporary {
  public static long convertDurationToMillis(String duration) {
    String[] parts = duration.split(":");
    long hours = Long.parseLong(parts[0]);
    long minutes = Long.parseLong(parts[1]);
    String[] secondsAndMillis = parts[2].split("\\.");
    long seconds = Long.parseLong(secondsAndMillis[0]);
    long milliseconds = Long.parseLong(secondsAndMillis[1]);
    // String timeOnly = "00:23:11.967";
    // long millisDuration = convertDurationToMillis(timeOnly);
    // System.out.println("Milliseconds for duration " + timeOnly + ": " +
    // millisDuration);
    return hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds;
  }

  public static void main(String[] args) throws IOException {
    String[][] arr = {
      {"00:44:37.333", "01:07:49.267"},
      {"01:15:10.833", "01:37:46.800"},
      {"01:44:43.433", "02:07:25.400"}
    };
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    // LocalTime start = LocalTime.parse(arr[0][0], formatter);
    // LocalTime end = LocalTime.parse(arr[0][1], formatter);
    // Duration duration = Duration.between(start, end);
    // System.out.println(duration.toString());
    /* */
    long[] result = new long[arr.length];
    long[] hasil = new long[arr.length];
    long sum = 0;
    long jumlah = 0;
    for (int i = 0; i < arr.length; i++) {
      LocalTime start = LocalTime.parse(arr[i][0], formatter);
      LocalTime end = LocalTime.parse(arr[i][1], formatter);
      Duration duration = Duration.between(start, end);
      // System.out.println(duration.toMillis());
      long mula = convertDurationToMillis(arr[i][0]);
      long tamat = convertDurationToMillis(arr[i][1]);
      long beza = tamat - mula;
      jumlah += beza;
      sum += duration.toMillis();
      result[i] = sum;
      hasil[i] = jumlah;
    }
    for (long val: result) {
      System.out.print(val + " ");
    }
    System.out.println("\n");
    for (long val: hasil) {
      System.out.print(val + " ");
    }
    // System.out.println(convertDurationToMillis(arr[0][0]));
    /*
    String mltpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\UFC 302 Early Prelims.mlt";
    String txtpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\UFC 302 Early Prelims.txt";
    String chapterpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\UFC_302_Early_Prelims.ffmetadata";
    String cmdpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\UFC_302_Early_Prelims.cmd";
    String mylistpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\mylist.txt";
    Files.deleteIfExists(Paths.get(chapterpath));
    // File chapterFile = new File(chapterpath);

    ArrayList<ArrayList<String>> mainArrayList = new ArrayList<ArrayList<String>>();
    ArrayList<String> chapterArrayList = new ArrayList<String>();
    ArrayList<String> timestamp = new ArrayList<String>();
    // List<String> uniqueTimestamp = new List<>();
    try (BufferedReader br = new BufferedReader(new FileReader(new File(txtpath)))) {
      
      String line;
      while ((line = br.readLine()) != null) {
        chapterArrayList.add(line);
        // chapterArrayList.add("Start");
        // chapterArrayList.add("end");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    // System.out.println(chapterArrayList);


    ArrayList<String[]> inOut = new ArrayList<>();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      File file = new File(mltpath);
      Document doc = builder.parse(file);
      doc.getDocumentElement().normalize();
      Element root = doc.getDocumentElement();
      NodeList nodeList = root.getElementsByTagName("playlist");
      for (int i = 0; i < nodeList.getLength(); i++) {
        if (nodeList.item(i).getAttributes().getNamedItem("id").getNodeValue().equals("playlist0")) {
          NodeList nodeList2 = ((Element) nodeList.item(i)).getElementsByTagName("entry");
          for (int j = 0; j < nodeList2.getLength(); j++) {
            Element entryElement = (Element) nodeList2.item(j);
            String[] myArr = {entryElement.getAttribute("in"), entryElement.getAttribute("out")};
            inOut.add(myArr);
            // System.out.println("in=" + entryElement.getAttribute("in") + "\tout=" + entryElement.getAttribute("out"));
          }
        }
      }

      // create the mylist.txt before creating the cmd bat file so that we can have it ready
      // to be concatenated
      Files.deleteIfExists(Paths.get(mylistpath));
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(mylistpath, true))) {
        for (int i = 0; i < inOut.size(); i++) {
          bw.write("file '" +  "UFC_302_Early_Prelims_" + String.format("%02d", i + 1) + ".mkv'");
          if (i != inOut.size() - 1) {
            bw.newLine();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      Files.deleteIfExists(Paths.get(cmdpath));
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(cmdpath, true))) {
        for (int i = 0; i < inOut.size(); i++) {
          String[] item = inOut.get(i);
          // System.out.println("in=" + item[0] + "\tout=" + item[1]);
          StringBuilder cmd = new StringBuilder();
          cmd.append("ffmpeg -y -hide_banner -ss ");
          cmd.append(item[0]);
          cmd.append(" -to ");
          cmd.append(item[1]);
          cmd.append(" -i ");
          cmd.append("\"UFC 302 Early Prelims.mkv\"");
          cmd.append(" -c:v libx265 -c:a aac ");
          cmd.append("\"UFC_302_Early_Prelims_" + String.format("%02d", i + 1) + ".mkv\"");
          bw.write(cmd.toString());
          bw.newLine();
        }
        bw.write("ffmpeg -f concat -safe 0 -i mylist.txt -c copy UFC_302_Early_Prelims.mkv");
        //to concat and add metadata in one go
        //ffmpeg -f concat -safe 0 -i filelist.txt -i metadata.txt -map_metadata 1 -c copy output.mkv
      } catch (IOException e) {
        e.printStackTrace();
      }

      

      NodeList tractorNodeList = root.getElementsByTagName("tractor");
      for (int i = 0; i < tractorNodeList.getLength(); i++) {
        NodeList propertiesNodeList = ((Element) tractorNodeList.item(i)).getElementsByTagName("properties");
        for (int j = 0; j < propertiesNodeList.getLength(); j++) {
          NodeList propertyNodeList = ((Element) propertiesNodeList.item(j)).getElementsByTagName("property");
          for (int k = 0; k < propertyNodeList.getLength(); k++) {
            Element propertyElement = (Element) propertyNodeList.item(k);
            if (propertyElement.getAttribute("name").equals("start")) {
              // System.out.println("start=" + propertyElement.getTextContent());
              timestamp.add(propertyElement.getTextContent());
            }
            if (propertyElement.getAttribute("name").equals("end")) {
              // System.out.println("end=" + propertyElement.getTextContent());
              timestamp.add(propertyElement.getTextContent());
            }
          }
        }
      }
      // uniqueTimestamp = timestamp.stream().distinct().collect(Collectors.toList());
      // System.out.println(uniqueTimestamp);
    } catch (Exception e) {
      e.printStackTrace();
    }
    List<String> uniqueTimestamp =timestamp.stream().distinct().collect(Collectors.toList());
    for (int a = 0; a < chapterArrayList.size(); a++) {
      ArrayList<String> tempah = new ArrayList<String>();
      tempah.add(chapterArrayList.get(a));
      tempah.add(uniqueTimestamp.get(a));
      tempah.add(uniqueTimestamp.get(a + 1));
      mainArrayList.add(tempah);
    }
    System.out.println(mainArrayList);

    try (BufferedWriter bw = new BufferedWriter(new FileWriter(chapterpath, true))) {
      bw.write(";FFMETADATA1");
      // bw.newLine();
      for (ArrayList<String> chap: mainArrayList) {
        bw.newLine();
        bw.newLine();
        bw.write("[CHAPTER]");
        bw.newLine();
        bw.write("TIMEBASE=1/1000");
        bw.newLine();
        bw.write("START=" + String.valueOf(convertDurationToMillis(chap.get(1))));
        bw.newLine();
        bw.write("END=" + String.valueOf(convertDurationToMillis(chap.get(2)) - 1));
        bw.newLine();
        bw.write("title=" + chap.get(0));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
       */
  }
}

/*
 * ;FFMETADATA1 title=My Video Title artist=My Name
 * 
 * [CHAPTER] TIMEBASE=1/1000 START=0 END=60000 title=Chapter 1 Title
 * 
 * [CHAPTER] TIMEBASE=1/1000 START=60001 END=120000 title=Chapter 2 Title
 * 
 * [CHAPTER] TIMEBASE=1/1000 START=120001 END=180000 title=Chapter 3 Title
 * 
 */