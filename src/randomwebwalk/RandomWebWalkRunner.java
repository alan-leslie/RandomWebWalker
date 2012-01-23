package randomwebwalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import org.openqa.selenium.WebDriverException;
import randomwebwalk.browser.Browser;
import randomwebwalk.browser.Hyperlink;
import randomwebwalk.browser.Page;

/**
 *
 * @author al
 * Class encompassing basic functionality (go forward, back, error status etc.)
 * for walking the web.
 * @invariant - type of walk is set to one value alone.
 * @invariant - the Logger is a valid logger.
 * Note that the web browser can be set to null (before start and when stopped),
 * to a valid browser and to an invalid browser (the browser window has been
 * shut down without a pause/stop). In certain cases it should recover from
 * incorrect states (see precons below for more details).
 * Normally expect to have only one object of this class in a program.
 */
public class RandomWebWalkRunner {

    // enum indication of the current status of the walk
    public enum WalkStatus {

        successfulStep,
        pageNotEnglish,
        permissionDenied,
        pageTimedOut,
        pageNotFound,
        pageDeadEnd,
        loginFailure,
        failedStep
    ,   complete};

    // type of walk - crucial to decision of where the walk starts, how a
    // random link is selected and decisions on walkStatus
    public enum WalkType {

        stumbleUpon,
        randomArticle,
        delicious,
        trail,
        free
    };
    
    private Browser webBrowser = null;
    private final Logger theLogger;
    private WalkStatus walkStatus = WalkStatus.successfulStep;
    private int failureCount = 0;
    private final WalkType theType;
    private boolean shouldRandomize = false;    // how a random link should be picked
    private String defaultLinkText = "";     // the link that should be selected if applicable
    private URL initialURL = null; // starting URL
    private final String theTrailFileName; // name of file that includes trail to be followed
    private List<URL> theTrail = null;  // trail of urls to be visited
    private Iterator<URL> trailIterator = null;
    private boolean shouldDumpScreen = false;
    private String dumpDirBase = "./dumpDir";
    private String dumpDirName = dumpDirBase;    
    private int dumpFileNumber = 1;

    /**
     *
     * @param newType - the type of walk required
     * @param trailFile 
     * @param newLogger - valid logger for output info
     * @throws MalformedURLException 
     * @precon - as per invariant/param spec
     * @postcon - as per invariant
     */
    public RandomWebWalkRunner(WalkType newType,
            String trailFile,
            Logger newLogger) throws MalformedURLException {
        theType = newType;

        switch (theType) {
            case randomArticle:
                defaultLinkText = "Random article";
                initialURL = new URL("http://en.wikipedia.org/wiki/Main_Page");
                shouldRandomize = false;
                break;
            case stumbleUpon:
                defaultLinkText = "Stumble!";
                initialURL = new URL("http://www.stumbleupon.com/login.php");
                shouldRandomize = false;
                break;
            case trail:
                defaultLinkText = "";
                shouldRandomize = false;
                break;
            case delicious:
                defaultLinkText = "";
                shouldRandomize = false;
                break;
            case free:
                defaultLinkText = "";
                shouldRandomize = true;
                break;
        }

        theTrailFileName = trailFile;
        theLogger = newLogger;
    }

    /**
     * 
     * @param newInitialURL
     */
    public void setInitialURL(URL newInitialURL) {
        initialURL = newInitialURL;
    }

