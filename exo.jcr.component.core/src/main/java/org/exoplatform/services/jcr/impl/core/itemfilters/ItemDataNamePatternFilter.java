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
import org.exoplatform.services.jcr.impl.core.JCRName;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id$
 */

public class ItemDataNamePatternFilter implements ItemDataFilter
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ItemDataNamePatternFilter");

   private final SessionImpl session;

   private final List<String> expressions = new ArrayList<String>();

   public ItemDataNamePatternFilter(String namePattern, SessionImpl session) throws NamespaceException,
      RepositoryException
   {
      this.session = session;

      StringTokenizer parser = new StringTokenizer(namePattern, "|");
      while (parser.hasMoreTokens())
      {
         String token = parser.nextToken();

         expressions.add(token.trim());
      }
   }

   public boolean accept(ItemData item)
   {
      try
      {
         JCRName name = session.getLocationFactory().createJCRName(item.getQPath().getName());
         for (String expr : expressions)
         {
            if (estimate(name.getAsString(), expr))
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

   private boolean estimate(String name, String expr)
   {
      if (expr.indexOf("*") == -1)
      {
         return name.equals(expr);
      }

      String regexp = expr.replaceAll("\\*", ".*");
      return Pattern.compile(regexp).matcher(name).matches();
   }

   /**
    * @see org.exoplatform.services.jcr.impl.core.itemfilters.ItemDataFilter#accept(java.util.List)
    */
   @Override
   public List<? extends ItemData> accept(List<? extends ItemData> item)
   {
      // TODO Auto-generated method stub
      return null;
   }

}
