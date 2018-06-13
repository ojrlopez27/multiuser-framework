package edu.cmu.inmind.multiuser.controller.composer.bn;

import com.rits.cloning.Cloner;
import edu.cmu.inmind.multiuser.controller.commons.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * A behavior i can be described by a tuple (ci ai di zi). Where:
 * ci is a list of preconditions which have to be full-filled before the
 * behavior can become active. ai and di represent the expected
 * effects of the behavior's action in terms of an add-list and a
 * delete-list. In addition, each behavior has a level of activation zi
 *
 * @author oromero
 *
 */
public class Behavior implements Comparable<Behavior>{
	public static final String TOKEN = "-";
	private String name;
	private String id;
	private List<List<Premise>> preconditions = new Vector <>();
	private List<String> addList = new Vector <>();
	private String description;
	private List<String> addGoals = new Vector<>();
	private List<String> deleteList = new Vector <>();

	private transient double activation = 0;
	private transient int idx;
	private transient boolean executable = false, activated = false;
	private transient boolean verbose = false;
	private transient int numMatches;
	private transient List<Premise> stateMatches;
	private transient double utility;
	private transient BehaviorNetwork network;
	private transient String userName;
	private transient String shortName;

	public Behavior(String name){
		this.name = name;
	}

	public Behavior(String name, Premise[][] preconds, String[] addlist, String[] deletelist){
		this.name = name;
		addPreconditions(preconds);
		addList.addAll(Arrays.asList(addlist));
		deleteList.addAll(Arrays.asList(deletelist));
	}

	public Behavior(String name, String description, Premise[][] preconds, String[] addlist, String[] deletelist){
		this(name, preconds, addlist, deletelist);
		this.description = description;
	}

	public Behavior(String name, String description, Premise[][] preconds, String[] addlist, String[] deletelist,
					String[] addGoals){
		this(name, description, preconds, addlist, deletelist);
		this.description = description;
		this.addGoals.addAll(Arrays.asList(addGoals));
	}

	public BehaviorNetwork getNetwork() {
		return network;
	}

	public void setNetwork(BehaviorNetwork network) {
		this.network = network;
	}

	public void addPreconditions(Premise[][] preconds){
		for(int i = 0; preconds != null && i < preconds.length; i++) {
			List<Premise> precondList = new Vector<>();
			for(int j = 0; j < preconds[i].length; j++){
				precondList.add( preconds[i][j] );
			}
			preconditions.add( precondList );
		}
	}

	public String getId() {return id;}
	public void setId(String id) {this.id = id;}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getAddGoals() {
		return addGoals;
	}
	public List <String> getAddList() {
		return addList;
	}
	public void setAddList(List <String> addList) {
		this.addList = addList;
	}
	public void addAddList(List <String> addList) {
		if( this.addList == null ){
			addList = new Vector<>();
		}
		this.addList.addAll(addList);
	}
	public void setName(String name) {
		this.name = name;
	}

	public List <String> getDeleteList() {
		return deleteList;
	}
	public void setDeleteList(List <String> deleteList) {
		this.deleteList = deleteList;
	}
	public double getActivation() {
		return activation;
	}
	public void setActivation(double activation) {
		this.activation = activation;
	}
	public Collection<List<Premise>> getPreconditions() {
		if(preconditions == null){
			preconditions = new Vector<>();
		}
		return preconditions;
	}
	public int getIdx(){
		return this.idx;
	}
	public boolean getExecutable(){
		return executable;
	}
	public String getName(){
		return name;
	}
	public boolean getActivated(){
		return activated;
	}
	public void setActivated(boolean a){
		activated = a;
	}
	public void setUserName(String userName) {this.userName = userName;}
	public String getUserName() {return userName;}

	/**
	 * Determines if is into the add-list
	 * @param proposition
	 * @return
	 *
	 * Note: modified with weights.
	 */
	public boolean isSuccesor(String proposition){
		return isKindOfLink( addList, proposition );
	}

	/**
	 * Determines if is into the delete-list
	 * @param proposition
	 * @return
	 *
	 * Note: modified with weights.
	 */
	public boolean isInhibition(String proposition){
		return isKindOfLink( deleteList, proposition );
	}



	private boolean isKindOfLink(List<String> list, String proposition){
		if( list.contains(proposition) ) {
			return true;
		}else{
			for( String premise : list ){
				if( premise.contains("*") && Pattern.compile(premise.replace("*", "[a-zA-Z0-9_]*"))
						.matcher(proposition).matches()) {
					return true;
				}
			}
		}
		return false;
	}