    /**
     * Start up the walk by logging into the web site if required.
     * @param idString - a valid iidentifier or null
     * @param passwordString - the password for the identifier or null
     * @param profileId - a profile (defaults if invalid) identifier or null
     * @throws WebDriverException - when the browser is invalid (e.g. the
     * attached firefox browser has been shut down without a pause/stop)
     * @precon - as per invariant/param spec
     * @postcon - the firefox browser is on the correct page to begin walk if
     * the status is set to successfulStep, otherwise set to loginFailure
     * (in case of login failure) or pageTimeout if there has been a socket
     * timeout.
     * @postcon - as per invariant
     */
    public void startUp(String idString,
            String passwordString,
            String profileId) throws WebDriverException {
        theLogger.log(Level.INFO, "Start up");
        webBrowser = new Browser(profileId, theLogger);
        boolean isStumbleUpon = (theType == WalkType.stumbleUpon);

        try {
            if (theType == WalkType.trail) {
                initTrail();
            }
            webBrowser.start(initialURL, isStumbleUpon, idString, passwordString);
            
            if(theType == WalkType.delicious){
                Page webPage = webBrowser.getCurrentPage();
                Hyperlink link = null;

                link = webPage.getLinkFromText("Browse these bookmarks");
                webBrowser.goForward(link);
            }
            setStatus(WalkStatus.successfulStep);
        } catch (LoginException ex) {
            theLogger.log(Level.SEVERE, null, ex);
            setStatus(WalkStatus.loginFailure);
        } catch (WebDriverException theEx) {
            if (isExceptionTimeout(theEx)) {
                theLogger.log(Level.WARNING,
                        "Socket Timeout exception", theEx);
                webBrowser.stopPageLoad();
                setStatus(WalkStatus.pageTimedOut);
            } else {
                throw theEx;
            }
        }
    }

    /**
     *
     * @return - the type of walk
     * @precon - as per invariant
     * @postcon - as per invariant/return value
     */
    public WalkType getType() {
        return theType;
    }

    /**
     *
     * @precon - as per invariant
     * @postcon - the browser is pointing to the last page that was
     * successfully visited
     * @postcon - as per invariant/return value
     */
    public void restore() {
        Page theCurrentPage = webBrowser.getCurrentPage();

        if (theCurrentPage == null) {
            webBrowser.addNewPage();
        } else {
            if (webBrowser.hasPageMoved()) {
                webBrowser.restorePage();
            }
        }
    }

    /**
     * @precon - as per invariant
     * @postcon - as per invariant
     * @postcon - the status is set to successful
     */
    public void pause() {
        setStatus(RandomWebWalkRunner.WalkStatus.successfulStep);
    }

    /**
     *
     * @return - whether the page that the browser is currently pointing to is
     * different from the one last visited by the browser.
     * @precon - as per invariant
     * @postcon - as per invariant (no change to internal state).
     */
    public boolean hasPageMoved() {
        return webBrowser.hasPageMoved();
    }

    /**
     * @precon - as per invariant
     * @postcon - loading of the page in the browser has been interrupted.
     * @postcon - browser is closed.
     */
    public void stop() {
        theLogger.log(Level.INFO, "Stop");

        if (webBrowser != null) {
            try {
                webBrowser.quit();
            } catch (WebDriverException ex) {
                theLogger.log(Level.INFO, "WebDriverException caught on trying to close down - ignored");
            }
            webBrowser = null;
        }

        setStatus(WalkStatus.successfulStep);
    }

