/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.jcr.backupconsole;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: FormAuthntication.java 111 2010-11-11 11:11:11Z rainf0x $
 */
public class FormAuthentication
{
   private String method;

   private String formPath;

   private HashMap<String, String> formParams;

   public FormAuthentication(String method, String formPath, HashMap<String, String> formParams)
   {
      this.method = method;
      this.formPath = formPath;
      this.formParams = formParams;
   }

   public String getMethod()
   {
      return method;
   }

   public String getFormPath()
   {
      return formPath;
   }

   public HashMap<String, String> getFormParams()
   {
      return formParams;
   }

}
