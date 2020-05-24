package job.scheduler.core.zk;

/**
 * Interface to watch ZK node changes.
 */
public interface IZkNodeChangeHandler {
  String path();
  void handleCreation();
  void handleDeletion();
  void handleDataChange();
  void handleChildChange();
}