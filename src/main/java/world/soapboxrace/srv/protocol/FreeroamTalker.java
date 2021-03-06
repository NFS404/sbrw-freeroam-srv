package world.soapboxrace.srv.protocol;

import java.nio.ByteBuffer;
import java.util.Date;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

public class FreeroamTalker {

	private DatagramPacket datagramPacket;
	private ChannelHandlerContext ctx;
	private boolean ready = false;
	private FreeroamVisibleTalkers freeroamVisibleTalkers;
	private long latestTime = new Date().getTime();
	private int countA = 0;
	private SbrwParser sbrwParser;
	private boolean removing = false;

	public FreeroamTalker(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
		this.ctx = ctx;
		this.datagramPacket = datagramPacket;
		freeroamVisibleTalkers = new FreeroamVisibleTalkers();
	}

	public Integer getPort() {
		return datagramPacket.sender().getPort();
	}

	public void send(byte[] packetData) {
		ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(packetData), datagramPacket.sender()));
	}

	public void broadcastPlayersInfo() {
		freeroamVisibleTalkers.broadcastPlayersInfoToTalker(this);
	}

	public void broadcastPlayersXYZ() {
		freeroamVisibleTalkers.broadcastPlayersXYZToTalker(this);
	}

	public void parsePlayerInfo(byte[] playerInfo) {
		if (sbrwParser == null) {
			sbrwParser = new SbrwParser(playerInfo);
		} else {
			sbrwParser.parseInputData(playerInfo);
		}
		latestTime = new Date().getTime();
		ready = true;
	}

	public boolean isReady() {
		return ready && isAlive();
	}

	public byte[] getPlayerInfo() {
		return sbrwParser.getPlayerPacket();
	}

	public byte[] getPlayerXYZ() {
		return sbrwParser.getStatePosPacket();
	}

	public FreeroamVisibleTalkers getFreeroamVisibleTalkers() {
		return freeroamVisibleTalkers;
	}

	public int getXPos() {
		return sbrwParser.getXPos();
	}

	public int getYPos() {
		return sbrwParser.getYPos();
	}

	public int getPlayerId() {
		return sbrwParser.getPlayerId();
	}

	public boolean isAlive() {
		long now = new Date().getTime();
		long aliveTime = now - latestTime;
		return (aliveTime < 2000L);
	}

	public byte[] getSequence() {
		return ByteBuffer.allocate(2).putShort((short) countA++).array();
	}

	public void sendFullPacket(byte[] packet) {
		ByteBuffer byteBuff = ByteBuffer.allocate(packet.length + 5);
		byteBuff.put((byte) 0x00);
		byteBuff.put((byte) 0x00);
		byteBuff.put((byte) 0x07);
		byteBuff.put((byte) 0xff);
		byteBuff.put(packet);
		byteBuff.put((byte) 0x01);
		send(byteBuff.array());
	}

	public void removeIdleVisibleTalkers() {
		freeroamVisibleTalkers.removeIdleVisibleTalkerToTalker();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		FreeroamTalker freeroamTalker = (FreeroamTalker) obj;
		return freeroamTalker.getPort().equals(this.getPort());
	}

	@Override
	public String toString() {
		return "[" + getPlayerId() + "]";
	}

	public boolean isRemoving() {
		return removing;
	}

	public void removingSwitch() {
		removing = !removing;
	}

}
