/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav;

import junit.framework.TestCase;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.jcr.webdav.util.InitParamsDefaults;
import org.exoplatform.services.jcr.webdav.util.InitParamsNames;
import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;

import javax.ws.rs.ext.RuntimeDelegate;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestWebDavServiceInitParams extends TestCase
{
   public void testInitParams()
   {
      WebDavServiceInitParams params = new WebDavServiceInitParams();
      assertEquals(InitParamsDefaults.FOLDER_NODE_TYPE, params.getDefaultFolderNodeType());
      assertEquals(InitParamsDefaults.FILE_NODE_TYPE, params.getDefaultFileNodeType());
      assertEquals(InitParamsDefaults.FILE_MIME_TYPE, params.getDefaultFileMimeType());
      assertEquals(InitParamsDefaults.UPDATE_POLICY, params.getDefaultUpdatePolicyType());
      assertEquals(InitParamsDefaults.AUTO_VERSION, params.getDefaultAutoVersionType());
      assertTrue(params.getXsltParams().isEmpty());
      assertTrue(params.getUntrustedUserAgents().isEmpty());
      assertTrue(params.getUntrustedUserAgentsPattern().isEmpty());
      assertTrue(params.getCacheControlMap().isEmpty());
      assertEquals(1, params.getAllowedFolderNodeTypes().size());
      assertTrue(params.getAllowedFolderNodeTypes().contains(InitParamsDefaults.FOLDER_NODE_TYPE));
      assertEquals(1, params.getAllowedFileNodeTypes().size());
      assertTrue(params.getAllowedFileNodeTypes().contains(InitParamsDefaults.FILE_NODE_TYPE));
      
      InitParams ip = new InitParams();
      ValueParam vp = new ValueParam();
      vp.setName(InitParamsNames.DEF_FOLDER_NODE_TYPE);
      vp.setValue(InitParamsNames.DEF_FOLDER_NODE_TYPE);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.DEF_FILE_NODE_TYPE);
      vp.setValue(InitParamsNames.DEF_FILE_NODE_TYPE);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.DEF_FILE_MIME_TYPE);
      vp.setValue(InitParamsNames.DEF_FILE_MIME_TYPE);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.UPDATE_POLICY);
      vp.setValue(InitParamsNames.UPDATE_POLICY);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.AUTO_VERSION);
      vp.setValue(InitParamsNames.AUTO_VERSION);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.FILE_ICON_PATH);
      vp.setValue(InitParamsNames.FILE_ICON_PATH);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.FOLDER_ICON_PATH);
      vp.setValue(InitParamsNames.FOLDER_ICON_PATH);
      ip.addParameter(vp);
      vp = new ValueParam();
      vp.setName(InitParamsNames.CACHE_CONTROL);
      vp.setValue("text/xml,text/html:max-age=1800;text/*:max-age=777;image/png,image/jpg:max-age=3600;*/*:no-cache;image/*:max-age=555");
      ip.addParameter(vp);
      ValuesParam vsp = new ValuesParam();
      vsp.setName(InitParamsNames.UNTRUSTED_USER_AGENTS);
      vsp.getValues().add(InitParamsNames.UNTRUSTED_USER_AGENTS);
      vsp.getValues().add(InitParamsNames.UNTRUSTED_USER_AGENTS + "2");
      vsp.getValues().add("^(Microsoft Office Excel 2013).*(Windows NT 6.1)$");
      ip.addParameter(vsp);
      vsp = new ValuesParam();
      vsp.setName(InitParamsNames.ALLOWED_FOLDER_NODE_TYPES);
      vsp.getValues().add(InitParamsNames.ALLOWED_FOLDER_NODE_TYPES);
      ip.addParameter(vsp);
      vsp = new ValuesParam();
      vsp.setName(InitParamsNames.ALLOWED_FILE_NODE_TYPES);
      vsp.getValues().add(InitParamsNames.ALLOWED_FILE_NODE_TYPES);
      ip.addParameter(vsp);
      
      assertEquals(11, ip.size());
      
      // This is required to be able to parse the MimeType
      RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
      
      params = new WebDavServiceInitParams(ip);
      
      assertEquals(InitParamsNames.DEF_FOLDER_NODE_TYPE, params.getDefaultFolderNodeType());
      assertEquals(InitParamsNames.DEF_FILE_NODE_TYPE, params.getDefaultFileNodeType());
      assertEquals(InitParamsNames.DEF_FILE_MIME_TYPE, params.getDefaultFileMimeType());
      assertEquals(InitParamsNames.UPDATE_POLICY, params.getDefaultUpdatePolicyType());
      assertEquals(InitParamsNames.AUTO_VERSION, params.getDefaultAutoVersionType());
      assertEquals(2, params.getXsltParams().size());
      assertEquals(InitParamsNames.FILE_ICON_PATH, params.getXsltParams().get(InitParamsNames.FILE_ICON_PATH));
      assertEquals(InitParamsNames.FOLDER_ICON_PATH, params.getXsltParams().get(InitParamsNames.FOLDER_ICON_PATH));      
      assertEquals(3, params.getUntrustedUserAgents().size());
      assertEquals(3, params.getUntrustedUserAgentsPattern().size());
      assertTrue(params.getUntrustedUserAgents().contains(InitParamsNames.UNTRUSTED_USER_AGENTS));
      assertTrue(params.isUntrustedUserAgent(InitParamsNames.UNTRUSTED_USER_AGENTS));
      assertTrue(params.getUntrustedUserAgents().contains(InitParamsNames.UNTRUSTED_USER_AGENTS + "2"));
      assertTrue(params.isUntrustedUserAgent(InitParamsNames.UNTRUSTED_USER_AGENTS + "2"));
      assertTrue(params.isUntrustedUserAgent("Microsoft Office Excel 2013 (15.0.4701) Windows NT 6.1"));
      assertTrue(params.isUntrustedUserAgent("Microsoft Office Excel 2013 (15.0.4631) Windows NT 6.1"));
      assertFalse(params.isUntrustedUserAgent("Microsoft Office Excel 2013"));
      assertFalse(params.isUntrustedUserAgent("Microsoft Office PowerPoint 2013"));
      assertEquals(7, params.getCacheControlMap().size());
      assertEquals(1, params.getAllowedFolderNodeTypes().size());
      assertTrue(params.getAllowedFolderNodeTypes().contains(InitParamsNames.ALLOWED_FOLDER_NODE_TYPES));
      assertEquals(1, params.getAllowedFileNodeTypes().size());
      assertTrue(params.getAllowedFileNodeTypes().contains(InitParamsNames.ALLOWED_FILE_NODE_TYPES));

   }
}
