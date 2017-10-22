package assignment2;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

	private double pGraph;
	private double pMalicious;
	private double pTxDistribution;
	private int numRounds;

	private boolean[] myFollowees;

	private Set<Transaction> myPendingTransactions;
	Set<Candidate> myCandidates;

	public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
		this.pGraph = p_graph;
		this.pMalicious = p_malicious;
		this.pTxDistribution = p_txDistribution;
		this.numRounds = numRounds;
	}

	public void setFollowees(boolean[] followees) {
		this.myFollowees = followees;
	}

	public void setPendingTransaction(Set<Transaction> pendingTransactions) {
		this.myPendingTransactions = pendingTransactions;
	}

	public Set<Transaction> sendToFollowers() {
		return myPendingTransactions;
	}

	public void receiveFromFollowees(Set<Candidate> candidates) {
		for (Candidate c : candidates) {
			this.myPendingTransactions.add(c.tx);
		}
	}
}
