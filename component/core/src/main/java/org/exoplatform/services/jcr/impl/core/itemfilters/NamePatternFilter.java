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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: NamePatternFilter.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class NamePatternFilter implements ItemFilter
{

   private ArrayList expressions;

   public NamePatternFilter(String namePattern)
   {
      expressions = new ArrayList();
      StringTokenizer parser = new StringTokenizer(namePattern, "|");
      while (parser.hasMoreTokens())
      {
         String token = parser.nextToken();
         expressions.add(token.trim());
      }
   }

   public boolean accept(Item item) throws RepositoryException
   {
      String name = item.getName();
      // boolean result = false;
      for (int i = 0; i < expressions.size(); i++)
      {
         String expr = (String)expressions.get(i);
         if (estimate(name, expr))
            return true;
      }
      return false;
   }

   private boolean estimate(String name, String expr)
   {
      if (expr.indexOf("*") == -1)
         return name.equals(expr);
      String regexp = expr.replaceAll("\\*", ".*");
      return Pattern.compile(regexp).matcher(name).matches();
   }

}
