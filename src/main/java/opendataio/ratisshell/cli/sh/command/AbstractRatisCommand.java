package opendataio.ratisshell.cli.sh.command;

import opendataio.ratisshell.cli.Command;
import opendataio.ratisshell.cli.RaftUtils;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.RaftClientReply;

import java.io.IOException;
import java.io.PrintStream;

/**
 * The base class for all the ratis shell {@link Command} classes.
 */
public abstract class AbstractRatisCommand implements Command {
  protected final PrintStream mPrintStream;

  protected AbstractRatisCommand(Context context) {
    mPrintStream = context.getPrintStream();
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