	public void setAddList(String proposition){
		addList.add(proposition);
	}

	/**
	 * Determines whether proposition is into the preconditions set
	 * @param proposition
	 * Note: modified with weights.
	 * @return
	 */
	public boolean hasPrecondition(String proposition){
		for(List<Premise> precondList : preconditions ){
			for( Premise precond : precondList ){
				if( precond.getLabel().contains("*")){
					if ( Pattern.compile( precond.getLabel().replace("*", "[a-zA-Z0-9_]*") )
							.matcher( proposition ).matches() ){
						return true;
					}
				}else if( precond.getLabel().equals(proposition) ){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * the input of activation to behavior x from the state at time t is
	 * @param states
	 * @param matchedStates
	 * @param phi
	 * Note: modified with weights.
	 * @return
	 */
	public double calculateInputFromState(List<String> states, int[] matchedStates, double phi){
		double activation = 0;
		for(List<Premise> condList : preconditions ){
			for(Premise cond : condList ) {
				int index = findPremise( states, cond.getLabel() );
				if (index != -1) {
					double temp = phi * (1.0d / (double) matchedStates[index]) * (1.0d / (double) preconditions.size()) * cond.getWeight();
					activation += temp;
					if(verbose) {
						System.out.println("state gives " + this.name + " an extra activation of " + temp + " for " + cond);
					}
				}
			}
		}
		return activation;
	}

	private int findPremise(List<String> states, String condition){
		int idx = states.indexOf(condition);
		if(idx > -1) return idx;
		for( int i = 0; i < states.size(); i++ ){
			if( condition.contains("*") && Pattern.compile(condition.replace("*", "[a-zA-Z0-9_]*"))
					.matcher(states.get(i)).matches() ){
				return i;
			}else if(condition.equals( states.get(i))){
				return i;
			}
		}
		return -1;
	}

	/**
	 * The input of activation to behavior x from the goals at time t is
	 * @param goals
	 * @param achievedPropositions
	 * @param gamma
	 * Note: modified with weights.
	 * @return
	 */
	public double calculateInputFromGoals(List<String> goals, int[] achievedPropositions, double gamma){
		double activation = 0;
		for(int i = 0; i < addList.size(); i++){
			int index = findPremise(goals, addList.get(i));
			if(index != -1){
				double temp = gamma * (1.0d / (double) achievedPropositions[index]) * (1.0d / (double) addList.size());
				activation += temp;
				if(verbose) {
					System.out.println("goals give " + this.name + " an extra activation of " + temp);
				}
			}
		}
		return activation;
	}

	/**
	 * The removal of activation from behavior x by the goals that are protected
	 * at time t is.
	 * @param goalsR
	 * @param undoPropositions
	 * @param delta
	 * Note: modified with weights.
	 * @return
	 */
	public double calculateTakeAwayByProtectedGoals(List<String> goalsR, int[] undoPropositions, double delta){
		double activation = 0;
		for(int i = 0; i < deleteList.size(); i++){ //ojrl addlist
			int index = findPremise(goalsR, deleteList.get(i)); //ojrl addList
			if(index != -1){
				double temp = delta * (1.0d / (double) undoPropositions[index]) * (1.0d / (double) deleteList.size());
				activation += temp;
				if(verbose) {
					System.out.println("goalsR give " + this.name + " an extra activation of " + temp);
				}
			}
		}
		return activation;
	}

	/**
	 * A function executable(i t), which returns 1 (true) if behavior i is executable
	 * at time t (i.e., if all of the preconditions of behavior i are members
	 * of S (t)), and 0 (false) otherwise.
	 * Note: modified with weights.
	 */
	public boolean isExecutable (ConcurrentSkipListSet<String> states){
		Collection<List<Premise>> preconds = new Vector<> (this.getPreconditions());
		executable = true;
		for(List<Premise> precondRow : preconds ){
			executable = false;
			for(Premise precond : precondRow ){
				if(states.contains(precond.getLabel())){
					executable = true;
				}
			}
			if( !executable ) break;
		}
		return executable;
	}

	public boolean isExecutable (int maximum){
		return executable = numMatches >= maximum;
	}

	public boolean isExecutable (double maximum){
		return executable = utility >= (maximum * .8);
	}

	/**
	 * Note: modified with weights.
	 * @return
	 */
	public double computeUtility() {
		return utility = this.getActivation() + (this.getNumMatches() * 5);
	}

	public void resetActivation(boolean reset){
		if(reset){
//			activation = 0;
			activation = activation/2;
		}
		executable = false;
		activated = false;
	}

	/**
	 * Note: modified with weights.
	 * @param act
	 */
	public void updateActivation(double act){
		activation += act;
		if(activation < 0)
			activation = 1;
	}

	public void decay(double factor){
		activation *= factor;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	@Override
	public int compareTo(Behavior other) {
		return Double.compare( this.computeUtility(), other.computeUtility() );
	}

	@Override
	public Behavior clone(){
		return Utils.clone(this);
	}

	public Behavior deepClone(){
		//return Utils.deepClone(this);
		return new Cloner().deepClone(this);
	}

	public void reset() {
		activation = 0;
		executable = false;
		activated = false;
		numMatches = 0;
	}

	/**
	 * Note: modified with weights.
	 * @param states
	 * @return
	 */
	public int calculateMatchPreconditions(ConcurrentSkipListSet<String> states) {
		numMatches = 0;
		stateMatches = new ArrayList<>();
		for( List<Premise> precondList : preconditions ){
			for( Premise precond : precondList ){
				if( states.contains(precond.getLabel()) ){
					stateMatches.add(precond);
					numMatches++;
				}
			}
		}
		return numMatches;
	}

	public int getNumMatches() {
		return numMatches;
	}

	public void setNumMatches(int numMatches) {
		this.numMatches = numMatches;
	}

	public List<Premise> getStateMatches() {
		return stateMatches;
	}

	public List<Premise> getMissingStates() {
		List<Premise> missing = new ArrayList<>();
		for(List<Premise> premises : preconditions){
			for(Premise premise : premises){
				if(!stateMatches.contains(premise)){
					missing.add(premise);
				}
			}
		}
		return missing;
	}

	/**
	 * Creates a grounded (specific) behavior from an abstract one. Basically,
	 * we add a (grounded) prefix to behavior name, pre and post conditions.
	 * It is used for user's devices
	 * @param devicePrefix
	 * @param userPrefix
	 * @return
	 */
	public Behavior groundByPrefix(String devicePrefix, String userPrefix) {
		Behavior clone = deepClone();
		clone.name = devicePrefix + TOKEN + clone.name;
		clone.extractShortName();
		devicePrefix += TOKEN;
		userPrefix += TOKEN;
		for(List<Premise> premises : clone.preconditions){
			for(Premise premise : premises){
				premise.setLabel( (premise.isDependsOnDevice()? devicePrefix : userPrefix) + premise.getLabel() );
			}
		}
		for(int i = 0; i < clone.addList.size(); i++){
			clone.addList.set(i, userPrefix + clone.addList.get(i) );
		}
		for(int i = 0; i < clone.deleteList.size(); i++){
			clone.deleteList.set(i, userPrefix + clone.deleteList.get(i) );
		}
		return clone;
	}


	/***
	 * Unlike {@ground} method, which only adds a prefix to each premise (pre and post conditions),
	 * this method also replaces some keywords by actual values. It is used for server devices
	 * @param mappings
	 * @return
	 */
	public Behavior groundByReplacing(String prefix, Map<String, String> mappings) {
		Behavior clone = deepClone();
		clone.name = prefix + TOKEN + clone.name;
		clone.extractShortName();
		for(List<Premise> premises : clone.preconditions){
			for(Premise premise : premises){
				premise.setLabel( replaceMapping(premise.getLabel(), mappings) );
			}
		}
		for(int i = 0; i < clone.addList.size(); i++){
			clone.addList.set(i, replaceMapping(clone.addList.get(i), mappings) );
		}
		for(int i = 0; i < clone.deleteList.size(); i++){
			clone.deleteList.set(i, replaceMapping(clone.deleteList.get(i), mappings) );
		}
		return clone;
	}

	private String replaceMapping(String premise, Map<String, String> mappings){
		for(String keyword : mappings.keySet() ){
			if( premise.contains(keyword) ){
				return premise.replace(keyword, mappings.get(keyword) );
			}
		}
		return premise;
	}

	public String getShortName() {
		return shortName;
	}

	private void extractShortName(){
		String[] words = name.split(TOKEN);
		shortName = "";
		for(String word : words){
			shortName += word.toUpperCase().charAt(0);
		}
	}

	@Override
	public String toString(){
		return name + " (" + shortName + ")";
	}
}