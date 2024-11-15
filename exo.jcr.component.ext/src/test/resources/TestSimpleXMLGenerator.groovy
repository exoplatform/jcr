package org.exoplatform.script.groovy.test

import groovy.xml.MarkupBuilder
import org.exoplatform.services.script.groovy.Book
public class SimpleXMLGenerator {
  
  public void generateXML (Book b) {
    def xmlBuilder = new MarkupBuilder()
    xmlBuilder.books() {
      book() {
        title(b.getTitle())
        author(b.getAuthor())
        price(b.getPrice())
        ISDN(b.getIsdn())
      }
    }
  }
}