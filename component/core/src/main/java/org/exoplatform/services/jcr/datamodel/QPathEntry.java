/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.datamodel;

/**
 * QPathEntry it's a QPath element entry. Extends InternalQName and contains index value.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: QPathEntry.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class QPathEntry extends InternalQName implements Comparable<QPathEntry>
{

   /**
    * QPath index.
    */
   private final int index;

   /**
    * QPathEntry constructor.
    * 
    * @param qName
    *          - InternalQName (full qualified name)
    * @param index
    *          - Item index
    */
   public QPathEntry(InternalQName qName, int index)
   {
      super(qName.getNamespace(), qName.getName());
      this.index = index > 0 ? index : 1;
   }

   /**
    * QPathEntry constructor.
    * 
    * @param namespace
    *          - namespace URI
    * @param name
    *          - Item name
    * @param index
    *          - Item index
    */
   public QPathEntry(String namespace, String name, int index)
   {
      super(namespace, name);
      this.index = index > 0 ? index : 1;
   }

   /**
    * Parse QPath entry in form of eXo-JCR names conversion string
    * <br/><code>[name_space]item_name:item_index</code>.
    * 
    * <br/> E.g. <code>[http://www.jcp.org/jcr/nt/1.0]system:1</code>.
    * 
    * @param qEntry
    *          - String to be parsed
    * @return InternalQName instance
    * @throws IllegalNameException
    *           if String contains invalid QName
    * @throws NumberFormatException
    *           if String contains invalid index
    */
   public static QPathEntry parse(String qEntry) throws IllegalNameException, NumberFormatException
   {

      int delimIndex = qEntry.lastIndexOf(QPath.PREFIX_DELIMITER);
      String qnameString = qEntry.substring(0, delimIndex);
      String indexString = qEntry.substring(delimIndex + 1);

      InternalQName qname = InternalQName.parse(qnameString);
      return new QPathEntry(qname, Integer.valueOf(indexString));
   }

   /**
    * Return Item path index.
    * 
    * @return item index
    */
   public int getIndex()
   {
      return index;
   }

   /**
    * Tells if the given entry is same.
    * 
    * @param obj
    *          - another QPathEntry
    * @return - boolean, true if is same
    */
   public boolean isSame(QPathEntry obj)
   {
      if (super.equals(obj))
         return index == obj.getIndex();

      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getAsString()
   {
      return getAsString(false);
   }

   /**
    * Return entry textual representation.
    * 
    * @return - if showIndex=false it's a string without index
    */
   public String getAsString(boolean showIndex)
   {
      return super.getAsString() + (showIndex ? QPath.PREFIX_DELIMITER + this.index : "");
   }

   /**
    * {@inheritDoc}
    */
   public int compareTo(QPathEntry compare)
   {
      int result = 0;

      if (this.isSame(compare))
         return result;
      result = namespace.compareTo(compare.namespace);
      if (result == 0)
      {
         result = name.compareTo(compare.name);
         if (result == 0)
            result = index - compare.index;
      }
      return result;
   }

   /**
    * {@inheritDoc}
    */
   protected String asString()
   {
      return getAsString(true);
   }

}
