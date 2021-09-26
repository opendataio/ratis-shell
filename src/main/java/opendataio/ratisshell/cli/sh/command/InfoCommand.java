package opendataio.ratisshell.cli.sh.command;

import com.google.common.annotations.VisibleForTesting;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.GroupInfoReply;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InfoCommand extends AbstractRatisCommand {
  public static final String PEER_OPTION_NAME = "peers";
  public static final String GROUPID_OPTION_NAME = "groupid";
  public static final RaftGroupId DEFAULT_ALLUXIO_RAFT_GROUP_ID
      = RaftGroupId.valueOf(
          UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1"));
  private RaftGroup mRaftGroup;

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
    List<InetSocketAddress> addresses = new ArrayList<>();
    if (cl.hasOption(PEER_OPTION_NAME)) {
      String peersStr = cl.getOptionValue(PEER_OPTION_NAME);
      String[] peersArray = peersStr.split(",");
      for (int i = 0; i< peersArray.length; i++) {
        String[] hostPortPair = peersArray[i].split(":");
        InetSocketAddress addr = new InetSocketAddress(hostPortPair[0], Integer.parseInt(hostPortPair[1]));
        addresses.add(addr);
      }
    } else {
      // TODO(maobaolong) fill addresses from config
    }

    RaftGroupId raftGroupId = DEFAULT_ALLUXIO_RAFT_GROUP_ID;
    if (cl.hasOption(GROUPID_OPTION_NAME)) {
      raftGroupId = RaftGroupId.valueOf(
          UUID.fromString(cl.getOptionValue(GROUPID_OPTION_NAME)));
    } else {
      // TODO(maobaolong) fill groupid from config
    }

    List<RaftPeer> peers = addresses.stream()
        .map(addr -> RaftPeer.newBuilder()
            .setId(RaftUtils.getPeerId(addr))
            .setAddress(addr)
            .build()
        ).collect(Collectors.toList());
    mRaftGroup = RaftGroup.valueOf(raftGroupId, peers);
    try (RaftClient client = RaftUtils.createClient(mRaftGroup)) {
      GroupInfoReply reply =
          client.getGroupManagementApi(peers.get(0).getId()).info(raftGroupId);
      processReply(reply,
          "failed to get info");
      mPrintStream.println(reply.getCommitInfos());
      mPrintStream.println("leader id: " + getLeaderId(reply.getRoleInfoProto()));
    }
    return 0;
  }

  /**
   * Get the leader id.
   *
   * @return the leader id
   */
  public String getLeaderId(RaftProtos.RoleInfoProto roleInfo) {
    if (roleInfo == null) {
      return null;
    }
    if (roleInfo.getRole() == RaftProtos.RaftPeerRole.LEADER) {
      return roleInfo.getSelf().getAddress();
    }
    RaftProtos.FollowerInfoProto followerInfo = roleInfo.getFollowerInfo();
    if (followerInfo == null) {
      return null;
    }
    return followerInfo.getLeaderInfo().getId().getId().toStringUtf8();
  }

  private void processReply(RaftClientReply reply, String msg)
      throws IOException {
    RaftUtils.processReply(reply, msg, mPrintStream);
  }

  @Override
  public String getUsage() {
    return String.format("%s"
        + " [-%s PEER0_HOST:PEER0_PORT,PEER1_HOST:PEER1_PORT,PEER2_HOST:PEER2_PORT]"
        + " [-%s RAFT_GROUP_ID]",
        getCommandName(), PEER_OPTION_NAME, GROUPID_OPTION_NAME);
  }

  @Override
  public String getDescription() {
    return description();
  }

  @Override
  public Options getOptions() {
    return new Options()
        .addOption(PEER_OPTION_NAME, true, "Peer addresses seperated by comma")
        .addOption(GROUPID_OPTION_NAME, true, "Raft group id");
  }

  /**
   * @return command's description
   */
  @VisibleForTesting
  public static String description() {
    return "Display the information of a specific raft group";
  }
}
