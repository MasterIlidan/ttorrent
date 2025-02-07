package com.turn.ttorrent.tracker;

import com.turn.ttorrent.common.TorrentLoggerFactory;
import com.turn.ttorrent.common.protocol.AnnounceRequestMessage;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class TorrentsRepository {

    private final ReentrantLock[] myLocks;
    private final ConcurrentMap<String, TrackedTorrent> myTorrents;
    private static final Logger logger = TorrentLoggerFactory.getLogger(Tracker.class);

    public TorrentsRepository(int locksCount) {

        if (locksCount <= 0) {
            throw new IllegalArgumentException("Lock count must be positive");
        }

        myLocks = new ReentrantLock[locksCount];
        for (int i = 0; i < myLocks.length; i++) {
            myLocks[i] = new ReentrantLock();
        }
        myTorrents = new ConcurrentHashMap<String, TrackedTorrent>();
    }

    public TrackedTorrent getTorrent(String hexInfoHash) {
        return myTorrents.get(hexInfoHash);
    }

    public void putIfAbsent(String hexInfoHash, TrackedTorrent torrent) {
        myTorrents.putIfAbsent(hexInfoHash, torrent);
    }

    public TrackedTorrent putIfAbsentAndUpdate(String hexInfoHash, TrackedTorrent torrent,
                                               AnnounceRequestMessage.RequestEvent event, ByteBuffer peerId,
                                               String hexPeerId, String ip, int port, long uploaded, long downloaded,
                                               long left) throws UnsupportedEncodingException {
        TrackedTorrent actualTorrent;
        try {
            lockFor(hexInfoHash).lock();
            TrackedTorrent oldTorrent = myTorrents.putIfAbsent(hexInfoHash, torrent);
            actualTorrent = oldTorrent == null ? torrent : oldTorrent;
            actualTorrent.update(event, peerId, hexPeerId, ip, port, uploaded, downloaded, left);
        } finally {
            lockFor(hexInfoHash).unlock();
        }
        return actualTorrent;
    }

    private ReentrantLock lockFor(String torrentHash) {
        return myLocks[Math.abs(torrentHash.hashCode()) % myLocks.length];
    }

    @SuppressWarnings("unused")
    public void clear() {
        myTorrents.clear();
    }

    public void removeTorrent(String infoHash) {
        logger.info("Unregistered torrent with hash {}",
                infoHash);
        myTorrents.remove(infoHash);
    }

    public List<TrackedTorrent> cleanup(int torrentExpireTimeoutSec) {
        List<TrackedTorrent> torrentsWithoutPeers = new ArrayList<>();
        for (TrackedTorrent trackedTorrent : myTorrents.values()) {
            try {
                lockFor(trackedTorrent.getHexInfoHash()).lock();
                trackedTorrent.collectUnfreshPeers(torrentExpireTimeoutSec);
                if (trackedTorrent.getPeers().size() == 0) {
                    torrentsWithoutPeers.add(trackedTorrent);
                    //myTorrents.remove(trackedTorrent.getHexInfoHash());
                    //logger.info("Unregistered torrent with hash {}", trackedTorrent.getHexInfoHash());
                }
            } finally {
                lockFor(trackedTorrent.getHexInfoHash()).unlock();
            }
        }
        return torrentsWithoutPeers;
    }


    public Map<String, TrackedTorrent> getTorrents() {
        return new HashMap<String, TrackedTorrent>(myTorrents);
    }

}
