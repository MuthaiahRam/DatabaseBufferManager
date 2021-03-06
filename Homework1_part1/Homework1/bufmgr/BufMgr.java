/*  File BufMgr,java */

package bufmgr;


import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import global.AbstractBufMgr;
import global.AbstractBufMgrFrameDesc;
import global.Minibase;
import global.PageId;
//import global.SystemDefs;
import diskmgr.Page;

import exceptions.BufMgrException;
import exceptions.BufferPoolExceededException;
import exceptions.DiskMgrException;
import exceptions.FileIOException;
import exceptions.HashEntryNotFoundException;
import exceptions.HashOperationException;
import exceptions.InvalidBufferException;
import exceptions.InvalidFrameNumberException;
import exceptions.InvalidPageNumberException;
import exceptions.InvalidReplacerException;
import exceptions.InvalidRunSizeException;
import exceptions.PageNotFoundException;
import exceptions.PageNotReadException;
import exceptions.PagePinnedException;
import exceptions.PageUnpinnedException;
import exceptions.ReplacerException;

// *****************************************************

/**
 * This is a dummy buffer manager class. You will need to replace it
 * with a buffer manager that reads from and writes to disk
 *
 * algorithm to replace the page.
 */
public class BufMgr extends AbstractBufMgr
{
	// Replacement policies to be implemented
	public static final String Clock = "Clock";
	public static final String LRU = "LRU";
	public static final String MRU = "MRU";
	
	// Total number of buffer frames in the buffer pool. */
	private int numBuffers;

	// This buffer manager keeps all pages in memory! 
	private Hashtable<PageId, byte []> pageIdToPageData = new Hashtable<PageId, byte []>();

	// An array of Descriptors one per frame. 
	private BufMgrFrameDesc[] frameTable = new BufMgrFrameDesc[NUMBUF];


	/**
	 * Create a buffer manager object.
	 * 
	 * @param numbufs
	 *            number of buffers in the buffer pool.
	 * @param replacerArg
	 *            name of the buffer replacement policy (e.g. BufMgr.Clock).
	 * @throws InvalidReplacerException 
	 */
	public BufMgr(int numbufs, String replacerArg) throws InvalidReplacerException
	{
		numBuffers = numbufs;
        //frameTable = new BufMgrFrameDesc[numBuffers];
		setReplacer(replacerArg);
	}

	/**
     * Default Constructor
	 * Create a buffer manager object.
	 * @throws InvalidReplacerException 
	 */
	public BufMgr() throws InvalidReplacerException
	{
		numBuffers = NUMBUF;
		//frameTable = new BufMgrFrameDesc[numBuffers];
		replacer = new Clock(this);
	}
	
	private void removeFromPool(int pageId){
		
		Enumeration<PageId> enumeration=pageIdToPageData.keys();
		while(enumeration.hasMoreElements()){
			if(enumeration.nextElement().pid == pageId){
				pageIdToPageData.remove(pageId);
				return;
			}
		}
		
	}
	
	private int searchFrameDescriptor(PageId pageId){
		int length= frameTable.length;
		System.out.println("frame length"+length);
		for(int i =0 ; i < length; i++){
			System.out.println("::"+frameTable[i].getPageNo()+"::"+pageId.pid);
			if(frameTable[i]!=null && frameTable[i].getPageNo()==pageId.pid){
				return i;
			}
		}
		return -1;
	}

	private byte[] searchBufferPool(PageId pageId){
		Enumeration<PageId> enumeration=pageIdToPageData.keys();
		while(enumeration.hasMoreElements()){
			PageId id=enumeration.nextElement();
			if(id.pid == pageId.pid){
				return pageIdToPageData.get(id);
			}
		}
		return null;
		
	}
	/**
	 * Check if this page is in buffer pool, otherwise find a frame for this
	 * page, read in and pin it. Also write out the old page if it's dirty
	 * before reading. If emptyPage==TRUE, then actually no read is done to bring
	 * the page in.
	 * 
	 * @param pin_pgid
	 *            page number in the minibase.
	 * @param page
	 *            the pointer poit to the page.
	 * @param emptyPage
	 *            true (empty page); false (non-empty page)
	 * 
	 * @exception ReplacerException
	 *                if there is a replacer error.
	 * @exception HashOperationException
	 *                if there is a hashtable error.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception InvalidFrameNumberException
	 *                if there is an invalid frame number .
	 * @exception PageNotReadException
	 *                if a page cannot be read.
	 * @exception BufferPoolExceededException
	 *                if the buffer pool is full.
	 * @exception PagePinnedException
	 *                if a page is left pinned .
	 * @exception BufMgrException
	 *                other error occured in bufmgr layer
	 * @exception IOException
	 *                if there is other kinds of I/O error.
	 */

