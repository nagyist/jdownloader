//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lolcrypt.org" }, urls = { "http://(www\\.)?lolcrypt\\.org/folder\\?fid=[a-z0-9]+" }, flags = { 0 })
public class LolCryptOrg extends PluginForDecrypt {

    public LolCryptOrg(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Only works for megaupload links at the moment */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String[] links = br.getRegex("(/decrypt\\?fid=[a-z0-9]+\\&lid=\\d+\\&key=[a-z0-9]+)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            br.getPage("http://lolcrypt.org" + singleLink);
            /**
             * Schauen, ob es ein megaupload Link ist
             */
            String finallink = br.getRegex("href=\"javascript:clipboardcopy\\(\\'(http://(www\\.)?megaupload\\.com/\\?d=[A-Z0-9]+)\\'\\)").getMatch(0);
            final Boolean mu = finallink != null ? true : false;
            /**
             * Megaupload Direktlink holen
             */
            finallink = br.getRegex("id=\"dlbuttondisabled\"></a> [\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            if (mu) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(finallink);
                    finallink = br.getRedirectLocation();
                    finallink = finallink == null ? br.getRequest().getHttpConnection().getHeaderField("etag").replace("\"", "") : finallink;
                    if (finallink == null) {
                        continue;
                    }
                    if (!finallink.startsWith("http")) {
                        finallink = "http://www.megaupload.com/?d=" + finallink;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

}
