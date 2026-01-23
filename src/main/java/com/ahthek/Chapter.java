package com.ahthek;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Chapter {
  private final StringProperty start;
  private final StringProperty end;
  private final StringProperty description;

  public Chapter(String start, String end, String description) {
    this.start = new SimpleStringProperty(start);
    this.end = new SimpleStringProperty(end);
    this.description = new SimpleStringProperty(description);
  }
/* 
  public String getStart() {
    return start.get();
  }
 */
  public StringProperty startProperty() {
    return start;
  }
/* 
  public String getEnd() {
    return end.get();
  }
 */
  public StringProperty endProperty() {
    return end;
  }
/* 
  public String getDescription() {
    return description.get();
  }
 */
  public StringProperty descriptionProperty() {
    return description;
  }
}
