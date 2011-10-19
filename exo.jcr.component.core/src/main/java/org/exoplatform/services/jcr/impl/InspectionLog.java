/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl;

import java.io.IOException;

/**
 * Interface of inspection log. It provides general methods for logging consistency issues.  
 * 
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 4.10.2011 skarpenko $
 *
 */
public interface InspectionLog
{
   // represents broken object state
   public enum InspectionStatus {
      ERR("Error"), WARN("Warning"), REINDEX("Reindex");
      final String text;

      InspectionStatus(String text)
      {
         this.text = text;
      }

      public String toString()
      {
         return text;
      }
   }

   /**
    * @return true, if inconsistency was found
    */
   boolean hasInconsistency();

   /**
    * @return true, if inconsistency or warning was found
    */
   boolean hasWarnings();

   /**
    * Adds comment to log
    */
   void logComment(String message) throws IOException;

   /**
    * Adds description to log
    */
   void logInspectionDescription(String description) throws IOException;

   /**
    * Adds detailed event to log, with issue found
    */
   void logBrokenObjectInfo(String brokenObjectDesc, String comment, InspectionStatus status) throws IOException;

   /**
    * Adds exception with full trace to the log
    */
   void logException(String message, Exception ex) throws IOException;
}
