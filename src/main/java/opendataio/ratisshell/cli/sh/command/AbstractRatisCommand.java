package opendataio.ratisshell.cli.sh.command;

import opendataio.ratisshell.cli.Command;
import opendataio.ratisshell.cli.RaftUtils;
import opendataio.ratisshell.conf.InstancedConfiguration;
import opendataio.ratisshell.conf.PropertyKey;
import opendataio.ratisshell.conf.RatisShellConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The base class for all the ratis shell {@link Command} classes.
 */
public abstract class AbstractRatisCommand implements Command {
  public static final String SERVICE_ID_OPTION_NAME = "serviceid";
  public static final String PEER_OPTION_NAME = "peers";
  public static final String GROUPID_OPTION_NAME = "groupid";
  public static final RaftGroupId DEFAULT_ALLUXIO_RAFT_GROUP_ID
      = RaftGroupId.valueOf(
      UUID.fromString("02511d47-d67c-49a3-9011-abb3109a44c1"));
  protected final PrintStream mPrintStream;
  protected RaftGroup mRaftGroup;
  protected List<RaftPeer> peers;

  protected AbstractRatisCommand(Context context) {
    mPrintStream = context.getPrintStream();
  }

  @Override
  public int run(CommandLine cl) throws IOException {
    RatisShellConfiguration conf = InstancedConfiguration.defaults();
    List<InetSocketAddress> addresses = new ArrayList<>();
    String peersStr = "";
    if (cl.hasOption(PEER_OPTION_NAME)) {
      peersStr = cl.getOptionValue(PEER_OPTION_NAME);
    } else {
      if (cl.hasOption(SERVICE_ID_OPTION_NAME)) {
        peersStr = conf.get(
            PropertyKey.Template.RATIS_SHELL_PEER_IDS.format(
                cl.getOptionValue(SERVICE_ID_OPTION_NAME)));
      }
    }
    String[] peersArray = peersStr.split(",");
    for (int i = 0; i < peersArray.length; i++) {
      String[] hostPortPair = peersArray[i].split(":");
      InetSocketAddress addr =
          new InetSocketAddress(hostPortPair[0], Integer.parseInt(hostPortPair[1]));
      addresses.add(addr);
    }

    RaftGroupId raftGroupId = DEFAULT_ALLUXIO_RAFT_GROUP_ID;
    if (cl.hasOption(GROUPID_OPTION_NAME)) {
      raftGroupId = RaftGroupId.valueOf(
          UUID.fromString(cl.getOptionValue(GROUPID_OPTION_NAME)));
    } else {
      if (cl.hasOption(SERVICE_ID_OPTION_NAME)) {
        RaftGroupId.valueOf(
            UUID.fromString(conf.get(
                PropertyKey.Template.RATIS_SHELL_GROUP_ID.format(
                    cl.getOptionValue(SERVICE_ID_OPTION_NAME)))));
      }
    }

    peers = addresses.stream()
        .map(addr -> RaftPeer.newBuilder()
            .setId(RaftUtils.getPeerId(addr))
            .setAddress(addr)
            .build()
        ).collect(Collectors.toList());
    mRaftGroup = RaftGroup.valueOf(raftGroupId, peers);
    return 0;
  }

  @Override
  public void validateArgs(CommandLine cl) throws IllegalArgumentException {
    if (!cl.hasOption(SERVICE_ID_OPTION_NAME)
        && !cl.hasOption(PEER_OPTION_NAME)) {
      throw new IllegalArgumentException(String.format(
          "should provide at least one of [%s] and [%s]",
          SERVICE_ID_OPTION_NAME, PEER_OPTION_NAME));
    }
  }

  /**
   * Get the leader id.
   *
   * @param roleInfo the role info
   * @return the leader id
   */
  protected String getLeaderId(RaftProtos.RoleInfoProto roleInfo) {
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

  protected void processReply(RaftClientReply reply, String msg)
      throws IOException {
    RaftUtils.processReply(reply, msg, mPrintStream);
  }
}