    /**
     * steps forward to next random link.
     * @precon - as per invariant
     * @postcon - that the browser has moved on one page and status is set to
     * success.
     * @postcon - or status is set as failed after x number of incorrect attempts.
     * @postcon - or status is set to a value that reflects the reason for failure.
     * @throws WebDriverException - if the step forward fails because of socket
     * timeout.
     */
    public void step() throws WebDriverException {
        theLogger.log(Level.INFO, "Step");
        String currentPageURL = webBrowser.getCurrentPageURL();

        theLogger.log(Level.INFO, "Current page: {0}",
                currentPageURL);
        try {
            Page webPage = webBrowser.getCurrentPage();
            Hyperlink link = null;

            switch (theType) {
                case stumbleUpon:
                case randomArticle: {
                    link = webPage.getLinkFromText(defaultLinkText);
                    webBrowser.goForward(link);
                }
                break;
                case delicious: 
                {
                    link = webPage.getLinkFromId("nextLink");
                    webBrowser.goForward(link);
                }
                break;
                case trail: {
                    if (trailIterator != null){
                       if(trailIterator.hasNext()) {
                            String theURL = trailIterator.next().toString();
                            webBrowser.gotoURL(theURL);
                        } else {
                         // todo - need to stop at end 
                            setStatus(WalkStatus.complete);
                        }
                    }
                }
                break;
                default: {
                    if (shouldRandomize) {
                        link = webPage.getRandomLink();
                        if (webBrowser.hasAlreadyBeenVisited(link)) {
                            link = webPage.getRandomLink();
                        }
                    }

                    webBrowser.goForward(link);
                }
                break;
            }

            Page newPage = webBrowser.getCurrentPage();
            String newPageURL = newPage.getURL();
            theLogger.log(Level.INFO, "New page: {0}", newPageURL);

            if (theType == WalkType.free) {
                if (!newPage.isInEnglish()) {
                    setStatus(WalkStatus.pageNotEnglish);
                } else {
                    if (currentPageURL.equalsIgnoreCase(newPage.getURL())
                            || newPage.isDeadEnd()) {
                        setStatus(WalkStatus.pageDeadEnd);
                    } else {
                        setStatus(WalkStatus.successfulStep);
                    }
                }
            } else {
                if(checkStatus() != WalkStatus.complete){
                    setStatus(WalkStatus.successfulStep);
                }
            }

            theLogger.log(Level.INFO, "Status set");
        } catch (WebDriverException theEx) {
            if (isExceptionTimeout(theEx)) {
                theLogger.log(Level.WARNING,
                        "Socket Timeout exception", theEx);
                webBrowser.stopPageLoad();
                setStatus(WalkStatus.pageTimedOut);
            } else {
                throw theEx;
            }
        }

        if (checkStatus() == WalkStatus.successfulStep) {
            if(shouldDumpScreen){
                String dumpFilePath = dumpDirName + "/dump" + Integer.toString(dumpFileNumber) + ".png";
                
                try {
                    webBrowser.dumpScreen(dumpFilePath);
                    ++dumpFileNumber;
                } catch (IOException ex) {
                    theLogger.log(Level.WARNING, null, ex);
                }
            }
        } else {
            if (failureCount > 3) {
                setStatus(WalkStatus.failedStep);
            }
        }
    }

    /**
     * causes the browser to refresh the current page.
     * @precon - as per invariant
     * @postcon - as per invariant
     * @throws WebDriverException - if there is a socket timeout.
     */
    public void refresh() throws WebDriverException {
        theLogger.log(Level.INFO, "Refresh");
        String currentPageURL = webBrowser.getCurrentPageURL();

        try {
            webBrowser.refresh();

            Page newPage = webBrowser.getCurrentPage();

            if (theType == WalkType.randomArticle
                    || theType == WalkType.stumbleUpon) {
                setStatus(WalkStatus.successfulStep);
            } else {
                if (!newPage.isInEnglish()) {
                    setStatus(WalkStatus.pageNotEnglish);
                } else {
                    if (currentPageURL.equalsIgnoreCase(newPage.getURL())
                            || newPage.isDeadEnd()) {
                        setStatus(WalkStatus.pageDeadEnd);
                    } else {
                        setStatus(WalkStatus.successfulStep);
                    }
                }
            }
        } catch (WebDriverException theEx) {
            if (isExceptionTimeout(theEx)) {
                theLogger.log(Level.WARNING,
                        "Socket Timeout exception", theEx);
                webBrowser.stopPageLoad();
                setStatus(WalkStatus.pageTimedOut);
            } else {
                throw theEx;
            }
        }
    }

    /**
     * goes back to the previously (successfully visited) page.
     * @precon - as per invariant
     * @postcon - the browser is pointing to the previous page.
     * @postcon - as per invariant
     * @throws WebDriverException - if it was unsuccessful.
     */
    public void goBack() throws WebDriverException {
        theLogger.log(Level.INFO, "GoBack");
        try {
            webBrowser.goBack();
            setStatus(WalkStatus.successfulStep);
        } catch (WebDriverException theEx) {
            if (isExceptionTimeout(theEx)) {
                theLogger.log(Level.WARNING,
                        "Socket Timeout exception", theEx);
                webBrowser.stopPageLoad();
                setStatus(WalkStatus.pageTimedOut);
            } else {
                throw theEx;
            }
        }
    }

