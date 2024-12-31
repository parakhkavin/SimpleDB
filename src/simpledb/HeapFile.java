package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

	private File f;
	private TupleDesc td;
	private int pageNum;
	private HeapPageId [] hpID;

	/**
	 * Constructs a heap file backed by the specified file.
	 *
	 * @param f the file that stores the on-disk backing store for this heap file.
	 */
	public HeapFile(File f, TupleDesc td) {
		this.f = f;
		this.td = td;
		this.pageNum = (int)Math.ceil(f.length()/BufferPool.PAGE_SIZE);
		this.hpID = new HeapPageId[pageNum];
		for(int i = 0 ; i < pageNum ; i ++) {
			HeapPageId tempPid = new HeapPageId(getId(), i);
			hpID[i] = tempPid;
		}
	}

	/**
	 * Returns the File backing this HeapFile on disk.
	 *
	 * @return the File backing this HeapFile on disk.
	 */
	public File getFile() {
		return f;
	}

	/**
	 * Returns page number of the this HeapFile.
	 */
	public int getPageNum() {
		return pageNum;
	}

	/**
	 * Returns an ID uniquely identifying this HeapFile. Implementation note: you
	 * will need to generate this tableid somewhere ensure that each HeapFile has a
	 * "unique id," and that you always return the same value for a particular
	 * HeapFile. We suggest hashing the absolute file name of the file underlying
	 * the heapfile, i.e. f.getAbsoluteFile().hashCode().
	 *
	 * @return an ID uniquely identifying this HeapFile.
	 */
	public int getId() {
		return f.getAbsoluteFile().hashCode();
		//throw new UnsupportedOperationException("implement this");
	}

	/**
	 * Returns the TupleDesc of the table stored in this DbFile.
	 * 
	 * @return TupleDesc of this DbFile.
	 */
	public TupleDesc getTupleDesc() {
		return td;
		//throw new UnsupportedOperationException("implement this");
	}

	// see DbFile.java for javadocs
	public Page readPage(PageId pid) {
		if(pid.pageno() >= pageNum || pid.pageno() < 0)
			throw new IllegalArgumentException("the page does not exist in this file");
		byte [] data = new byte[BufferPool.PAGE_SIZE];
		try{
			RandomAccessFile as = new RandomAccessFile(this.f, "r");
			as.skipBytes(pid.pageno()*BufferPool.PAGE_SIZE);// jump to the no-th page.
			as.read(data, 0, BufferPool.PAGE_SIZE);
			as.close();
			HeapPage hp = new HeapPage((HeapPageId)pid, data);
			return hp;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// see DbFile.java for javadocs
	public void writePage(Page page) throws IOException {
		if(page.isDirty() != null){
			RandomAccessFile os = new RandomAccessFile(getFile(), "rw");
			int offset = page.getId().pageno()*BufferPool.PAGE_SIZE;
			os.skipBytes(offset);
			byte [] data = page.getPageData();
			os.write(data, 0, BufferPool.PAGE_SIZE);
			os.close();
		}
	}

	/**
	 * Returns the number of pages in this HeapFile.
	 */
	public int numPages() {
		// some code goes here
		return pageNum;
	}

	// see DbFile.java for javadocs
	public ArrayList<Page> addTuple(TransactionId tid, Tuple t)
			throws DbException, IOException, TransactionAbortedException {
		ArrayList<Page> pageList = new ArrayList<Page>();
		int i;
		HeapPage hp;

		for(i = 0; i < pageNum; i++){
			hp = (HeapPage)Database.getBufferPool().getPage(tid, hpID[i], Permissions.READ_WRITE);
			if(hp.getNumEmptySlots() > 0){
				hp.addTuple(t);
				hp.markDirty(true, tid);
				writePage(hp);
				pageList.add(hp);
				break;
			}
		}
		if(i == pageNum){
			this.pageNum ++;
			this.hpID = new HeapPageId[pageNum];
			for(int k = 0 ; k < pageNum; k++) {
				HeapPageId tempPid = new HeapPageId(getId(), k);
				hpID[k] = tempPid;
			}

			byte [] pageData = new byte[BufferPool.PAGE_SIZE];
			HeapPage newHp = new HeapPage(new HeapPageId(getId(), i), pageData);
			newHp.addTuple(t);
			newHp.markDirty(true, tid);
			writePage(newHp);
			System.out.println("Create a new page on the HeapFile.");
			pageList.add(newHp);
		}
		return pageList;
	}

	// see DbFile.java for javadocs
	public Page deleteTuple(TransactionId tid, Tuple t) throws DbException, TransactionAbortedException {
		// some code goes here
		HeapPageId hpId = (HeapPageId)t.getRecordId().getPageId();
		HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, hpId, Permissions.READ_WRITE);
		hp.deleteTuple(t);
		hp.markDirty(true, tid);
		return hp;
	}

	// see DbFile.java for javadocs
	public DbFileIterator iterator(TransactionId tid) {
		List<Tuple> tpList = new ArrayList<Tuple>();
		for(int i = 0; i < pageNum; i ++) {
			try{
				HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, hpID[i], Permissions.READ_ONLY);
				Iterator<Tuple> pageIter = hp.iterator();
				while(pageIter.hasNext()) // pageIter.hasNext cann't write as: hp.iterator().hasNext()
					tpList.add(pageIter.next());
			} catch (TransactionAbortedException e) {
				e.printStackTrace();
			} catch (DbException e) {
				e.printStackTrace();
			}
		}
		TupleIterator dbfItr = new TupleIterator(td, tpList);
		return (DbFileIterator) dbfItr;
	}

}
