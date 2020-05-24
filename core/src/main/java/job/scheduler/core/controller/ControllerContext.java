package job.scheduler.core.controller;

public class ControllerContext {
  public int epoch = JobSchedulerController.INITIAL_CONTROLLER_EPOCH;
  public int epochZkVersion = JobSchedulerController.INITIAL_CONTROLLER_EPOCH_ZK_VERSION;;
}
