package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.proto.RaftProtos;
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
          client.getGroupManagementApi(
              mRaftGroup.getPeers().stream()
                  .findFirst()
                  .get()
                  .getId())
              .info(mRaftGroup.getGroupId());
      processReply(reply,
          "failed to get info");
      RaftProtos.RaftPeerProto leader =
          getLeader(reply.getRoleInfoProto());
      mPrintStream.printf("leader info: %s(%s)%n%n",
          leader.getId().toStringUtf8(), leader.getAddress());
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
    return super.getOptions();
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Display the information of a specific raft group";
  }
}
