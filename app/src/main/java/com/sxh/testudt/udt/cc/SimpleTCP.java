package com.sxh.testudt.udt.cc;

import com.sxh.testudt.udt.UDTCongestionControl;
import com.sxh.testudt.udt.UDTSession;

import java.util.List;


/**
 * simple TCP CC algorithm from the paper 
 * "Optimizing UDP-based Protocol Implementations" by Y. Gu and R. Grossmann
 */
public class SimpleTCP extends UDTCongestionControl
{

	public SimpleTCP(UDTSession session){
		super(session);
	}

	@Override
	public void init() {
		packetSendingPeriod=0;
		congestionWindowSize=2;
		setAckInterval(2);
	}

	@Override
	public void onACK(long ackSeqno) {
		congestionWindowSize += 1/congestionWindowSize;
	}

	@Override
	public void onLoss(List<Integer> lossInfo) {
		congestionWindowSize *= 0.5;
	}
	
	
}
