package bufmgr;


import global.PageId;
import global.AbstractBufMgr;



import exceptions.BufferPoolExceededException;
import exceptions.InvalidFrameNumberException;
import exceptions.PagePinnedException;
import exceptions.PageUnpinnedException;


/**
 * This class should implement a Clock replacement strategy.
 */
public class Clock extends BufMgrReplacer
{
	private boolean [] referenceBits;

	public Clock() 
	{
		referenceBits = new boolean[NUMBUF];
		for( int x = 0 ; x < NUMBUF ; x++){
			referenceBits[x] = false;
		}
	}
	public Clock(AbstractBufMgr b) 
	{
		this.mgr = (BufMgr)b;
		this.frameTable = (BufMgrFrameDesc[]) mgr.getFrameTable();
		int numBuffers = mgr.getNumBuffers();
		state_bit = new int[numBuffers]; //check
		for (int index = 0; index < numBuffers; ++index)
			state_bit[index] = Available;

		int numbuffs = mgr.getNumBuffers();
		referenceBits = new boolean[numbuffs];
		for( int x = 0 ; x < numbuffs ; x++){
			referenceBits[x] = false;
		}
		
		//setBufferManager(b);
	}
	
	/**
	 * Pins a candidate page in the buffer pool.
	 * 
	 * @param frameNo
	 *            frame number of the page.
	 * @throws InvalidFrameNumberException
	 *             if the frame number is less than zero or bigger than number
	 *             of buffers.
	 * @return true if successful.
	 */
	public void pin(int frameNo) throws InvalidFrameNumberException{
		
		if(frameNo <0 ||  frameNo >= mgr.getNumBuffers()){
			throw new InvalidFrameNumberException(null,"INVALID BUFFER NUMBER.");
		}

		if(state_bit[frameNo] == Available || state_bit[frameNo] == Referenced )
			state_bit[frameNo] = Pinned;

		referenceBits[frameNo] = false;
		
	}

	/**
	 * Unpins a page in the buffer pool.
	 * 
	 * @param frameNo
	 *            frame number of the page.
	 * @throws InvalidFrameNumberException
	 *             if the frame number is less than zero or bigger than number
	 *             of buffers.
	 * @throws PageUnpinnedException
	 *             if the page is originally unpinned.
	 * @return true if successful.
	 */
	public boolean unpin(int frameNo) throws InvalidFrameNumberException,
			PageUnpinnedException{
		
		if(frameNo <0 ||  frameNo > mgr.getNumBuffers()){
			throw new InvalidFrameNumberException(null,"INVALID BUFFER NUMBER.");
		}

		if(state_bit[frameNo] == Pinned){
			state_bit[frameNo] = Referenced;
			BufMgrFrameDesc [] frame = mgr.getFrameTable();
			if(frame[frameNo].getPinCount() ==0){
				referenceBits[frameNo] = true;
			}
		}
		return true;
	}

	/**
	 * Frees and unpins a page in the buffer pool.
	 * 
	 * @param frameNo
	 *            frame number of the page.
	 * @throws PagePinnedException
	 *             if the page is pinned.
	 */
	public void free(int frameNo) throws PagePinnedException
	{
		if(state_bit[frameNo]== Referenced){
			state_bit[frameNo] = Available;
		}
		else if (state_bit[frameNo] == Pinned){
			throw new PagePinnedException(null, "CANNOT FREE PINNED PAGE.");
		}
	}

	/** Must pin the returned frame. */
	public int pick_victim() throws BufferPoolExceededException,
			PagePinnedException
	{
		int numbuffs = mgr.getNumBuffers();
		BufMgrFrameDesc [] myTable = mgr.getFrameTable();
		
		for(int i = 0 ; i < numbuffs ; i++){
			if(state_bit[i] == Available){
				frameTable[i] = new BufMgrFrameDesc(new PageId(-10),i);
				return i;
				}

			else{
				if(myTable[i].getPinCount() >0){
					//DO NOTHING. NOT ELLIGIBLE FOR REPLACEMENT.
				}

				else if (referenceBits[i] == true)
					referenceBits[i] = false;

				else if(myTable[i].getPinCount()==0&&referenceBits[i] == false){
					return i;
				}
			}
		}
		throw new BufferPoolExceededException(null , "BUFFER POOL OVERFLOW");
	} 
	/** Retruns the name of the replacer algorithm. */
	public String name()
	{ return "Clock"; }

	/**
	 * Counts the unpinned frames (free frames) in the buffer pool.
	 * 
	 * @returns the total number of unpinned frames in the buffer pool.
	 */
	public int getNumUnpinnedBuffers()
	{ return mgr.getNumUnpinnedBuffers(); }
}
