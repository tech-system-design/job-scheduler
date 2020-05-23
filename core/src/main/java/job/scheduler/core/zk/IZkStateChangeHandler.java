package job.scheduler.core.zk;

/**
 * Interface to watch ZK session state.
 */
public interface IZkStateChangeHandler {
  String name();
  void beforeInitializingSession();
  void afterInitializingSession();
  void onAuthFailure();
}
