/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: PatternQPathEntryFilter.java 34360 19.05.2011 skarpenko $
 */
public class PatternQPathEntryFilter implements QPathEntryFilter
{
   private QPathEntry entry;

   private boolean namespaceIsWildcard;

   private boolean localNameHasWildcard;

   private Pattern localNamePattern = null;

   public PatternQPathEntryFilter()
   {
   }

   public PatternQPathEntryFilter(QPathEntry entry)
   {
      this.entry = entry;
      namespaceIsWildcard = entry.getNamespace().equals("*");
      localNameHasWildcard = entry.getName().indexOf('*') != -1;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isExactName()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public QPathEntry getQPathEntry()
   {
      return entry;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean accept(ItemData item)
   {
      if (localNameHasWildcard && localNamePattern == null)
      {
         initializePattern();
      }

      QPathEntry itemEntry = item.getQPath().getEntries()[item.getQPath().getDepth()];

      if (entry.getIndex() != -1 && entry.getIndex() != itemEntry.getIndex())
      {
         return false;
      }

      if (!namespaceIsWildcard && !entry.getNamespace().equals(itemEntry.getNamespace()))
      {
         return false;
      }

      if (!localNameHasWildcard)
      {
         return entry.getName().equals(itemEntry.getName());
      }
      else
      {
         return localNamePattern.matcher(itemEntry.getName()).matches();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<? extends ItemData> accept(List<? extends ItemData> itemData)
   {

      List<ItemData> result = new ArrayList<ItemData>();
      for (int i = 0; i < itemData.size(); i++)
      {
         if (accept(itemData.get(i)))
         {
            result.add(itemData.get(i));
         }
      }

      return result;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(Object o)
   {
      if (o == this)
         return true;

      if (o == null)
         return false;

      if (o instanceof QPathEntryFilter)
      {

         QPathEntryFilter that = (QPathEntryFilter)o;
         return this.getQPathEntry().equals(that.getQPathEntry());
      }
      return false;
   }

   private void initializePattern()
   {
      StringBuilder sb = new StringBuilder();
      for (char c : entry.getName().toCharArray())
      {
         switch (c)
         {
            case '*' :
               sb.append(".*");
               break;
            case '\\' :
            case ':' :
            case '+' :
            case '-' :
            case '=' :
            case '.' :
            case ',' :
            case '[' :
            case ']' :
            case '{' :
            case '}' :
            case '(' :
            case ')' :
            case '^' :
            case '&' :
            case '$' :
            case '?' :
            case '<' :
            case '>' :
            case '!' :
               sb.append('\\');
            default :
               sb.append(c);
         }
      }
      localNamePattern = Pattern.compile(sb.toString());
   }

   @Override
   public int hashCode()
   {
      return entry.hashCode();
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException
   {
      byte[] buf = entry.getNamespace().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      buf = entry.getName().getBytes(Constants.DEFAULT_ENCODING);
      out.writeInt(buf.length);
      out.write(buf);

      out.writeInt(entry.getIndex());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      byte[] buf = new byte[in.readInt()];
      in.readFully(buf);
      String namespace = new String(buf, Constants.DEFAULT_ENCODING);
      namespaceIsWildcard = namespace.equals("*");

      buf = new byte[in.readInt()];
      in.readFully(buf);
      String localName = new String(buf, Constants.DEFAULT_ENCODING);
      localNameHasWildcard = localName.indexOf('*') != -1;

      int index = in.readInt();
      entry = new PatternQPathEntry(namespace, localName, index);
   }
}
