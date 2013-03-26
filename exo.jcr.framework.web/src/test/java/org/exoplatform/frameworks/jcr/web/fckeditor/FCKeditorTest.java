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
