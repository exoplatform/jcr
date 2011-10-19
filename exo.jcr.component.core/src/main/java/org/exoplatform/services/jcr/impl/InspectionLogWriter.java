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
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Text-based inspection log implementation. It uses any compatible Writer instance for output.
 * 
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 6.10.2011 skarpenko $
 *
 */
public class InspectionLogWriter implements InspectionLog
{
   private static final String LINE_COMMENT = "//";

   private static final String LINE_DELIMITER = "\n";

   private static final String WSP = " ";

   private final Writer out;

   private final Set<InspectionStatus> statusSet = new HashSet<InspectionStatus>();

   public InspectionLogWriter(Writer out)
   {
      this.out = out;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasInconsistency()
   {
      return statusSet.contains(InspectionStatus.ERR) || statusSet.contains(InspectionStatus.REINDEX);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasWarnings()
   {
      return statusSet.contains(InspectionStatus.WARN);
   }

   /**
    * {@inheritDoc}
    */
   public void logComment(String message) throws IOException
   {
      writeLine(message);
      out.flush();
   }

   /**
    * {@inheritDoc}
    */
   public void logInspectionDescription(String description) throws IOException
   {
      writeLine(description);
      out.flush();
   }

   /**
    * {@inheritDoc}
    */
   public void logBrokenObjectInfo(String brokenObjectDesc, String comment, InspectionStatus status) throws IOException
   {
      statusSet.add(status);

      out.write(status.toString());
      out.write(WSP);
      out.write(brokenObjectDesc);
      out.write(WSP);
      out.write(comment);
      out.write(LINE_DELIMITER);
      out.flush();
   }

   public void logException(String message, Exception ex) throws IOException
   {
      statusSet.add(InspectionStatus.ERR);

      writeLine(message);
      writeStackTrace(ex);
      out.flush();
   }

   private void writeLine(String message) throws IOException
   {
      out.write(LINE_COMMENT);
      out.write(message);
      out.write(LINE_DELIMITER);
   }

   private void writeStackTrace(Throwable e) throws IOException
   {
      writeLine(e.getMessage());
      writeLine(e.toString());
      StackTraceElement[] trace = e.getStackTrace();
      for (int i = 0; i < trace.length; i++)
      {
         writeLine("\tat " + trace[i]);
      }

      Throwable ourCause = e.getCause();
      if (ourCause != null)
      {
         writeLine("Cause:");
         writeStackTrace(ourCause);
      }
   }
}
