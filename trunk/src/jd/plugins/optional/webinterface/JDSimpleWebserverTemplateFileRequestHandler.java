package jd.plugins.optional.webinterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JProgressBar;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.plugins.optional.webinterface.template.Template;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private Logger logger = JDUtilities.getLogger();
    private JDSimpleWebserverResponseCreator response;
    private JDController controller = JDUtilities.getController();
    private DecimalFormat f = new DecimalFormat("#0.00");

    private Vector v_info = new Vector();

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {

        this.response = response;
    }
    
    private void add_all_info(Template t, HashMap<String, String> requestParameter){
        FilePackage fp;
        String[] ids;
        Integer package_id = 0;
        if (requestParameter.containsKey("all_info")) {
            ids = requestParameter.get("all_info").toString().split("[+]", 2);
            package_id = JDUtilities.filterInt(ids[0].toString());
            fp = JDUtilities.getController().getPackages().get(package_id);
            
            addEntry("name",fp.getName());            
            addEntry("comment",fp.getComment());
            addEntry("dldirectory",fp.getDownloadDirectory());
            addEntry("packagesize",JDUtilities.formatKbReadable(fp.getTotalEstimatedPackageSize())+" "+fp.getTotalEstimatedPackageSize()+" KB");
            addEntry("loaded",JDUtilities.formatKbReadable(fp.getTotalKBLoaded())+" "+fp.getTotalKBLoaded()+" KB");
            addEntry("links","");
            
            DownloadLink next=null;
            int i=1;
            for(Iterator<DownloadLink> it = fp.getDownloadLinks().iterator();it.hasNext();i++){
                next=it.next();         
                double percent=next.getDownloadCurrent() * 100.0 / Math.max(1,next.getDownloadMax());                
                addEntryandPercent(i+". "+next.getName(), JDUtilities.formatKbReadable((int)next.getDownloadSpeed()/1024)+"/s "+ JDUtilities.getPercent((int)next.getDownloadCurrent(), (int)next.getDownloadMax())+" | "+next.getDownloadCurrent()+"/"+next.getDownloadMax()+" bytes",percent);                 
            }
            t.setParam("all_infos", v_info);
        }
    }

    private void add_single_info(Template t, HashMap<String, String> requestParameter) {        

        /* überprüft ob single_info vorhanden und füllt ggf. dieses template */
        DownloadLink downloadLink;
        Integer download_id = 0;
        Integer package_id = 0;
        String[] ids;
        if (requestParameter.containsKey("single_info")) {

            ids = requestParameter.get("single_info").toString().split("[+]", 2);
            package_id = JDUtilities.filterInt(ids[0].toString());
            download_id = JDUtilities.filterInt(ids[1].toString());
            downloadLink = JDUtilities.getController().getPackages().get(package_id).getDownloadLinks().get(download_id);

            addEntry("file", new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getPassword() != null) {
                addEntry(JDLocale.L("linkinformation.password.name", "Passwort"), downloadLink.getFilePackage().getPassword());
            }
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
                addEntry(JDLocale.L("linkinformation.comment.name", "Kommentar"), downloadLink.getFilePackage().getComment());
            }
            if (downloadLink.getFilePackage() != null) {
                addEntry(JDLocale.L("linkinformation.package.name", "Packet"), downloadLink.getFilePackage().getName());
            }
            if (downloadLink.getDownloadMax() > 0) {
                addEntry(JDLocale.L("linkinformation.filesize.name", "Dateigröße"), JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()));
            }
            if (downloadLink.isAborted()) {
                addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.aborted", "Abgebrochen"));
            }
            if (downloadLink.isAvailabilityChecked()) {
                addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), downloadLink.isAvailable() ? JDLocale.L("linkinformation.available.ok", "Datei OK") : JDLocale.L("linkinformation.available.error", "Fehler!"));
            } else {
                addEntry(JDLocale.L("linkinformation.available.name", "Verfügbar"), JDLocale.L("linkinformation.available.notchecked", "noch nicht überprüft"));
            }
            if (downloadLink.getDownloadSpeed() > 0) {
                addEntry(JDLocale.L("linkinformation.speed.name", "Geschwindigkeit"), downloadLink.getDownloadSpeed() / 1024 + " kb/s");
            }
            if (downloadLink.getFileOutput() != null) {
                addEntry(JDLocale.L("linkinformation.saveto.name", "Speichern in"), downloadLink.getFileOutput());
            }
            if (downloadLink.getRemainingWaittime() > 0) {
                addEntry(JDLocale.L("linkinformation.waittime.name", "Wartezeit"), downloadLink.getRemainingWaittime() + " sek");
            }
            if (downloadLink.isInProgress()) {
                addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.underway", " ist in Bearbeitung"));
            } else {
                addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.notunderway", " ist nicht in Bearbeitung"));
            }
            if (!downloadLink.isEnabled()) {
                addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.deactivated", " ist deaktiviert"));
            } else {
                addEntry(JDLocale.L("linkinformation.download.name", "Download"), JDLocale.L("linkinformation.download.activated", " ist aktiviert"));
            }
            addEntry(JDLocale.L("linkinformation.download.status","Status"), downloadLink.getStatusText());
            
            DownloadInterface dl;
            if (downloadLink.isInProgress() && (dl = downloadLink.getDownloadInstance()) != null) {
                addEntry(JDLocale.L("linkinformation.download.chunks.label","Chunks"), "");
                int i = 1;
                for (Iterator<Chunk> it = dl.getChunks().iterator(); it.hasNext(); i++) {                    
                    Chunk next = it.next();
                    double percent=next.getBytesLoaded() * 100.0 / Math.max(1,next.getChunkSize());
                    addEntryandPercent(JDLocale.L("download.chunks.connection", "Verbindung")+ " " + i,JDUtilities.formatKbReadable((int)next.getBytesPerSecond()/1024)+"/s "+ JDUtilities.getPercent(next.getBytesLoaded(), next.getChunkSize()),percent );
                }

            }
            t.setParam("single_infos", v_info);
        }
        ;
    }
    
    private void addEntry(String var, String value) {
        Hashtable h_info = new Hashtable();
        h_info.put("info_var", var);
        h_info.put("info_value", value);
        v_info.addElement(h_info);
    }
    
    private void addEntryandPercent(String var, String value,double percent){
        Hashtable h_info = new Hashtable();
        h_info.put("info_var", var);
        h_info.put("info_value", value);
        h_info.put("info_percent", f.format(percent));
        v_info.addElement(h_info);
    }


    private void add_status_page(Template t,HashMap<String, String> requestParameter){
        Vector v, v2 = new Vector();
        Hashtable h, h2 = new Hashtable();
        v = new Vector();
        String value;

        FilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
        Double percent = 0.0;
        for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
            filePackage = JDUtilities.getController().getPackages().get(Package_ID);

            h = new Hashtable();
            /* Paket Infos */
            h.put("download_name", filePackage.getName());

            value = "";
            percent = filePackage.getPercent();
            h.put("download_status_percent", f.format(percent));

            if (filePackage.getLinksInProgress() > 0) {
                value = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDLocale.L("gui.treetable.packagestatus.links_active", "aktiv");
            }
            if (filePackage.getTotalDownloadSpeed() > 0) value = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + JDUtilities.formatSeconds(filePackage.getETA()) + " @ " + JDUtilities.formatKbReadable(filePackage.getTotalDownloadSpeed() / 1024) + "/s";

            h.put("package_id", Package_ID.toString());
            h.put("download_hoster", value);
            h.put("download_status_color", "#8bffa1");
            h.put("download_status", f.format(percent) + " % (" + JDUtilities.formatKbReadable(filePackage.getTotalKBLoaded()) + " / " + JDUtilities.formatKbReadable(filePackage.getTotalEstimatedPackageSize()) + ")");

            v2 = new Vector();

            for (Download_ID = 0; Download_ID < filePackage.getDownloadLinks().size(); Download_ID++) {
                dLink = filePackage.getDownloadLinks().get(Download_ID);

                /* Download Infos */

                percent = (double) (dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadMax()));

                h2 = new Hashtable();
                h2.put("download_status_percent", f.format(percent));
                h2.put("package_id", Package_ID.toString());
                h2.put("download_id", Download_ID.toString());
                h2.put("download_name", dLink.getName());

                h2.put("download_hoster", dLink.getHost());

                if (dLink.isEnabled()) {

                    switch (dLink.getStatus()) {
                    case DownloadLink.STATUS_DONE:
                        h2.put("download_status_color", "#8bffa1");
                        break;

                    case DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS:
                        h2.put("download_status_color", "#1189ee");
                        break;

                    default:
                        h2.put("download_status_color", "#92afc6");
                    }
                    ;
                } else {
                    h2.put("download_status_color", "#c70000");
                }

                h2.put("download_status", f.format(percent) + "% " + dLink.getStatusText());
                v2.addElement(h2);

            }
            h.put("downloads", v2);
            v.addElement(h);
        }
        t.setParam("config_current_speed", JDUtilities.getController().getSpeedMeter() / 1024);

        t.setParam("config_max_downloads", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 0));
        t.setParam("config_max_speed", JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));

        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false) == true) {
            t.setParam("config_autoreconnect", "");
        } else {
            t.setParam("config_autoreconnect", "checked");
        }
        ;

        if (JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            t.setParam("config_startstopbutton", "Stop Downloads");
        } else {
            t.setParam("config_startstopbutton", "Start Downloads");
        }
        ;

        t.setParam("pakete", v);        
    }

    private void add_linkadder_page(Template t,HashMap<String, String> requestParameter){
        Vector v, v2 = new Vector();
        Hashtable h, h2 = new Hashtable();
        v = new Vector();
        

        FilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
     
        for (Package_ID = 0; Package_ID < JDWebinterface.Link_Adder_Packages.size(); Package_ID++) {
            filePackage = JDWebinterface.Link_Adder_Packages.get(Package_ID);

            h = new Hashtable();
            /* Paket Infos */
            h.put("download_name", filePackage.getName());

            
            h.put("package_id", Package_ID.toString());
            
            
            v2 = new Vector();

            for (Download_ID = 0; Download_ID < filePackage.getDownloadLinks().size(); Download_ID++) {
                dLink = filePackage.getDownloadLinks().get(Download_ID);

                /* Download Infos */                

                h2 = new Hashtable();
                
                h2.put("package_id", Package_ID.toString());
                h2.put("download_id", Download_ID.toString());
                h2.put("download_name", dLink.getName());

                h2.put("download_hoster", dLink.getHost());
                
                v2.addElement(h2);

            }
            h.put("downloads", v2);
            v.addElement(h);
        }
        

        t.setParam("pakete", v);        
    }
    
    private void add_password_list(Template t, HashMap<String, String> requestParameter){
        JUnrar unrar = new JUnrar(false);
        String[] pws = unrar.returnPasswords();
        String pwlist="";
        for (int i = 0; i < pws.length; i++) {
            pwlist=pwlist+System.getProperty("line.separator")+pws[i];            
        }
        t.setParam("password_list", pwlist);
    }
    @SuppressWarnings("unchecked")
    public void handleRequest(String url, HashMap<String, String> requestParameter) {
        try {
            Template t = new Template(JDUtilities.getResourceFile("plugins/webinterface/" + url).getAbsolutePath());

            t.setParam("webinterface_version", JDWebinterface.instance.getPluginID());
            if (url.startsWith("single_info.tmpl")==true) add_single_info(t, requestParameter);
            if (url.startsWith("all_info.tmpl")==true) add_all_info(t, requestParameter);            
            if (url.startsWith("index.tmpl")==true) add_status_page(t, requestParameter);
            if (url.startsWith("passwd.tmpl")==true) add_password_list(t, requestParameter);
            if (url.startsWith("link_adder.tmpl")==true) add_linkadder_page(t, requestParameter);
            
            response.addContent(t.output());
            response.setOk();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
