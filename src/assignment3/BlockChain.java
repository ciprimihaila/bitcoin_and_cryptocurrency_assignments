package assignment3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
	public static final int CUT_OFF_AGE = 10;

	private TransactionPool myTransactionPool;
	private HashMap<ByteArrayWrapper, BlockNode> myBlockChain;
	private BlockNode myMaxHeightNode;

	/**
	 * create an empty block chain with just a genesis block. Assume
	 * {@code genesisBlock} is a valid block
	 */
	public BlockChain(Block genesisBlock) {
		myBlockChain = new HashMap<>();
		myTransactionPool = new TransactionPool();
		UTXOPool utxoPool = new UTXOPool();
		addCoinbaseToTransactionPool(genesisBlock, utxoPool);
		BlockNode genesisNode = new BlockNode(genesisBlock, utxoPool, null);
		myMaxHeightNode = genesisNode;
		myBlockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
	}

	/** Get the maximum height block */
	public Block getMaxHeightBlock() {
		return myMaxHeightNode.getBlock();
	}

	/** Get the UTXOPool for mining a new block on top of max height block */
	public UTXOPool getMaxHeightUTXOPool() {
		return myMaxHeightNode.getTXPool();
	}

	/** Get the transaction pool to mine a new block */
	public TransactionPool getTransactionPool() {
		return myTransactionPool;
	}

	/**
	 * Add {@code block} to the block chain if it is valid. For validity, all
	 * transactions should be valid and block should be at
	 * {@code height > (maxHeight - CUT_OFF_AGE)}.
	 * 
	 * <p>
	 * For example, you can try creating a new block over the genesis block
	 * (block height 2) if the block chain height is {@code <=
	 * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot
	 * create a new block at height 2.
	 * 
	 * @return true if block is successfully added
	 */
	public boolean addBlock(Block block) {
		byte[] prevBlockHash = block.getPrevBlockHash();
		if (prevBlockHash == null) {
			return false;
		}

		BlockNode parentNode = myBlockChain.get(new ByteArrayWrapper(prevBlockHash));
		if (parentNode == null) {
			return false;
		}

		TxHandler txHandler = new TxHandler(parentNode.getTXPool());
		Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
		if (txHandler.handleTxs(txs).length != txs.length) {
			return false;
		}

		if (parentNode.getHeight() + 1 <= (myMaxHeightNode.getHeight() - CUT_OFF_AGE)) {
			return false;
		}

		UTXOPool utxoPool = txHandler.getUTXOPool();
		addCoinbaseToTransactionPool(block, utxoPool);
		BlockNode blockNode = new BlockNode(block, utxoPool, parentNode);
		myBlockChain.put(new ByteArrayWrapper(block.getHash()), blockNode);

		if (parentNode.getHeight() + 1 > myMaxHeightNode.getHeight()) {
			myMaxHeightNode = blockNode;
		}

		return true;
	}

	/** Add a transaction to the transaction pool */
	public void addTransaction(Transaction tx) {
		myTransactionPool.addTransaction(tx);
	}

	private void addCoinbaseToTransactionPool(Block block, UTXOPool utxoPool) {
		Transaction coinbase = block.getCoinbase();
		for (int i = 0; i < coinbase.numOutputs(); i++) {
			Transaction.Output out = coinbase.getOutput(i);
			UTXO utxo = new UTXO(coinbase.getHash(), i);
			utxoPool.addUTXO(utxo, out);
		}
	}

	class BlockNode {
		private Block myBlock;
		private UTXOPool myTXPool;
		private List<BlockNode> myNextNodes;
		private BlockNode myPreviuosNode;
		private int myHeight;

		public BlockNode(Block myBlock, UTXOPool myTXPool, BlockNode previuosNode) {
			this.myBlock = myBlock;
			this.myTXPool = myTXPool;
			this.myPreviuosNode = previuosNode;
			if (previuosNode == null) {
				myHeight = 1;
			} else {
				myHeight = previuosNode.getHeight() + 1;
				myPreviuosNode.addNextNode(this);
			}
			this.myNextNodes = new ArrayList<>();
		}

		public Block getBlock() {
			return myBlock;
		}

		public UTXOPool getTXPool() {
			return new UTXOPool(myTXPool);
		}

		public int getHeight() {
			return myHeight;
		}

		public void addNextNode(BlockNode node) {
			this.myNextNodes.add(node);
		}
	}
}