	public void pinPage(PageId pageId, Page page, boolean emptyPage)
			throws ReplacerException, HashOperationException,
			PageUnpinnedException, InvalidFrameNumberException,
			PageNotReadException, BufferPoolExceededException,
			PagePinnedException, BufMgrException, IOException{
		System.out.println("pageId" + pageId.pid);
		
		// This buffer manager just keeps allocating new pages and puts them 
		// the hash table. It regards each page it is passed as a parameter
		// (<code>page</code> variable) as empty, and thus doesn't take into
		// account any data stored in it.
		//
		// Extend this method to operate as it is supposed to (see the javadoc
		// description above).
		byte [] pageData = searchBufferPool(pageId);
		if(pageData == null){  //means page is not in pool
			pageData = new byte [1024];
			// Find a victim page to replace it with the current one.
			int replaceIndex = replacer.pick_victim();			
			if(replaceIndex < 0){
				page = null;
				throw new ReplacerException(null, "BUFMGR: REPLACER_ERROR.");
			}
			BufMgrFrameDesc replaceFrame = frameTable[replaceIndex]; //obtain victim frame's desc info
			boolean wasNull = false;
			if(replaceFrame.getPageNo() == -10)
				wasNull = true;
			
			// The following code excerpt reads the contents of the page with id pin_pgid
			// into the object page. Use it to read the contents of a dirty page to be
			// written back to disk.
			
			else if(replaceFrame.isDirty()){
				byte [] replaceData = searchBufferPool(new PageId(replaceFrame.getPageNo())); //dirty data of victim
				try {
					Minibase.JavabaseDB.write_page(new PageId(replaceFrame.getPageNo()), new Page(replaceData));
					//SystemDefs.JavabaseDB.write_page(replaceFrame.getPageNo(), new Page(replaceData));

				} catch (InvalidPageNumberException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileIOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(!wasNull){
				this.removeFromPool(replaceFrame.getPageNo());
				}
			
			replaceFrame.setPage(pageId); //new page to victim frame
			if(!emptyPage){
				page.setpage(pageData); 
				try {
					Minibase.JavabaseDB.read_page(pageId, page);
					//SystemDefs.JavabaseDB.read_page(pageId, page);
				} 
				catch ( Exception e){
					e.printStackTrace();
				}
			}

			else{
				page.setpage(pageData);
			}

			// Hint: Notice that this naive Buffer Manager allocates a page, but does not
			// associate it with a page frame descriptor (an entry of the frameTable
			// object). Your Buffer Manager shouldn't be that naive ;) . Have in mind that
			// the hashtable is simply the "storage" area of your pages, while the frameTable
			// contains the frame descriptors, each associated with one loaded page, which
			// stores that page's metadata (pin count, status, etc.)
			
			replaceFrame.setDirty(false);
			replaceFrame.pin();
			replacer.pin(replaceIndex);
			replaceFrame.setPage(pageId);

			pageIdToPageData.put(new PageId(pageId.pid), pageData); //toexplore
		}

		else{
			///This means that the requested page is already in Buffer pool. Just increase pin count.
			int frameIndex = searchFrameDescriptor(pageId);
			page.setpage(pageIdToPageData.get(pageId));
			frameTable[frameIndex].pin();
			replacer.pin(frameIndex);	

		}
		System.out.println("pin exit");
	}

	/**
	 * To unpin a page specified by a pageId. If pincount>0, decrement it and if
	 * it becomes zero, put it in a group of replacement candidates. if
	 * pincount=0 before this call, return error.
	 * 
	 * @param globalPageId_in_a_DB
	 *            page number in the minibase.
	 * @param dirty
	 *            the dirty bit of the frame
	 * 
	 * @exception ReplacerException
	 *                if there is a replacer error.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception InvalidFrameNumberException
	 *                if there is an invalid frame number .
	 * @exception HashEntryNotFoundException
	 *                if there is no entry of page in the hash table.
	 */
	public void unpinPage(PageId PageId, boolean dirty)
			throws ReplacerException, PageUnpinnedException,
			HashEntryNotFoundException, InvalidFrameNumberException
	{
		if(PageId.pid <0 ){
			return;
		}

		int indexOfFrame = searchFrameDescriptor(PageId);
		if(indexOfFrame==-1){
			throw new HashEntryNotFoundException(null,"ERROR 404: PAGE_NOT_FOUND_IN_POOL.");
		}
		BufMgrFrameDesc frame = frameTable[indexOfFrame];
		if(dirty == true){ //need to explore
			frame.setDirty(true);}


		if(frame.getPinCount() > 0){
			frame.unpin();
			replacer.unpin(indexOfFrame);
		}
		else
			throw new PageUnpinnedException(null, "CANNOT REDUCE PIN_COUNT FOR A FRAME BELOW 0.");
			}
	

	/**
	 * Call DB object to allocate a run of new pages and find a frame in the
	 * buffer pool for the first page and pin it. If buffer is full, ask DB to
	 * deallocate all these pages and return error (null if error).
	 * 
	 * @param firstpage
	 *            the address of the first page.
	 * @param howmany
	 *            total number of allocated new pages.
	 * @return the first page id of the new pages.
	 * 
	 * @exception BufferPoolExceededException
	 *                if the buffer pool is full.
	 * @exception HashOperationException
	 *                if there is a hashtable error.
	 * @exception ReplacerException
	 *                if there is a replacer error.
	 * @exception HashEntryNotFoundException
	 *                if there is no entry of page in the hash table.
	 * @exception InvalidFrameNumberException
	 *                if there is an invalid frame number.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception PagePinnedException
	 *                if a page is left pinned.
	 * @exception PageNotReadException
	 *                if a page cannot be read.
	 * @exception IOException
	 *                if there is other kinds of I/O error.
	 * @exception BufMgrException
	 *                other error occured in bufmgr layer
	 * @exception DiskMgrException
	 *                other error occured in diskmgr layer
	 */
	public PageId newPage(Page firstpage, int howmany)
			throws BufferPoolExceededException, HashOperationException,
			ReplacerException, HashEntryNotFoundException,
			InvalidFrameNumberException, PagePinnedException,
			PageUnpinnedException, PageNotReadException, BufMgrException,
			DiskMgrException, IOException
	{

		PageId newpage = new PageId();
		try {
			
			Minibase.JavabaseDB.allocate_page(newpage, howmany);
			
			//SystemDefs.JavabaseDB.allocate_page(newpage, howmany);
			pinPage(newpage, firstpage, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				Minibase.JavabaseDB.deallocate_page(newpage, howmany);
				//SystemDefs.JavabaseDB.deallocate_page(newpage, howmany);
				newpage = null;
				firstpage = null;
			} catch (InvalidRunSizeException | InvalidPageNumberException
					| FileIOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return newpage;
	}

	/**
	 * User should call this method if s/he needs to delete a page. this routine
	 * will call DB to deallocate the page.
	 * 
	 * @param globalPageId
	 *            the page number in the data base.
	 * @exception InvalidBufferException
	 *                if buffer pool corrupted.
	 * @exception ReplacerException
	 *                if there is a replacer error.
	 * @exception HashOperationException
	 *                if there is a hash table error.
	 * @exception InvalidFrameNumberException
	 *                if there is an invalid frame number.
	 * @exception PageNotReadException
	 *                if a page cannot be read.
	 * @exception BufferPoolExceededException
	 *                if the buffer pool is already full.
	 * @exception PagePinnedException
	 *                if a page is left pinned.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception HashEntryNotFoundException
	 *                if there is no entry of page in the hash table.
	 * @exception IOException
	 *                if there is other kinds of I/O error.
	 * @exception BufMgrException
	 *                other error occured in bufmgr layer
	 * @exception DiskMgrException
	 *                other error occured in diskmgr layer
	 */
	public void freePage(PageId pageId) throws InvalidBufferException,
			ReplacerException, HashOperationException,
			InvalidFrameNumberException, PageNotReadException,
			BufferPoolExceededException, PagePinnedException,
			PageUnpinnedException, HashEntryNotFoundException, BufMgrException,
			DiskMgrException, IOException{
		
		Enumeration<PageId> enumeration = pageIdToPageData.keys();
		while(enumeration.hasMoreElements()){
			
			if(enumeration.nextElement().pid == pageId.pid){
				pageIdToPageData.remove(pageId);
				//return;
			}
		}

		int index = searchFrameDescriptor(pageId);
		if ( index < 0){
			throw new InvalidFrameNumberException(null,"PAGE NOT FOUND.");
		}
		frameTable[index] = null;
		replacer.free(index);
		
			try {
				Minibase.JavabaseDB.deallocate_page(pageId);
				//SystemDefs.JavabaseDB.deallocate_page(pageId);
			} catch (InvalidRunSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidPageNumberException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 
	}

	/**
	 * Added to flush a particular page of the buffer pool to disk
	 * 
	 * @param pageid
	 *            the page number in the database.
	 * 
	 * @exception HashOperationException
	 *                if there is a hashtable error.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception PagePinnedException
	 *                if a page is left pinned.
	 * @exception PageNotFoundException
	 *                if a page is not found.
	 * @exception BufMgrException
	 *                other error occured in bufmgr layer
	 * @exception IOException
	 *                if there is other kinds of I/O error.
	 */
	public void flushPage(PageId pageid) throws HashOperationException,
			PageUnpinnedException, PagePinnedException, PageNotFoundException,
			BufMgrException, IOException{
		
		int frameIndex=searchFrameDescriptor(pageid);
		if(frameTable[frameIndex].isDirty()){
			Enumeration<PageId> enumeration=pageIdToPageData.keys();
			while(enumeration.hasMoreElements()){
				if(enumeration.nextElement().pid == pageid.pid){
					try {
						Minibase.JavabaseDB.write_page(pageid, new Page(pageIdToPageData.get(pageid)));
						//SystemDefs.JavabaseDB.write_page(pageid, new Page(pageIdToPageData.get(pageid)));
					} catch (InvalidPageNumberException | FileIOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					frameTable[frameIndex].setDirty(false);
					return;
				}
			}
		}
	}

	/**
	 * Flushes all pages of the buffer pool to disk
	 * 
	 * @exception HashOperationException
	 *                if there is a hashtable error.
	 * @exception PageUnpinnedException
	 *                if there is a page that is already unpinned.
	 * @exception PagePinnedException
	 *                if a page is left pinned.
	 * @exception PageNotFoundException
	 *                if a page is not found.
	 * @exception BufMgrException
	 *                other error occured in bufmgr layer
	 * @exception IOException
	 *                if there is other kinds of I/O error.
	 */
	public void flushAllPages() throws HashOperationException,
			PageUnpinnedException, PagePinnedException, PageNotFoundException,
			BufMgrException, IOException{
		
		for(int i=0;i<frameTable.length;i++){
			if(frameTable[i] != null){
				flushPage(new PageId(frameTable[i].getPageNo()));
			}
		}
	}

	/**
	 * Gets the total number of buffers.
	 * 
	 * @return total number of buffer frames.
	 */
	public int getNumBuffers()
	{
		return numBuffers;
	}

	/**
	 * Gets the total number of unpinned buffer frames.
	 * 
	 * @return total number of unpinned buffer frames.
	 */
	public int getNumUnpinnedBuffers()
	{
		int count=0;
		for(int i=0;i<frameTable.length;i++){
			if(frameTable[i]==null || frameTable[i].getPinCount()==0){
				count++;
			}
		}
		return count;
	}

	/** A few routines currently need direct access to the FrameTable. */
	public BufMgrFrameDesc[] getFrameTable()
	{
		return this.frameTable;
	}

}

