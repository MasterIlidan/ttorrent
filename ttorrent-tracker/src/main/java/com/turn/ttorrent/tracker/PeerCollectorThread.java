package com.turn.ttorrent.tracker;

/**
 * The unfresh peer collector thread.
 * <p>
 * <p>
 * Every PEER_COLLECTION_FREQUENCY_SECONDS, this thread will collect
 * unfresh peers from all announced torrents.
 * </p>
 */
public class PeerCollectorThread extends Thread {

  public static final int COLLECTION_FREQUENCY = 180;
  private final TorrentsRepository myTorrentsRepository;
  private volatile int myTorrentExpireTimeoutSec = 1200 * 60;

  public PeerCollectorThread(TorrentsRepository torrentsRepository) {
    myTorrentsRepository = torrentsRepository;
  }

  public void setTorrentExpireTimeoutSec(int torrentExpireTimeoutSec) {
    myTorrentExpireTimeoutSec = torrentExpireTimeoutSec;
  }

  @Override
  public void run() {
    while (!isInterrupted()) {
      try {
        Thread.sleep(COLLECTION_FREQUENCY * 1000);
      } catch (InterruptedException ie) {
        break;
      }
      //Cleanup expired torrents
      //myTorrentsRepository.cleanup(myTorrentExpireTimeoutSec);
    }
  }
}
