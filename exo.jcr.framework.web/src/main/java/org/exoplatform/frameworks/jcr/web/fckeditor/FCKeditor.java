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
package org.exoplatform.frameworks.jcr.web.fckeditor;

import javax.servlet.http.HttpServletRequest;

/**
 * The main class of the class lib.<br>
 * It's the container for all properties and the class that generate the output based on browser
 * capabilities and configurations passed by the developer.
 * 
 * @author Simone Chiaretta (simo@users.sourceforge.net)
 */
public class FCKeditor
{

   private FCKeditorConfigurations oConfig;

   private String instanceName;

   private String userAgent;

   private String value = "";

   private String basePath;

   private String toolbarSet = "Default";

   private String width = "100%";

   private String height = "200";

   /**
    * Get the unique name of the editor
    * 
    * @return name
    */
   public String getInstanceName()
   {
      return instanceName;
   }

   /**
    * Set the unique name of the editor
    * 
    * @param value
    *          name
    */
   public void setInstanceName(String value)
   {
      instanceName = value;
   }

   /**
    * Get the initial value to be edited.<br>
    * In HTML code
    * 
    * @return value
    */
   public String getValue()
   {
      return value;
   }

   /**
    * Set the initial value to be edited.<br>
    * In HTML code
    * 
    * @param value
    *          value
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * Get the dir where the FCKeditor files reside on the server
    * 
    * @return path
    */
   public String getBasePath()
   {
      return basePath;
   }

   /**
    * Set the dir where the FCKeditor files reside on the server.<br>
    *<b>Remarks</b>:<br>
    *Avoid using relative paths. It is preferable to set the base path starting from the root (/).<br>
    *Always finish the path with a slash (/).
    * 
    * @param value
    *          path
    */
   public void setBasePath(String value)
   {
      basePath = value;
   }

   /**
    * Get the name of the toolbar to display
    * 
    * @return toolbar name
    */
   public String getToolbarSet()
   {
      return toolbarSet;
   }

   /**
    * Set the name of the toolbar to display
    * 
    * @param value
    *          toolbar name
    */
   public void setToolbarSet(String value)
   {
      toolbarSet = value;
   }

   /**
    * Get the width of the textarea
    * 
    * @return width
    */
   public String getWidth()
   {
      return width;
   }

   /**
    * Set the width of the textarea
    * 
    * @param value
    *          width
    */
   public void setWidth(String value)
   {
      width = value;
   }

   /**
    * Get the height of the textarea
    * 
    * @return height
    */
   public String getHeight()
   {
      return height;
   }

   /**
    * Set the height of the textarea
    * 
    * @param value
    *          height
    */
   public void setHeight(String value)
   {
      height = value;
   }

   /**
    * Get the advanced configuation set.<br>
    * Adding element to this collection you can override the settings specified in the config.js
    * file.
    * 
    * @return configuration collection
    */
   public FCKeditorConfigurations getConfig()
   {
      return oConfig;
   }

   /**
    * Set the advanced configuation set.
    * 
    * @param value
    *          configuration collection
    */
   public void setConfig(FCKeditorConfigurations value)
   {
      oConfig = value;
   }

   /**
    * Initialize the object setting all value to the default ones.
    * <p>
    * <ul>
    * <li>width: 100%</li>
    * <li>height: 200</li>
    * <li>toolbar name: Default</li>
    * <li>basePath: context root + "/FCKeditor/"</li>
    * </ul>
    * </p>
    * 
    * @param req
    *          request object
    */
   public FCKeditor(HttpServletRequest req)
   {
      // request=req;
      userAgent = req.getHeader("user-agent");
      basePath = req.getContextPath() + "/FCKeditor/";
      oConfig = new FCKeditorConfigurations();
   }

   /**
    * Initialize the object setting the unique name and then all value to the default ones.
    * <p>
    * <ul>
    * <li>width: 100%</li>
    * <li>height: 200</li>
    * <li>toolbar name: Default</li>
    * <li>basePath: context root + "/FCKeditor/"</li>
    * </ul>
    * </p>
    * 
    * @param req
    *          request object
    * @param parInstanceName
    *          unique name
    */
   public FCKeditor(HttpServletRequest req, String parInstanceName)
   {
      // request=req;
      userAgent = req.getHeader("user-agent");
      basePath = req.getContextPath() + "/FCKeditor/";
      instanceName = parInstanceName;
      oConfig = new FCKeditorConfigurations();
   }

