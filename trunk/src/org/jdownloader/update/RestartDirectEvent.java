package org.jdownloader.update;

import java.io.File;
import java.io.IOException;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;

public class RestartDirectEvent extends ShutdownEvent {
    private static final RestartDirectEvent INSTANCE = new RestartDirectEvent();

    /**
     * get the only existing instance of RestartDirectEvent. This is a singleton
     * 
     * @return
     */
    public static RestartDirectEvent getInstance() {
        return RestartDirectEvent.INSTANCE;
    }

    /**
     * Create a new instance of RestartDirectEvent. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private RestartDirectEvent() {
        super();
        this.setHookPriority(Integer.MIN_VALUE);
    }

    @Override
    public void run() {

        final String tiny[] = new String[] { CrossSystem.getJavaBinary(), "-jar", RestartController.UPDATER_JARNAME, "-noupdate", "-guiless", "-restart", RestartController.getRestartCommandLine() };
        if (Application.getResource(RestartController.JARNAME).exists()) {
            System.out.println(Application.getResource(RestartController.JARNAME) + " exists");
        } else {
            System.err.println(Application.getResource(RestartController.JARNAME) + " is Missing");
        }

        /*
         * build complete call arguments for tinybootstrap
         */
        final StringBuilder sb = new StringBuilder();

        for (final String arg : tiny) {
            sb.append(arg + " ");
        }

        System.out.println("UpdaterCall: " + sb.toString());

        final ProcessBuilder pb = new ProcessBuilder(tiny);
        /*
         * needed because the root is different for jre/class version
         */
        File pbroot = null;

        pbroot = new File(Application.getHome());

        System.out.println("Root: " + pbroot);
        pb.directory(pbroot);
        try {
            pb.start();
        } catch (final IOException e) {
        }

    }

}
