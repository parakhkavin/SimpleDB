package simpledb;

import java.util.*;
import java.io.*;

/**
 * BufferPool manages the reading and writing of pages into memory from disk.
 * Access methods call into it to retrieve pages, and it fetches pages from the
 * appropriate location.
 * <p>
 * The BufferPool is also responsible for locking; when a transaction fetches a
 * page, BufferPool which check that the transaction has the appropriate locks
 * to read/write the page.
 */
public class BufferPool {
	/** Bytes per page, including header. */
	public static final int PAGE_SIZE = 4096;

	/**
	 * Default number of pages passed to the constructor. This is used by other
	 * classes. BufferPool should use the numPages argument to the constructor
	 * instead.
	 */
	public static final int DEFAULT_PAGES = 50;

	private int numPages;
	private Map<PageId, Page> pages;
	private Map<PageId, TransactionId> pageLocks;

	/**
	 * Creates a BufferPool that caches up to numPages pages.
	 *
	 * @param numPages maximum number of pages in this buffer pool.
	 */
	public BufferPool(int numPages) {
		this.numPages = numPages;
		this.pages = new HashMap<>();
		this.pageLocks = new HashMap<>();
	}

	/**
	 * Retrieve the specified page with the associated permissions. Will acquire a
	 * lock and may block if that lock is held by another transaction.
	 * <p>
	 * The retrieved page should be looked up in the buffer pool. If it is present,
	 * it should be returned. If it is not present, it should be added to the buffer
	 * pool and returned. If there is insufficient space in the buffer pool, an page
	 * should be evicted and the new page should be added in its place.
	 *
	 * @param tid  the ID of the transaction requesting the page
	 * @param pid  the ID of the requested page
	 * @param perm the requested permissions on the page
	 */
	public Page getPage(TransactionId tid, PageId pid, Permissions perm)
			throws TransactionAbortedException, DbException {
		if (pages.containsKey(pid)) {
			return pages.get(pid);
		}

		if (pages.size() >= numPages) {
			evictPage();
		}

		DbFile file = Database.getCatalog().getDbFile(pid.getTableId());
		Page page = file.readPage(pid);
		pages.put(pid, page);
		return page;
	}

	/**
	 * Releases the lock on a page. Calling this is very risky, and may result in
	 * wrong behavior. Think hard about who needs to call this and why, and why they
	 * can run the risk of calling it.
	 *
	 * @param tid the ID of the transaction requesting the unlock
	 * @param pid the ID of the page to unlock
	 */
	public void releasePage(TransactionId tid, PageId pid) {
		pageLocks.remove(pid);
	}

	/**
	 * Release all locks associated with a given transaction.
	 *
	 * @param tid the ID of the transaction requesting the unlock
	 */
	public void transactionComplete(TransactionId tid) throws IOException {
		transactionComplete(tid, true);
	}

	/** Return true if the specified transaction has a lock on the specified page */
	public boolean holdsLock(TransactionId tid, PageId p) {
		return tid.equals(pageLocks.get(p));
	}

	/**
	 * Commit or abort a given transaction; release all locks associated to the
	 * transaction.
	 *
	 * @param tid    the ID of the transaction requesting the unlock
	 * @param commit a flag indicating whether we should commit or abort
	 */
	public void transactionComplete(TransactionId tid, boolean commit) throws IOException {
		if (commit) {
			flushPages(tid);
		} else {
			// Abort: revert changes
			for (PageId pid : pages.keySet()) {
				Page page = pages.get(pid);
				if (tid.equals(page.isDirty())) {
					Page oldPage = page.getBeforeImage();
					pages.put(pid, oldPage);
				}
			}
		}

		// Release locks held by the transaction
		pageLocks.entrySet().removeIf(entry -> tid.equals(entry.getValue()));
	}

	/**
	 * Add a tuple to the specified table behalf of transaction tid. Will acquire a
	 * write lock on the page the tuple is added to(Lock acquisition is not needed
	 * for lab2). May block if the lock cannot be acquired.
	 *
	 * Marks any pages that were dirtied by the operation as dirty by calling their
	 * markDirty bit, and updates cached versions of any pages that have been
	 * dirtied so that future requests see up-to-date pages.
	 *
	 * @param tid     the transaction adding the tuple
	 * @param tableId the table to add the tuple to
	 * @param t       the tuple to add
	 */
	public void insertTuple(TransactionId tid, int tableId, Tuple t)
			throws DbException, IOException, TransactionAbortedException {
		HeapFile table = (HeapFile) Database.getCatalog().getDbFile(tableId);
		ArrayList<Page> affectedPages = table.addTuple(tid, t);
		for (Page page : affectedPages) {
			page.markDirty(true, tid);
			pages.put(page.getId(), page);
		}
	}

	/**
	 * Remove the specified tuple from the buffer pool. Will acquire a write lock on
	 * the page the tuple is removed from. May block if the lock cannot be acquired.
	 *
	 * Marks any pages that were dirtied by the operation as dirty by calling their
	 * markDirty bit. Does not need to update cached versions of any pages that have
	 * been dirtied, as it is not possible that a new page was created during the
	 * deletion (note difference from addTuple).
	 *
	 * @param tid the transaction adding the tuple.
	 * @param t   the tuple to add
	 */
	public void deleteTuple(TransactionId tid, Tuple t) throws DbException, TransactionAbortedException {
		HeapFile table = (HeapFile) Database.getCatalog().getDbFile(t.getRecordId().getPageId().getTableId());
		Page affectedPage = table.deleteTuple(tid, t);
		affectedPage.markDirty(true, tid);
		pages.put(affectedPage.getId(), affectedPage);
	}

	/**
	 * Flush all dirty pages to disk. NB: Be careful using this routine -- it writes
	 * dirty data to disk so will break simpledb if running in NO STEAL mode.
	 */
	public synchronized void flushAllPages() throws IOException {
		for (PageId pid : pages.keySet()) {
			flushPage(pid);
		}
	}

	/**
	 * Remove the specific page id from the buffer pool. Needed by the recovery
	 * manager to ensure that the buffer pool doesn't keep a rolled back page in its
	 * cache.
	 */
	public synchronized void discardPage(PageId pid) {
		pages.remove(pid);
	}

	/**
	 * Flushes a certain page to disk
	 *
	 * @param pid an ID indicating the page to flush
	 */
	private synchronized void flushPage(PageId pid) throws IOException {
		Page page = pages.get(pid);
		if (page == null) {
			throw new NoSuchElementException("Page not found in BufferPool");
		}

		if (page.isDirty() != null) {
			DbFile file = Database.getCatalog().getDbFile(pid.getTableId());
			file.writePage(page);
			page.markDirty(false, null);
		}
	}

	/**
	 * Write all pages of the specified transaction to disk.
	 */
	public synchronized void flushPages(TransactionId tid) throws IOException {
//		for (Page page : pages.values()) {
//			if (tid.equals(page.isDirty())) {
//				flushPage(page.getId());
//			}
//		}
	}

	/**
	 * Discards a page from the buffer pool. Flushes the page to disk to ensure
	 * dirty pages are updated on disk.
	 */
	private synchronized void evictPage() throws DbException {
		for (Iterator<Map.Entry<PageId, Page>> it = pages.entrySet().iterator(); it.hasNext();) {
			Map.Entry<PageId, Page> entry = it.next();
			Page page = entry.getValue();
			if (page.isDirty() == null) {
				it.remove();
				return;
			}
		}
		throw new DbException("All pages in the BufferPool are dirty, cannot evict");
	}

}
