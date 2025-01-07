package com.turn.ttorrent.tracker;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // First, instantiate a Tracker object with the port you want it to listen on.
// The default tracker port recommended by the BitTorrent protocol is 6969.
        Tracker tracker = new Tracker(6969);

// Then, for each torrent you wish to announce on this tracker, simply created
// a TrackedTorrent object and pass it to the tracker.announce() method:
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".torrent");
            }
        };

        for (File f : new File("C:/Users/MasterIlidan/Downloads/").listFiles(filter)) {
            tracker.announce(TrackedTorrent.load(f));
        }

//Also you can enable accepting foreign torrents.
//if tracker accepts request for unknown torrent it starts tracking the torrent automatically
        tracker.setAcceptForeignTorrents(false);

// Once done, you just have to start the tracker's main operation loop:
        tracker.start(true);

// You can stop the tracker when you're done with:
        //tracker.stop();
    }
}