   /**
    * Initialize the object setting all basic configurations.<br>
    * The basePath is context root + "/FCKeditor/"
    * 
    * @param req
    *          request object
    * @param parInstanceName
    *          unique name
    * @param parWidth
    *          width
    * @param parHeight
    *          height
    * @param parToolbarSet
    *          toolbarSet name
    * @param parValue
    *          initial value
    */
   public FCKeditor(HttpServletRequest req, String parInstanceName, String parWidth, String parHeight,
      String parToolbarSet, String parValue)
   {
      // request=req;
      userAgent = req.getHeader("user-agent");
      basePath = req.getContextPath() + "/FCKeditor/";
      instanceName = parInstanceName;
      width = parWidth;
      height = parHeight;
      toolbarSet = parToolbarSet;
      value = parValue;
      oConfig = new FCKeditorConfigurations();
   }

    /**
     * Initialize the object without param
     *
     */
    FCKeditor()
    {
    }

   boolean isCompatible()
   {
      // [PN] 11.07.06 userAgent as global var, no request stored in editor
      // String userAgent=request.getHeader("user-agent");
      if (userAgent == null)
         return false;
      String userAgentString = userAgent.toLowerCase();
      if ((userAgentString.indexOf("msie") != -1) && (userAgentString.indexOf("mac") == -1)
         && (userAgentString.indexOf("opera") == -1))
      {
         if (retrieveBrowserVersion(userAgentString) >= 5.5)
            return true;
      }
      else if (userAgentString.indexOf("chrome") != -1)
      {
          if (retrieveBrowserVersion(userAgentString) >= 5)
              return true;
      }
      else if (userAgentString.indexOf("gecko") != -1)
      {
         if (retrieveBrowserVersion(userAgentString) >= 20030210)
            return true;
      }
      return false;
   }

    double retrieveBrowserVersion(String userAgentString)
   {
      try{
      if (userAgentString.indexOf("msie") > -1)
      {
         String str = userAgentString.substring(userAgentString.indexOf("msie") + 5);
         return Double.parseDouble(str.substring(0, str.indexOf(";")));
      }
      else if (userAgentString.indexOf("chrome")>-1)
      {
         String str = userAgentString.substring(userAgentString.indexOf("chrome") + 7,userAgentString.indexOf(" safari"));
         if(str.length()==0)
              return 0;
         else
              return  Double.parseDouble(str.substring(0,str.indexOf(".")+2));
      }
      else
      {
         String str = userAgentString.substring(userAgentString.indexOf("gecko") + 6);
         return Double.parseDouble(str.substring(0, 8));
      }
      }
      catch (NumberFormatException e)
      {
          return -1;
      }
   }

   private String HTMLEncode(String txt)
   {
      txt = txt.replaceAll("&", "&amp;");
      txt = txt.replaceAll("<", "&lt;");
      txt = txt.replaceAll(">", "&gt;");
      txt = txt.replaceAll("\"", "&quot;");
      txt = txt.replaceAll("'", "&#146;");
      return txt;
   }

   /**
    * Generate the HTML Code for the editor. <br>
    * Evalute the browser capabilities and generate the editor if IE 5.5 or Gecko 20030210 or
    * greater, or a simple textarea otherwise.
    * 
    * @return html code
    */
   public String create()
   {
      StringBuffer strEditor = new StringBuffer();

      strEditor.append("<div>");
      String encodedValue = HTMLEncode(value);

      if (isCompatible())
      {

         strEditor.append("<input type=\"hidden\" id=\"" + instanceName + "\" name=\"" + instanceName + "\" value=\""
            + encodedValue + "\">");

         strEditor.append(createConfigHTML());
         strEditor.append(createIFrameHTML());

      }
      else
      {
         strEditor.append("<TEXTAREA name=\"" + instanceName + "\" rows=\"4\" cols=\"40\" style=\"WIDTH: " + width
            + "; HEIGHT: " + height + "\" wrap=\"virtual\">" + encodedValue + "</TEXTAREA>");
      }
      strEditor.append("</div>");
      return strEditor.toString();
   }

   private String createConfigHTML()
   {
      String configStr = oConfig.getUrlParams();

      if (!configStr.equals(""))
         configStr = configStr.substring(1);

      return "<input type=\"hidden\" id=\"" + instanceName + "___Config\" value=\"" + configStr + "\">";
   }

   private String createIFrameHTML()
   {

      StringBuilder sLink =
         new StringBuilder(basePath).append("editor/fckeditor.html?InstanceName=").append(instanceName);

      if (!toolbarSet.equals(""))
      {
         sLink.append("&Toolbar=").append(toolbarSet);
      }

      return "<iframe id=\"" + instanceName + "___Frame\" src=\"" + sLink.toString() + "\" width=\"" + width
         + "\" height=\"" + height + "\" frameborder=\"no\" scrolling=\"no\"></iframe>";
   }

   /**
    * @return the userAgent
    */
   public String getUserAgent()
   {
      return userAgent;
   }

   /**
    * @param userAgent
    *          the userAgent to set
    */
   public void setUserAgent(String userAgent)
   {
      this.userAgent = userAgent;
   }

}
