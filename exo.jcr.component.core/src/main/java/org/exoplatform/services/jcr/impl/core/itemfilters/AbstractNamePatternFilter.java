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
package org.exoplatform.services.jcr.impl.core.itemfilters;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: AbstractNamePatternFilter.java 2137 2010-03-25 15:31:56Z sergiykarpenko $
 */
public abstract class AbstractNamePatternFilter implements ItemDataFilter
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemDataNamePatternFilter");

   private final SessionImpl session;

   private final boolean getAllItems;

   private final List<QPathEntryFilter> subFilters;

   /**
    * Exact names.
    */
   private final QPathEntry[] exactNames;

   /**
    * Not parsed names with wildcard symbols.
    */
   private final String[] wildcardExpressions;

   /**
    * wildcardExpressions parsed into Pattern.
    */
   private Pattern[] expressionNamePatterns = null;

   public AbstractNamePatternFilter(String namePattern, SessionImpl session) throws RepositoryException
   {
      this.session = session;

      StringTokenizer parser = new StringTokenizer(namePattern, "|");
      boolean getAll = false;

      List<QPathEntryFilter> filters = new ArrayList<QPathEntryFilter>();
      List<QPathEntry> exactNamesList = new ArrayList<QPathEntry>();
      List<String> wildcardExpressionsList = new ArrayList<String>();

      while (parser.hasMoreTokens())
      {
         String token = parser.nextToken().trim();
         if (token.equals("*") || token.equals("*:*") || token.equals("*:*[*]"))
         {
            getAll = true;
            filters.clear();
            break;
         }
         if (token.startsWith(":"))
         {
            throw new RepositoryException("Name pattern can not start with colon.");
         }
         else
         {
            QPathEntry entry = null;
            if (isExactName(token))
            {

               JCRPath path = session.getLocationFactory().parseRelPath(token);
               QPathEntry[] entries = path.getInternalPath().getEntries();
               entry = entries[entries.length - 1];
               exactNamesList.add(entry);
               filters.add(new ExactQPathEntryFilter(entry));
            }
            else
            {
               entry = parsePatternQPathEntry(token, session);
               wildcardExpressionsList.add(token);
               filters.add(new PatternQPathEntryFilter(entry));
            }
         }
      }

      getAllItems = getAll;
      if (getAllItems)
      {
         subFilters = null;
         exactNames = null;
         wildcardExpressions = null;
      }
      else
      {
         subFilters = filters;
         exactNames = new QPathEntry[exactNamesList.size()];
         exactNamesList.toArray(exactNames);
         wildcardExpressions = new String[wildcardExpressionsList.size()];
         wildcardExpressionsList.toArray(wildcardExpressions);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean accept(ItemData item)
   {
      if (getAllItems)
      {
         return true;
      }

      if (expressionNamePatterns == null)
      {
         initializePatterns();
      }

      try
      {
         // check exact names for first
         QPathEntry itemEntry = item.getQPath().getEntries()[item.getQPath().getDepth()];

         for (QPathEntry entry : exactNames)
         {
            if (entry.equals(itemEntry))
            {
               return true;
            }
         }

         JCRPath.PathElement[] pathElements = session.getLocationFactory().createRelPath(new QPathEntry[]{itemEntry});
         // prefix:name[index]
         String name = pathElements[0].getAsString(true);

         for (Pattern pattern : expressionNamePatterns)
         {
            if (pattern.matcher(name).matches())
            {
               return true;
            }
         }
      }
      catch (RepositoryException e)
      {
         // if error - just log and don't accept it
         LOG.error("Cannot parse JCR name for " + item.getQPath().getAsString(), e);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<? extends ItemData> accept(List<? extends ItemData> itemData)
   {
      if (getAllItems)
      {
         return itemData;
      }

      if (expressionNamePatterns == null)
      {
         initializePatterns();
      }

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
    * Check does pattern looking all data.
    * @return
    */
   public boolean isLookingAllData()
   {
      return getAllItems;
   }

   /**
    * Get sub filters. Each Pattern name stored in own subfilter.
    * @return
    */
   public List<QPathEntryFilter> getQPathEntryFilters()
   {
      return subFilters;
   }

   private void initializePatterns()
   {
      expressionNamePatterns = new Pattern[wildcardExpressions.length];
      for (int i = 0; i < wildcardExpressions.length; i++)
      {
         StringBuilder sb = new StringBuilder();
         for (char c : wildcardExpressions[i].toCharArray())
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
               case '{' :
               case '}' :
               case '(' :
               case ')' :
               case '[' :
               case ']' :
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

         if (wildcardExpressions[i].indexOf('[') == -1)
         {
            sb.append("\\[.*\\]");
         }

         expressionNamePatterns[i] = Pattern.compile(sb.toString());
      }
   }

   /**
    * Parse QPathEntry from string namePattern. NamePattern may contain wildcard symbols in 
    * namespace and local name. And may not contain index, which means look all samename siblings.
    * So ordinary QPathEntry parser is not acceptable.
    * 
    * @param namePattern string pattern
    * @param session session used to fetch namespace URI
    * @return PatternQPathEntry
    * @throws RepositoryException if namePattern is malformed or there is some namespace problem.
    */
   private QPathEntry parsePatternQPathEntry(String namePattern, SessionImpl session) throws RepositoryException
   {
      int colonIndex = namePattern.indexOf(':');
      int bracketIndex = namePattern.lastIndexOf('[');

      String namespaceURI;
      String localName;
      int index = getDefaultIndex();
      if (bracketIndex != -1)
      {
         int rbracketIndex = namePattern.lastIndexOf(']');
         if (rbracketIndex < bracketIndex)
         {
            throw new RepositoryException("Malformed pattern expression " + namePattern);
         }
         index = Integer.parseInt(namePattern.substring(bracketIndex + 1, rbracketIndex));
      }

      if (colonIndex == -1)
      {
         namespaceURI = "";
         localName = (bracketIndex == -1) ? namePattern : namePattern.substring(0, bracketIndex);
      }
      else
      {
         String prefix = namePattern.substring(0, colonIndex);
         localName =
            (bracketIndex == -1) ? namePattern.substring(colonIndex + 1) : namePattern.substring(0, bracketIndex);

         if (prefix.indexOf("*") != -1)
         {
            namespaceURI = "*";
         }
         else
         {
            namespaceURI = session.getNamespaceURI(prefix);
         }
      }

      return new PatternQPathEntry(namespaceURI, localName, index);
   }

   /**
    * Check is token exact name.
    * @param token
    * @return 
    */
   protected abstract boolean isExactName(String token);

   /**
    * Returns index that will be used, if pattern has no index.
    * @return index
    */
   protected abstract int getDefaultIndex();
}
