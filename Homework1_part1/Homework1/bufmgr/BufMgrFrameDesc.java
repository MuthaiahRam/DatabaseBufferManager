package bufmgr;

import global.AbstractBufMgrFrameDesc;
import global.GlobalConst;
import global.PageId;

public class BufMgrFrameDesc extends global.AbstractBufMgrFrameDesc implements GlobalConst
{
	private int pinCount;
	private boolean dirtyBit;
	private PageId pageId;
	private int frameIndex;
	
	public BufMgrFrameDesc(PageId page,int frameIndex){
		this.pageId=page;
		this.frameIndex=frameIndex;
		pinCount=0;
		dirtyBit=false;
	}
	
	public void setFrameIndex(int index){
		frameIndex=index;
	}
	
	public int getFrameIndex(){
		return frameIndex;
	}
	/**
	 * Returns the pin count of a certain frame page.
	 * 
	 * @return the pin count number.
	 */
	public int getPinCount()
	{ return pinCount; };
	
	public void setPinCount(int count){
		pinCount=count;
	}

	/**
	 * Increments the pin count of a certain frame page when the page is pinned.
	 * 
	 * @return the incremented pin count.
	 */
	public int pin()
	{ pinCount++; 
	return pinCount;};

	/**
	 * Decrements the pin count of a frame when the page is unpinned. If the pin
	 * count is equal to or less than zero, the pin count will be zero.
	 * 
	 * @return the decremented pin count.
	 */
	public int unpin()
	{ pinCount--; 
	return pinCount;}

	/**
	 * 
	 */
	public int getPageNo(){
		return pageId.pid; 
	}
	
	public void setPage(PageId pageId)
	{
		this.pageId=pageId;
	}

	/**
	 * the dirty bit, 1 (TRUE) stands for this frame is altered, 0 (FALSE) for
	 * clean frames.
	 */
	public boolean isDirty()
	{ return dirtyBit; };
	
	public boolean setDirty(boolean bit){
		dirtyBit=bit;
		return dirtyBit;
	}
	
	
}
