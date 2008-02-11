package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class FileFactory extends PluginForHost {
    static private final String host = "filefactory.com";

    private String version = "1.2.0.0";
    //http://www.filefactory.com/file/b1bf90/
    //www.filefactory.com/f/ef45b5179409a229/ 
    static private final Pattern patternSupported = Pattern.compile("http://.*?filefactory\\.com/file/.{6}/?", Pattern.CASE_INSENSITIVE);

    /**
     * Das findet die Captcha URL
     */
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");

    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");
    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    /**
     * <a target="_top"
     * href="http://archive01.filefactory.com/dl/f/cd66d1//b/3/h/4bb297a8a6f12168/"><img
     * src
     */
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");

    // TODO CaptchaWrong
   // private static Pattern patternErrorCaptchaWrong = Pattern.compile("(Sorry, the verification code you entered was incorrect)", Pattern.CASE_INSENSITIVE);

    private static final String DOWNLOAD_INFO = "<h1 style=\"width:370px;\">°</h1>°<p>°Size:°MB<br />°Description: ";

    private String captchaAddress;

    private String postTarget;

    private String actionString;

    private RequestInfo requestInfo;

    @Override
    public String getCoder() {
        return "GforE/JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return host + " - " + version;
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public FileFactory() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    // @Override
    // public URLConnection getURLConnection() {
    // return null;
    // }
    @Override
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        DownloadLink downloadLink = null;
        try {
            logger.info("Step: " + step);
            downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME :
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                    if (requestInfo.getHtmlCode().indexOf("this file is no longer available") >= 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        return step;
                    }
                    if (requestInfo.containsHTML("no free download slots")) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        return step;
                    }
                    String newURL = "http://"+requestInfo.getConnection().getURL().getHost()+getFirstMatch(requestInfo.getHtmlCode(), baseLink, 1);
                    System.out.println(newURL);
                    logger.info(newURL);
                    if (newURL != null) {
                        newURL = newURL.replaceAll("' \\+ '", "");
                        requestInfo = getRequest((new URL(newURL)), null, downloadLink.getName(), true);
                        actionString = "http://www.filefactory.com/" + getFirstMatch(requestInfo.getHtmlCode(), frameForCaptcha, 1);
                        actionString = actionString.replaceAll("&amp;", "&");
                        System.out.println(actionString);
                        requestInfo = getRequest((new URL(actionString)), "viewad11=yes", newURL, true);
                        // captcha Adresse finden
                        captchaAddress = "http://www.filefactory.com" + getFirstMatch(requestInfo.getHtmlCode(), patternForCaptcha, 1);
                        captchaAddress = captchaAddress.replaceAll("&amp;", "&");
                        // post daten lesen
                        System.out.println(captchaAddress);
                        postTarget = getFormInputHidden(requestInfo.getHtmlCode());
                    }
                    logger.info(captchaAddress + " : " + postTarget);
                    if (captchaAddress == null || postTarget == null) {
                    }
                    step.setStatus(PluginStep.STATUS_DONE);
                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE :
                    File file = this.getLocalCaptchaFile(this);

                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                    } else {
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                    }
                    break;
                case PluginStep.STEP_DOWNLOAD :
                    try {
                        requestInfo = postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + (String) steps.get(1).getParameter(), true);
                        postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
                        postTarget = postTarget.replaceAll("&amp;", "&");
                    } catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                    }

                    try {
                        URLConnection urlConnection = new URL(postTarget).openConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(this.getFileNameFormHeader(urlConnection));

                        if (!hasEnoughHDSpace(downloadLink)) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        if (download(downloadLink, urlConnection)!=DOWNLOAD_SUCCESS) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            return null;
                        } else {                        
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            step.setStatus(PluginStep.STATUS_ERROR);
                        }
                    } catch (IOException e) {
                        logger.severe("URL could not be opened. " + e.toString());

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    break;
            }
        } catch (Exception e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return step;
        }
        return step;
    }
/*
    private boolean prepareDownload(DownloadLink downloadLink) {
        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            int length = urlConnection.getContentLength();
            downloadLink.setDownloadMax(length);
            downloadLink.setName(this.getFileNameFormHeader(urlConnection));
            return download(downloadLink, urlConnection);
        } catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
        }
        return false;
    }
*/
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
            if (requestInfo.getHtmlCode().indexOf("this file is no longer available") >= 0) {
                return false;
            } else {
                String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode().replaceAll("\\&\\#8203\\;", ""), DOWNLOAD_INFO, 0));
                String fileSize = getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 3);

                downloadLink.setName(fileName);
                if (fileSize != null) {
                    try {
                        int length = (int) (Double.parseDouble(fileSize.trim()) * 1024 * 1024);
                        downloadLink.setDownloadMax(length);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        //
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.filefactory.com/info/terms.php";
    }
}
