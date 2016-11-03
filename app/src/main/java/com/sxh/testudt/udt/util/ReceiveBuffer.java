package com.sxh.testudt.udt.util;

import com.sxh.testudt.udt.UDTInputStream;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 
 * The receive buffer stores data chunks to be read by the application
 *
 * @author schuller
 */
public class ReceiveBuffer {

	private final UDTInputStream.AppData[]buffer;

	//the head of the buffer: contains the next chunk to be read by the application, 
	//i.e. the one with the lowest sequence number
	private volatile int readPosition=0;

	//the lowest sequence number stored in this buffer
	private final long initialSequenceNumber;

	//the highest sequence number already read by the application
	private long highestReadSequenceNumber;

	//number of chunks
	private final AtomicInteger numValidChunks=new AtomicInteger(0);

	//lock and condition for poll() with timeout
	private final Condition notEmpty;
	private final ReentrantLock lock;

	//the size of the buffer
	private final int size;

	public ReceiveBuffer(int size, long initialSequenceNumber){
		this.size=size;
		this.buffer=new UDTInputStream.AppData[size];
		this.initialSequenceNumber=initialSequenceNumber;
		lock=new ReentrantLock(false);
		notEmpty=lock.newCondition();
		highestReadSequenceNumber=SequenceNumber.decrement(initialSequenceNumber);
	}

	public boolean offer(UDTInputStream.AppData data){
		if(numValidChunks.get()==size) {
			return false;
		}
		lock.lock();
		try{
			long seq=data.getSequenceNumber();
			//if already have this chunk, discard it
			if(SequenceNumber.compare(seq, initialSequenceNumber)<0)return true;
			//else compute insert position
			int offset=(int)SequenceNumber.seqOffset(initialSequenceNumber, seq);
			int insert=offset% size;
			buffer[insert]=data;
			numValidChunks.incrementAndGet();
			notEmpty.signal();
			return true;
		}finally{
			lock.unlock();
		}
	}

	/**
	 * return a data chunk, guaranteed to be in-order, waiting up to the
	 * specified wait time if necessary for a chunk to become available.
	 *
	 * @param timeout how long to wait before giving up, in units of
	 *        <tt>unit</tt>
	 * @param units a <tt>TimeUnit</tt> determining how to interpret the
	 *        <tt>timeout</tt> parameter
	 * @return data chunk, or <tt>null</tt> if the
	 *         specified waiting time elapses before an element is available
	 * @throws InterruptedException if interrupted while waiting
	 */
	public UDTInputStream.AppData poll(int timeout, TimeUnit units)throws InterruptedException{
		lock.lockInterruptibly();
		long nanos = units.toNanos(timeout);

		try {
			for (;;) {
				if (numValidChunks.get() != 0) {
					return poll();
				}
				if (nanos <= 0)
					return null;
				try {
					nanos = notEmpty.awaitNanos(nanos);
				} catch (InterruptedException ie) {
					notEmpty.signal(); // propagate to non-interrupted thread
					throw ie;
				}

			}
		} finally {
			lock.unlock();
		}
	}


	/**
	 * return a data chunk, guaranteed to be in-order. 
	 */
	public UDTInputStream.AppData poll(){
		if(numValidChunks.get()==0){
			return null;
		}
		UDTInputStream.AppData r=buffer[readPosition];
		if(r!=null){
			long thisSeq=r.getSequenceNumber();
			if(1==SequenceNumber.seqOffset(highestReadSequenceNumber,thisSeq)){
				increment();
				highestReadSequenceNumber=thisSeq;
			}
			else return null;
		}
		return r;
	}

	public int getSize(){
		return size;
	}

	void increment(){
		buffer[readPosition]=null;
		readPosition++;
		if(readPosition==size)readPosition=0;
		numValidChunks.decrementAndGet();
	}

	public boolean isEmpty(){
		return numValidChunks.get()==0;
	}

}
