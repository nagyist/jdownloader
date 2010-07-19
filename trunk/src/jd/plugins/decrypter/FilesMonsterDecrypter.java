//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "http://[\\w\\.\\d]*?filesmonster\\.com/download.php\\?id=.+" }, flags = { 0 })
public class FilesMonsterDecrypter extends PluginForDecrypt {

    public FilesMonsterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            DownloadLink finalOne = createDownloadlink(parameter.replace("filesmonster.com", "decryptedfilesmonster.com"));
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        String postThat = br.getRegex("\"(http://filesmonster\\.com/dl/.*?/free/.*?)\"").getMatch(0);
        if (postThat == null) return null;
        br.postPage(postThat, "");
        String findOtherLinks = br.getRegex("reserve_ticket\\('(/dl/rft/.*?)'\\)").getMatch(0);
        if (findOtherLinks != null) {
            br.getPage("http://filesmonster.com" + findOtherLinks);
            String[] decryptedStuff = br.getRegex("(\\{\"dlcode\":\".*?\",\"name\":\".*?\",\"size\":\\d+,\"cutted_name\":\".*?\"\\})").getColumn(0);
            if (decryptedStuff == null || decryptedStuff.length == 0) return null;
            String theImportantPartOfTheMainLink = new Regex(parameter, "filesmonster\\.com/download\\.php\\?id=(.+)").getMatch(0);
            for (String fileInfo : decryptedStuff) {
                String filename = new Regex(fileInfo, "\"name\":\"(.*?)\"").getMatch(0);
                String filesize = new Regex(fileInfo, "\"size\":(\\d+)").getMatch(0);
                String filelinkPart = new Regex(fileInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
                if (filename.isEmpty() || filesize.isEmpty() || filelinkPart.isEmpty()) {
                    logger.warning("Filesmonsterdecrypter failed while decrypting link:" + parameter);
                    return null;
                }
                String dllink = "http://decryptedfilesmonster.com/dl/" + theImportantPartOfTheMainLink + "/free/2/" + filelinkPart;
                DownloadLink finalOne = createDownloadlink(dllink);
                finalOne.setName(filename.replace("amp;", ""));
                finalOne.setDownloadSize(Integer.parseInt(filesize));
                finalOne.setAvailable(true);
                finalOne.setProperty("origfilename", filename);
                finalOne.setProperty("origsize", filesize);
                decryptedLinks.add(finalOne);
            }
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("filesmonster.com", "decryptedfilesmonster.com")));

        return decryptedLinks;
    }

}