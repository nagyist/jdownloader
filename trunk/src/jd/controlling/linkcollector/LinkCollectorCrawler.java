package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.LinkCrawler;

public class LinkCollectorCrawler extends LinkCrawler implements LinkCollectorListener {

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case ABORT:
            stopCrawling();
            break;
        }
    }

}