    /**
     * @return - whether the walk has been successfully started.
     * @precon - as per invariant
     * @postcon - as per invariant/return
     */
    public boolean isStarted() {
        if (webBrowser != null) {
            if (webBrowser.containsLink(defaultLinkText)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return - the current status of the walk.
     * @precon - as per invariant
     * @postcon - as per invariant/return
     */
    public RandomWebWalkRunner.WalkStatus checkStatus() {
        return walkStatus;
    }

    /**
     * @return - whether the action required has failed or not.
     * @precon - as per invariant
     * @postcon - as per invariant/return
     */
    public boolean hasFailed() {
        if (walkStatus == WalkStatus.successfulStep) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param newStatus - the new status for the walker
     * @precon - as per invariant
     * @postcon - as per invariant
     * @postcon - status is as per the newStatus param
     */
    public void setStatus(WalkStatus newStatus) {
        theLogger.log(Level.INFO, "SetStatus: " + newStatus.toString(),
                newStatus);
        if (newStatus == WalkStatus.successfulStep) {
            failureCount = 0;
        } else {
            ++failureCount;
        }

        walkStatus = newStatus;
    }
    
    /**
     * 
     * @param shouldDumpScreen
     */
    public void setShouldDump(boolean shouldDumpScreen) {
        if (shouldDumpScreen) {
            boolean exists = true;
            int dirNumber = 1;

            while (exists) {
                dumpDirName = dumpDirBase + Integer.toString(dirNumber);
                exists = (new File(dumpDirName)).exists();

                if (exists) {
                    ++dirNumber;
                } 
            }

            boolean success = (new File(dumpDirName)).mkdir();
            if (success) {
                dumpFileNumber = 1;
                this.shouldDumpScreen = shouldDumpScreen;
            }
        }
    }

    /**
     * checks whether the exception to be examined is a timeout. So this has
     * nothing to do with the state of this object.
     * @param ex - the exception to be examined
     * @return - whether that exception is actually a timeout (usually a socket
     * timeout).
     * @precon - as per invariant
     * @postcon - as per invariant
     * @postcon - no change to internal state.
     */
    private boolean isExceptionTimeout(Exception ex) {
        boolean theResult = false;

        Throwable theCause = ex; //.getCause();
        if (theCause != null) {
            if (theCause instanceof WebDriverException) {
                Throwable theRealCause = theCause.getCause();

                if (theRealCause instanceof SocketTimeoutException) {
                    theResult = true;
                }
            }
        }

        return theResult;
    }
    
    /**
     *
     * @precon - as per invariant
     * @postcon - as per invariant/return value
     */
    private void initTrail(){
        theTrail = new ArrayList<URL>();
        FileReader theReader = null;

        try {
            theReader = new FileReader(theTrailFileName);
            BufferedReader in = new BufferedReader(theReader);
            
            String theURLAsString = null;
            
            while ((theURLAsString = in.readLine()) != null) {
                try{
                    URL theTrailURL = new URL(theURLAsString);
                    theTrail.add(theTrailURL);
                } catch(MalformedURLException ex) {
                    theLogger.log(Level.WARNING, "Failed making URL from{0}", theURLAsString);
                }
            }
        } catch (IOException e) {
            // ...
        } finally {
            if (null != theReader) {
                try {
                    theReader.close();
                } catch (IOException e) {
                    /* .... */
                }
            }
        }
        
        trailIterator = theTrail.iterator();
        if(trailIterator.hasNext()){
            initialURL = trailIterator.next();         
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RandomWebWalkRunner other = (RandomWebWalkRunner) obj;
        if (this.walkStatus != other.walkStatus) {
            return false;
        }
        if (this.failureCount != other.failureCount) {
            return false;
        }
        if (this.theType != other.theType) {
            return false;
        }
        if (this.shouldRandomize != other.shouldRandomize) {
            return false;
        }
        if ((this.defaultLinkText == null) ? (other.defaultLinkText != null) : !this.defaultLinkText.equals(other.defaultLinkText)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.walkStatus != null ? this.walkStatus.hashCode() : 0);
        hash = 29 * hash + this.failureCount;
        hash = 29 * hash + (this.theType != null ? this.theType.hashCode() : 0);
        hash = 29 * hash + (this.shouldRandomize ? 1 : 0);
        hash = 29 * hash + (this.defaultLinkText != null ? this.defaultLinkText.hashCode() : 0);
        return hash;
    }
}
