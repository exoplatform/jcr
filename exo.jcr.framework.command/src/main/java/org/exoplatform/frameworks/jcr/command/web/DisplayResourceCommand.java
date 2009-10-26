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
package org.exoplatform.frameworks.jcr.command.web;

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.frameworks.jcr.command.DefaultKeys;
import org.exoplatform.frameworks.jcr.command.JCRCommandHelper;

import java.io.InputStream;
import java.io.PrintWriter;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by The eXo Platform SAS .<br/> the command to output nt:resource to Servlet Response gets
 * DefaultKeys.PATH attrribute from the Context and acts as follow: - if there is nt:resource Node
 * on DefaultKeys.PATH displays it - otherwise recursively tries to get nt:resource from incoming
 * node's primary items and display it - throws PathNotFoundException if no such a node found
 * WARNING: this mechanizm is not suitable for big files streaming as uses byte arry buffer for data
 * transfer!
 * 
 * @author Gennady Azarenkov
 * @version $Id: DisplayResourceCommand.java 13861 2007-03-28 11:31:16Z vetal_ok $
 */

public class DisplayResourceCommand implements Command
{

   public static String DEFAULT_MAPPING = "/jcr";

   public static String DEFAULT_ENCODING = "UTF-8";

   private String pathKey = DefaultKeys.PATH;

   /*
    * (non-Javadoc)
    * @see org.apache.commons.chain.Command#execute(org.apache.commons.chain.Context)
    */
   public boolean execute(Context context) throws Exception
   {
      GenericWebAppContext webCtx = (GenericWebAppContext)context;
      HttpServletResponse response = webCtx.getResponse();
      HttpServletRequest request = webCtx.getRequest();

      // standalone request?
      String servletPath = request.getPathInfo();
      boolean doClose = true;
      // or included?
      if (servletPath == null)
      {
         servletPath = (String)request.getAttribute("javax.servlet.include.path_info");
         if (servletPath != null)
            doClose = false;
      }

      Node file = (Node)webCtx.getSession().getItem((String)context.get(pathKey));
      file.refresh(false);

      Node content = null;

      try
      {
         content = JCRCommandHelper.getNtResourceRecursively(file);
      }
      catch (ItemNotFoundException e)
      {
         // Patch for ver 1.0 back compatibility
         // as exo:image was not primary item
         if (file.isNodeType("exo:article"))
         {
            try
            {
               content = file.getNode("exo:image");
            }
            catch (PathNotFoundException e1)
            {
               throw e; // new ItemNotFoundException("No nt:resource node found at
               // "+file.getPath()+" nor primary items of nt:resource type
               // ");
            }
         }
         else
         {
            throw e; // new ItemNotFoundException("No nt:resource node found at
            // "+file.getPath()+" nor primary items of nt:resource type
            // ");
         }

      }

      // if(file.isNodeType("nt:file")) {
      // content = file.getNode("jcr:content");
      // } else if(file.isNodeType("exo:article")) {
      // content = file.getNode("exo:image");
      // } else
      // throw new Exception("Invalid node type, expected nt:file or exo:article,
      // have "+file.getPrimaryNodeType().getName()+" at "+file.getPath());

      Property data;
      try
      {
         data = content.getProperty("jcr:data");
      }
      catch (PathNotFoundException e)
      {
         throw new PathNotFoundException("No jcr:data node found at " + content.getPath());
      }

      String mime = content.getProperty("jcr:mimeType").getString();
      String encoding =
         content.hasProperty("jcr:encoding") ? content.getProperty("jcr:encoding").getString() : DEFAULT_ENCODING;

      MimeTypeResolver resolver = new MimeTypeResolver();
      String fileName = file.getName();
      String fileExt = "";
      if (fileName.lastIndexOf(".") > -1)
      {
         fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);
         fileName = fileName.substring(0, fileName.lastIndexOf("."));
      }
      String mimeExt = resolver.getExtension(mime);
      if (fileExt == null || fileExt.length() == 0)
      {
         fileExt = mimeExt;
      }
      response.setContentType(mime + "; charset=" + encoding);
      String parameter = (String)context.get("cache-control-max-age");
      String cacheControl = parameter == null ? "" : "public, max-age=" + parameter;
      response.setHeader("Cache-Control: ", cacheControl);
      response.setHeader("Pragma: ", ""); // leave blank to avoid IE errors
      response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "." + fileExt + "\"");

      if (mime.startsWith("text"))
      {
         PrintWriter out = response.getWriter();
         out.write(data.getString());
         out.flush();
         if (doClose)
            out.close();
      }
      else
      {
         InputStream is = data.getStream();
         byte[] buf = new byte[is.available()];
         is.read(buf);
         ServletOutputStream os = response.getOutputStream();
         os.write(buf);
         os.flush();
         if (doClose)
            os.close();
      }

      return true;
   }

   /**
    * @return path Key
    */
   public String getPathKey()
   {
      return pathKey;
   }

}
