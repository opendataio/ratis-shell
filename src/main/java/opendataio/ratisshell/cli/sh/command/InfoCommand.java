package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.protocol.GroupInfoReply;

import java.io.IOException;

/**
 * Command for querying ratis group information.
 */
public class InfoCommand extends AbstractRatisCommand {

  /**
   * @param context command context
   */
  public InfoCommand(Context context) {
    super(context);
  }

  @Override
  public String getCommandName() {
    return "info";
  }

  @Override
  public int run(CommandLine cl) throws IOException {
    super.run(cl);
    mPrintStream.println("group id: " + mRaftGroup.getGroupId().getUuid());
    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      GroupInfoReply reply =
          client.getGroupManagementApi(peers.get(0).getId()).info(mRaftGroup.getGroupId());
      processReply(reply,
          "failed to get info");
      mPrintStream.println("leader id: " + getLeaderId(reply.getRoleInfoProto()));
      mPrintStream.println(reply.getCommitInfos());
    }
    return 0;
  }

  @Override
  public String getUsage() {
    return String.format("%s"
        + " [-%s PEER0_HOST:PEER0_PORT,PEER1_HOST:PEER1_PORT,PEER2_HOST:PEER2_PORT]"
        + " [-%s RAFT_GROUP_ID]"
        + " [-%s SERVICE_ID]",
        getCommandName(), PEER_OPTION_NAME, GROUPID_OPTION_NAME, SERVICE_ID_OPTION_NAME);
  }

  @Override
  public String getDescription() {
    return description();
  }

  @Override
  public Options getOptions() {
    return new Options()
        .addOption(PEER_OPTION_NAME, true, "Peer addresses seperated by comma")
        .addOption(GROUPID_OPTION_NAME, true, "Raft group id")
        .addOption(SERVICE_ID_OPTION_NAME, true, "Service id");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Display the information of a specific raft group";
  }
}
