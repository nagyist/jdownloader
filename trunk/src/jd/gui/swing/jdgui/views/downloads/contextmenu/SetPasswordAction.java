package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class SetPasswordAction extends ContextMenuAction {

    private static final long serialVersionUID = -6673856992749946616L;

    private final ArrayList<DownloadLink> links;

    public SetPasswordAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.password";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_setdlpw() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        String pw = UserIO.getInstance().requestInputDialog(0, T._.gui_linklist_setpw_message(), null);
        for (DownloadLink link : links) {
            link.setProperty("pass", pw);
        }
    }

}