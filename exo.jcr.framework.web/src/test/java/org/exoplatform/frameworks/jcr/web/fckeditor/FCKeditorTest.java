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

package org.exoplatform.frameworks.jcr.web.fckeditor;

import junit.framework.TestCase;


/**
 * Created by The eXo Platform SAS .
 *
 * @author Aymen Boughzela
 * @version $Id: $
 */

public class FCKeditorTest extends TestCase {

    String[] userAgentString={"mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/ safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/0.2.149.27 safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/1.0.154.39 safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/3.0.194.0 safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/6.0 safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/10.0.601.0 safari/537.11",
            "mozilla/5.0 (windows nt 6.1; wow64) applewebkit/537.11 (khtml, like gecko) chrome/23.0.1271.97 safari/537.11"};
    String[] version={"0.0","0.2","1.0","3.0","6.0","10.0","23.0"} ;


    public void testChromeRetrieveBrowserVersion(){
        FCKeditor fck =new FCKeditor() ;
        for (int i=0 ;i<userAgentString.length ; i++) {
            assertEquals(Double.parseDouble(version[i]),fck.retrieveBrowserVersion(userAgentString[i]));
        }

    }

    public void testChromeIsCompatible(){
        FCKeditor fck =new FCKeditor() ;
        for (int i=0 ;i<userAgentString.length ; i++) {
            fck.setUserAgent(userAgentString[i]);
            if(i<4)
                assertFalse(fck.isCompatible());
            else
                assertTrue(fck.isCompatible());
        }

    }
}
