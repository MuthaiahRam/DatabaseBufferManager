package tests;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.PageId;
//import global.SystemDefs;
import global.TestDriver;

import java.io.IOException;

import bufmgr.BufMgr;

import diskmgr.Page;

public class BMDriver extends TestDriver implements GlobalConst
{
	
	// private int TRUE = 1;
	// private int FALSE = 0;
	private boolean OK = true;

	private boolean FAIL = false;

	/**
	 * BMDriver Constructor, inherited from TestDriver
	 */
	public BMDriver()
	{
		super("Buffer Manager");
	}

	public void initBeforeTests()
	{
		try {
	
			
			Minibase.initBufMgr(new BufMgr());
			
	
			
		} catch(Exception ire)
		{
			ire.printStackTrace(); 
			System.exit(1);
		}
		
		Minibase.initDiskMgr("BMDriver", NUMBUF+20);
	}
	
	/**
	 * Add your own test here.
	 * 
	 * @return whether test1 has passed
	 */
	/*public boolean test1()
	{
		
		System.out.print("\n  Test 1 is not implemented. \n ");
		
		return true;
	}*/

	/**
	 * Add your own test here.
	 * 
	 * @return whether test2 has passed
	 */
	/*public boolean test2()
	{
		
		System.out.print("\n  Test 2 is not implemented. \n ");
		
		return true;
	}*/
	
	//#########################################################################################################################
	public boolean test1 () {

		System.out.print("\n  Test 1 does a simple test of normal buffer ");
		System.out.print("manager operations:\n");

		// We choose this number to ensure that at least one page will have to be
		// written during this test.
		boolean status = OK;
		int numPages = Minibase.JavabaseBM.getNumUnpinnedBuffers() + 1;
		Page pg = new Page(); 
		PageId pid; 
		PageId lastPid;
		PageId firstPid = new PageId(); 

		System.out.print("  - Allocate a bunch of new pages  \n");

		try {
			firstPid = Minibase.JavabaseBM.newPage( pg, numPages );
		}
		catch (Exception e) {   
			System.err.print("*** Could not allocate " + numPages);
			System.err.print (" new pages in the database.\n");
			e.printStackTrace();
			return false;
		}


		// Unpin that first page... to simplify our loop.
		try {
			Minibase.JavabaseBM.unpinPage(firstPid, false /*not dirty*/);
		}
		catch (Exception e) {
			System.err.print("*** Could not unpin the first new page.\n");
			e.printStackTrace();
			status = FAIL;
		}

		System.out.print("  - Write something on each one\n");

		pid = new PageId();
		lastPid = new PageId();

		for ( pid.pid = firstPid.pid, lastPid.pid = pid.pid+numPages; 
		status == OK && pid.pid < lastPid.pid; 
		pid.pid = pid.pid + 1 ) {

			try {
				Minibase.JavabaseBM.pinPage( pid, pg, /*emptyPage:*/ false);
			}
			catch (Exception e) { 
				status = FAIL;
				System.err.print("*** Could not pin new page "+pid.pid+"\n");
				e.printStackTrace();
			}      

			if ( status == OK ) {

				
				int data = pid.pid + 99999;

				try {
					Convert.setIntValue (data, 0, pg.getpage());
				}
				catch (IOException e) {
					System.err.print ("*** Convert value failed\n");
					status = FAIL;
				}

				if (status == OK) {
					try {
						Minibase.JavabaseBM.unpinPage( pid, /*dirty:*/ true );
					}
					catch (Exception e)  { 
						status = FAIL;
						System.err.print("*** Could not unpin dirty page "
								+ pid.pid + "\n");
						e.printStackTrace();
					}
				}
			}
		}

		if ( status == OK )
			System.out.print ("  - Read that something back from each one\n" + 
					"   (because we're buffering, this is where "  +
			"most of the writes happen)\n");

		for (pid.pid=firstPid.pid; status==OK && pid.pid<lastPid.pid; 
		pid.pid = pid.pid + 1) {

			try {
				Minibase.JavabaseBM.pinPage( pid, pg, /*emptyPage:*/ false );
			}
			catch (Exception e) { 
				status = FAIL;
				System.err.print("*** Could not pin page " + pid.pid + "\n");
				e.printStackTrace();
			}

			if ( status == OK ) {

				int data = 0;

				try {
					data = Convert.getIntValue (0, pg.getpage());
				}
				catch (IOException e) {
					System.err.print ("*** Convert value failed \n");
					status = FAIL;
				}

				
			}
		}

		if (status == OK)
			System.out.print ("  - Free the pages again\n");

		for ( pid.pid=firstPid.pid; pid.pid < lastPid.pid; 
		pid.pid = pid.pid + 1) {

			try {
				Minibase.JavabaseBM.freePage( pid ); 
			}
			catch (Exception e) {
				status = FAIL;
				System.err.print("*** Error freeing page " + pid.pid + "\n");
				e.printStackTrace();
			}

		}

		if ( status == OK )
			System.out.print("  Test 1 completed successfully.\n");

		return status;
	}

	public static void main(String argv[])
	{

		BMDriver bmt = new BMDriver();
		
		boolean dbstatus;

		//dbstatus = bmt.runTests();

		if (dbstatus != true)
		{
			System.out.println("Error encountered during buffer manager tests:\n");
			System.out.flush();
			Runtime.getRuntime().exit(1);
		}

		System.out.println("Done. Exiting...");
		Runtime.getRuntime().exit(0);
	}
}
