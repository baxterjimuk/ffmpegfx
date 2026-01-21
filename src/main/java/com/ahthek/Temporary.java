package com.ahthek;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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

  public static void main(String[] args) {
    String mltpath = "C:\\Users\\te\\Downloads\\other transfer\\UFC 302\\UFC 302 Early Prelims.mlt";
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
            System.out.println("in=" + entryElement.getAttribute("in") + "\tout=" + entryElement.getAttribute("out"));
          }
        }
      }

      NodeList tractorNodeList = root.getElementsByTagName("tractor");
      for (int i = 0; i < tractorNodeList.getLength(); i++) {
        NodeList propertiesNodeList = ((Element) tractorNodeList.item(i)).getElementsByTagName("properties");
        for (int j = 0; j < propertiesNodeList.getLength(); j++) {
          NodeList propertyNodeList = ((Element) propertiesNodeList.item(j)).getElementsByTagName("property");
          for (int k = 0; k < propertyNodeList.getLength(); k++) {
            Element propertyElement = (Element) propertyNodeList.item(k);
            if (propertyElement.getAttribute("name").equals("start")) {
              System.out.println("start=" + propertyElement.getTextContent());
            }
            if (propertyElement.getAttribute("name").equals("end")) {
              System.out.println("end=" + propertyElement.getTextContent());
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
