package assignment3;
import java.util.ArrayList;
import java.util.List;

public class TxHandler {

	private UTXOPool myUtxoPool;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}. This should make a copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		myUtxoPool = new UTXOPool(utxoPool);
	}

	private boolean checkOutputs(Transaction tx) {
		for (Transaction.Output output : tx.getOutputs()) {
			boolean outputClaimed = false;
			if (output.value > 0) {
				for (UTXO utxo : myUtxoPool.getAllUTXO()) {
					Transaction.Output utxoOutput = myUtxoPool.getTxOutput(utxo);
					if (utxoOutput.address.equals(output.address) && utxoOutput.value == output.value) {
						outputClaimed = true;
						break;
					}
				}
			}
			if (!outputClaimed) {
				return false;
			}
		}
		return true;
	}

	private boolean checkSum(Transaction tx) {
		double inputSum = 0;
		for (Transaction.Input input : tx.getInputs()) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			Transaction.Output output = myUtxoPool.getTxOutput(utxo);
			if (output != null) {
				inputSum += output.value;
			}
		}
		double outpuSum = 0;
		for (Transaction.Output output : tx.getOutputs()) {
			outpuSum += output.value;
		}

		return inputSum > outpuSum;

	}

	private boolean checkSignature(Transaction tx) {
		UTXOPool uniqueUtxos = new UTXOPool();
		for (int inputIndex = 0; inputIndex < tx.getInputs().size(); inputIndex++) {
			Transaction.Input input = tx.getInput(inputIndex);
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			Transaction.Output txOutput = myUtxoPool.getTxOutput(utxo);
			if (txOutput != null) {
				if (!Crypto.verifySignature(txOutput.address, tx.getRawDataToSign(inputIndex), input.signature)) {
					return false;
				}
			}
			if (uniqueUtxos.contains(utxo)) {
				return false;
			}
			uniqueUtxos.addUTXO(utxo, myUtxoPool.getTxOutput(utxo));
		}
		return true;
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are
	 *         valid, (3) no UTXO is claimed multiple times by {@code tx}, (4)
	 *         all of {@code tx}s output values are non-negative, and (5) the
	 *         sum of {@code tx}s input values is greater than or equal to the
	 *         sum of its output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		return checkSignature(tx) && checkSum(tx);// checkOutputs(tx) &&
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness, returning a
	 * mutually valid array of accepted transactions, and updating the current
	 * UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		List<Transaction> validTxs = new ArrayList<Transaction>();
		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				validTxs.add(tx);
				updateLocalPool(tx);
			}
		}
		return validTxs.toArray(new Transaction[validTxs.size()]);
	}

	private void updateLocalPool(Transaction tx) {
		for (Transaction.Input in : tx.getInputs()) {
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			myUtxoPool.removeUTXO(utxo);
		}
		for (int i = 0; i < tx.numOutputs(); i++) {
			Transaction.Output out = tx.getOutput(i);
			UTXO utxo = new UTXO(tx.getHash(), i);
			myUtxoPool.addUTXO(utxo, out);
		}
	}

	public UTXOPool getUTXOPool() {
		return myUtxoPool;
	}

}
