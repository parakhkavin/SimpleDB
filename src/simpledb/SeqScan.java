package simpledb;

import java.util.*;

/**
 * SeqScan is an implementation of a sequential scan access method that reads
 * each tuple of a table in no particular order (e.g., as they are laid out on
 * disk).
 */
public class SeqScan implements DbIterator {

	private TransactionId tid;
	private int tableId;
	private Iterator<Tuple> i;
	private TupleDesc td;
	private HeapFile hf;
	private int numScannedPage = 0;

	/**
	 * Creates a sequential scan over the specified table as a part of the specified
	 * transaction.
	 *
	 * @param tid        The transaction this scan is running as a part of.
	 * @param tableid    the table to scan.
	 * @param tableAlias the alias of this table (needed by the parser); the
	 *                   returned tupleDesc should have fields with name
	 *                   tableAlias.fieldName (note: this class is not responsible
	 *                   for handling a case where tableAlias or fieldName are null.
	 *                   It shouldn't crash if they are, but the resulting name can
	 *                   be null.fieldName, tableAlias.null, or null.null).
	 */
	public SeqScan(TransactionId tid, int tableid, String tableAlias) {
		this.tid = tid;
		this.tableId = tableid;
		this.td = Database.getCatalog().getTupleDesc(tableid);
		this.hf = (HeapFile)Database.getCatalog().getDbFile(tableid);
	}

	public void open() throws DbException, TransactionAbortedException {
		this.numScannedPage = 0;
		HeapPageId hpId = new HeapPageId(tableId, numScannedPage);
		HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, hpId, Permissions.READ_ONLY);
		i = hp.iterator();
		numScannedPage ++;
	}

	/**
	 * Returns the TupleDesc with field names from the underlying HeapFile, prefixed
	 * with the tableAlias string from the constructor.
	 *
	 * @return the TupleDesc with field names from the underlying HeapFile, prefixed
	 *         with the tableAlias string from the constructor.
	 */
	public TupleDesc getTupleDesc() {
		return td;
	}

	public boolean hasNext() throws TransactionAbortedException, DbException {
		if(i == null)
			return false;
		else
			return i.hasNext();
	}

	public Tuple next() throws NoSuchElementException, TransactionAbortedException, DbException {
		Tuple next = i.next();
		if(!i.hasNext() && numScannedPage < hf.getPageNum()){
			HeapPageId hpId = new HeapPageId(tableId, numScannedPage);
			HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, hpId, Permissions.READ_ONLY);
			numScannedPage ++;
			i = hp.iterator();
		}
		return next;
	}

	public void close() {
		i = null;
	}

	public void rewind() throws DbException, NoSuchElementException, TransactionAbortedException {
		close();
		open();
	}
}
