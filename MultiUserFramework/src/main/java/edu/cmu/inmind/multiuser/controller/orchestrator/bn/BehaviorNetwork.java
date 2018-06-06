package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

/**
 * @author oromero
 */
public class BehaviorNetwork {
	private List<Behavior> behaviors = new Vector<>();
    private ConcurrentSkipListSet<String> states = new ConcurrentSkipListSet<>();
	private List<String> goals = new Vector <>();
    private boolean removePrecond = false;
	private transient List<String> goalsResolved = new Vector <>();

	private double pi = 15; //20 the mean level of activation,
	private double theta = 15; //45 the threshold of activation, where is lowered 10% every time no behavior could be selected, and is reset to its initial value whenever a behavior becomes active.
	private double initialTheta = 15;//45
	private double phi = 70; //20 // 90 the amount of activation energy injected by the state per true proposition,
	private double gamma = 20; //70 the amount of activation energy injected by the goals per goal,
	private double delta = 50; // 90 not defined the amount of activation energy taken away by the protected goals per protected goal.

	private transient double[][] activationSuccesors;
	private transient double[][] activationPredeccesors;
	private transient double[][] activationConflicters;
	private transient double[] activationInputs;
	private transient double[] activationLinks;
	private transient boolean execution = false;
	private transient int indexBehActivated = -1;
	private transient int cont = 1;
	private transient Double[] activations;
    private transient boolean verboseTree = false;
    private transient boolean verboseVal = false;
    private transient List<String> previousStates;
    private transient List<Behavior> behaviorsCopy = new Vector<>();
    private transient String nameBehaviorActivated;
	private String output;
	private String statesOutput;

    private transient int cycle = 1;
	private transient HashMap<String, Behavior> map;
    private transient int padBehaviorName;
    private final static String LEVEL_1 = "|-- ";
    private final static String LEVEL_2 = "   |-- ";
    private final static String LEVEL_3 = "      |-- ";

    BehaviorNetwork(){}

	public HashMap<String, Behavior> map(){
        int length = 0;
        map = new HashMap<>();
        for(Behavior behavior : behaviors){
            map.put(behavior.getName(), behavior);
            if(behavior.getName().length() > length) length = behavior.getName().length();
            behavior.setNetwork(this);
        }
        padBehaviorName = length + 5;
        return map;
    }

    public List<Behavior> getBehByPrefix(String prefix){
	    List<Behavior> behaviors = new ArrayList<>();
        for(Behavior behavior : this.behaviors){
            if(behavior.getName().startsWith(prefix)){
                behaviors.add(behavior);
            }
        }
        return behaviors;
    }


    public List<Behavior> getBehaviors() {
		return behaviors;
	}

	synchronized double[] getActivations() {
		int size = activations.length;
		double[] actvs = new double[ size ];
		for( int i = 0; i < size; i++){
			actvs[i] = activations[i];
		}
		return actvs;
	}

	int getIdxBehActivated() {
		return indexBehActivated;
	}

    public boolean isRemovePrecond() {
        return removePrecond;
    }

    public Behavior getBehaviorActivated(){
		if( indexBehActivated >= 0 ) {
			return behaviors.get(indexBehActivated);
		}
		return null;
	}

	public String getOutput() {
		return output;
	}

	public String getStatesOutput() {
		return statesOutput;
	}

	public String getNameBehaviorActivated() {
        return nameBehaviorActivated;
	}

	public void setBehaviors(List<Behavior> behaviors) {
		this.behaviors = behaviors;
		activationSuccesors = new double[behaviors.size()][behaviors.size()];
		activationPredeccesors = new double[behaviors.size()][behaviors.size()];
		activationConflicters = new double[behaviors.size()][behaviors.size()];
		activationInputs = new double[behaviors.size()];
		activationLinks = new double[behaviors.size()];
	}

	void setBehaviors(List<Behavior> behaviors, int size) {
		this.behaviors = behaviors;
		for( int i = 0; i < behaviors.size(); i++ ){
			behaviors.get(i).setIdx(i);
		}

		activationSuccesors = new double[behaviors.size()][behaviors.size()];
		activationPredeccesors = new double[behaviors.size()][behaviors.size()];
		activationConflicters = new double[behaviors.size()][behaviors.size()];
		activationInputs = new double[behaviors.size()];
		activationLinks = new double[behaviors.size()];
		if( size < behaviors.size() ) {
			activations = new Double[behaviors.size()];
		}else{
			activations = new Double[size];
		}
	}

	public double getPi() {
		return pi;
	}

	public void setPi(double pi) {
		this.pi = pi;
	}

	public double getTheta() {
		return theta;
	}

	public void setTheta(double theta) {
		this.theta = theta;
	}

	public double getInitialTheta() {
		return initialTheta;
	}

	public void setInitialTheta(double itheta) {
		this.initialTheta = itheta;
	}

	public double getPhi() {
		return phi;
	}

	public void setPhi(double phi) {
		this.phi = phi;
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	/**
	 * a function S (t) returning the propositions that are observed to be true at time t
	 * (the state of the environment as perceived by the agent) S being implemented
	 * by an independent process (or the real world),
	 */
	public ConcurrentSkipListSet<String> getState (){
		if(states != null)
			return states;
		return new ConcurrentSkipListSet<>();
	}

	public String getStateString(){
		String result = "";
		if(states != null) {
			for( String state : states ){
				result += state + ", ";
			}
			result = result.substring(0, result.length()-2);
		}
		return result;
	}

    public String getPreviousStateString(){
        String result = "";
        if(previousStates != null) {
            for( String state : previousStates ){
                result += state + ", ";
            }
            result = result.substring(0, result.length()-2);
        }
        return result;
    }

	public synchronized void setState(List<String> states){
        try {
            if( this.states == null ){
                this.states = new ConcurrentSkipListSet<>(states);
            }else{
                for(String state : states){
                    this.states.add(state);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
	}

//	public void addState(List<String> states){
//		if(states == null)
//			this.states = null;
//		else
//			for(int i = 0; i < states.size(); i++) {
//				if( !this.states.contains(states.get(i)) ) {
//					this.states.add(states.get(i));
//				}
//			}
//	}

	/**
	 * a function G(t) returning the propositions that are a goal of the agent at time
	 * t G being implemented by an independent process,
	 */
	public List<String> getGoals (){
		if(goals != null)
			return goals;
		return new Vector<>();
	}

	public void setGoals(List<String> goals){
		if(goals == null)
			this.goals = null;
		else
			this.goals.addAll(goals);
	}

	/**
	 * a function R(t) returning the propositions that are a goal of the agent that
	 * has already been achieved at time t R being implemented by an independent
	 * process (e.g. some internal or external goal creator),
	 */
	private Collection<String> getGoalsR (){
		if(goalsResolved != null)
			return goalsResolved;
		return new Vector<>();
	}

	private void setGoalsR(List<String> goalsR){
		if(goalsR == null)
			this.goalsResolved = null;
		else
			this.goalsResolved.addAll(goalsR);
	}

	/**
	 * A function executable(i t), which returns 1 if behavior i is executable
	 * at time t (i.e., if all of the preconditions of behavior i are members
	 * of S (t)), and 0 otherwise.
	 */
	public boolean executable (int i){
		try {
			return behaviors.get(i).isExecutable(states);
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;
	}

    public boolean executable (int idx, int maximum){
        try {
            return behaviors.get(idx).isExecutable(maximum);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

	private boolean executable (int idx, double maximum){
		try {
			return behaviors.get(idx).isExecutable(maximum);
		}catch (Exception e){
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * a function M (j), which returns the set of behaviors that match proposition j ,
	 * i.e., the behaviors x for which j E cx,
	 * Note: modified with weights.
	 */
	private List<Behavior> matchProposition(String proposition){
		Vector<Behavior> behaviors = new Vector<>();
		for(Behavior behavior : this.behaviors){
			if(behavior.hasPrecondition(proposition))
				behaviors.add(behavior);
		}
		return behaviors;
	}

	/**
	 * a function A(j ), which returns the set of behaviors that achieve proposition j ,
	 * i.e., the behaviors x for which j E ax,
	 * Note: modified with weights.
	 */
	private Vector<Behavior> achieveProposition(String proposition){
		Vector<Behavior> behaviors = new Vector<>();
		for(Behavior beh : this.behaviors){
			if( beh.isSuccesor(proposition) )
				behaviors.add( beh );
		}
		return behaviors;
	}

	/**
	 * a function U (j ), which returns the set of behaviors that undo proposition j , i.e.,
	 * the behaviors x for which j E dx,
	 */
	private Vector<Behavior> undoProposition(String proposition){
		Vector<Behavior> behaviors = new Vector<>();
		for(Behavior beh : this.behaviors){
			if( beh.isInhibition(proposition) )
				behaviors.add( beh );
		}
		return behaviors;
	}

	/**
	 * a function U (j ), which returns the set of behaviors that undo proposition j , i.e.,
	 * the behaviors x for which j E dx, and j E S(t)
	 *
	 * Note: modified with weights.
	 */
	private Vector<Behavior> undoPropositionState(String proposition, int indexBehx){
		Vector<Behavior> behaviors = new Vector<>();
		for(Behavior behx : this.behaviors){
			if(behx.getIdx() != indexBehx){
				if( behx.isInhibition(proposition) && states.contains(proposition) ) {
                    behaviors.add(behx);
                }
			}
		}
		return behaviors;
	}

	/**
	 * The impact of the state, goals and protected goals on the activation level of a behavior is computed.
	 Note: modified with weights.
	 */
	private void computeActivation (){
		Vector<String> statesList = new Vector<>(getState());
		Vector<String> goalsList = new Vector<>(getGoals());
		Vector<String> goalsRList = new Vector<>(getGoalsR());
		int[] matchesByState = new int[statesList.size()];
		int[] matchesByGoal = new int[goals.size()];
		int[] matchesByGoalsResolved = new int[states.size()];//new int[goals.size()];

		//#M(j)
		for(int i = 0; i < statesList.size(); i++){
			matchesByState[i] = matchProposition(statesList.get(i)).size();
		}
		//#A(j)
		for(int i = 0; i < goalsList.size(); i++){
			matchesByGoal[i] = achieveProposition(goalsList.get(i)).size();
		}
		//#U(j)
		//ojrl for(int i = 0; i < statesList.size(); i++){
		for(int i = 0; i < goalsRList.size(); i++){
			//ojrl matchesByGoalsResolved[i] = undoProposition(statesList.get(i)).size();
			matchesByGoalsResolved[i] = undoProposition(goalsRList.get(i)).size();
		}

		for(int i = 0; i < behaviors.size(); i++){
			double actState = behaviors.get(i).calculateInputFromState(statesList, matchesByState, phi);
			double actGoal = behaviors.get(i).calculateInputFromGoals(goalsList, matchesByGoal, gamma);
			double actGoalR = behaviors.get(i).calculateTakeAwayByProtectedGoals(goalsRList, matchesByGoalsResolved, delta); //ojrl states
			activationInputs[i] += actState + actGoal - actGoalR;
		}
	}

	/**
	 * The way the behavior activates and inhibits related behaviors through
	 * its successor links, predecessor links and conflicter links is computed.
	 *
	 * Note: modified with weights.
	 */
	private void computeLinks (){
        //int highest = highestNumPreconditions();
		double highest = highestUtility();
		for(int i = 0; i < behaviors.size(); i++){
			//boolean isExecutable = executable(i);
            boolean isExecutable = executable(i); //executable(i, highest);
			activationSuccesors[i] = spreadsForward(behaviors.get(i).getIdx(), isExecutable);
			activationPredeccesors[i] = spreadsBackward(behaviors.get(i).getIdx(), isExecutable);
			activationConflicters[i] = takesAway(behaviors.get(i).getIdx());
		}
	}

	/**
	 * Note: modified with weights.
	 * @return
	 */
	private double highestUtility() {
		double maximum = 0;
		for (Behavior behavior : behaviors) {
			behavior.calculateMatchPreconditions(states);
			double utility = behavior.computeUtility();
			if (utility > maximum) {
				maximum = utility;
			}
		}
		return maximum;
	}

	/**
     * This method returns the behavior with the highest number of preconditions present at the current state.
     * @return
     */
    private int highestNumPreconditions() {
        int maximum = 0;
		for (Behavior behavior : behaviors) {
			int numMatched = behavior.calculateMatchPreconditions(states);
			if (numMatched > maximum) {
				maximum = numMatched;
			}
		}
        return maximum;
    }

    /**
	 * An executable behavior x spreads activation forward. It increases
	 * (by a fraction of its own activation level) the activation level of those
	 * successors y for which the shared proposition p E ax n cy is not true.
	 * Intuitively, we want these successor behaviors to become more activated because
	 * they are 'almost executable', since more of their preconditions will be fulfilled
	 * after the behavior has become active.
	 * Formally, given that behavior x = (cx ax dx zx) is executable,
	 * it spreads forward through those successor links for which the proposition
	 * that defines them p E ax is false.
	 *
	 * Note: modified with weights.
	 */
	private double[] spreadsForward(int indexBehavior, boolean executable){
		double[] activation = new double[behaviors.size()];
		if(executable){
			Vector<String> addList = new Vector<> ( behaviors.get(indexBehavior).getAddList() );
			for(String addPremise : addList ){
				if( !states.contains(addPremise) ){ //p E ax is false
                    List<Behavior> sparseBehaviors = matchProposition(addPremise); //j E ax n cy
					for(Behavior beh : sparseBehaviors ){
						int cardinalityMj = sparseBehaviors.size();
						int cardinalityCy = beh.getPreconditions().size();
						double temp = beh.getActivation() * (phi/gamma) * (1d / cardinalityMj) * (1d / cardinalityCy);
                        activation[beh.getIdx()] += temp;
                        if( verboseVal ) {
                            System.out.println(behaviors.get(indexBehavior).getName() + " spreads " + temp + " forward to " +
                                    beh.getName() + " for " + addPremise);
                        }
					}
				}
			}
		}
		return activation;
	}

	/**
	 * A behavior x that is NOT executable spreads activation backward.
	 * It increases (by a fraction of its own activation level) the activation level of
	 * those predecessors y for which the shared proposition p E cx n ay is not true.
	 * Intuitively, a non-executable behavior spreads to the behaviors that
	 * `promise' to fulfill its preconditions that are not yet true, so that the competence
	 * behavior may become executable afterwards. Formally, given that competence
	 * behavior x = (cx ax dx zx) is not executable, it spreads backward through those
	 * predecessor links for which the proposition that defined them p E cx is false.
	 * @param indexBehavior
	 * @param executable
	 * @return
	 *
	 * Note: modified with weights.
	 */
	private double[] spreadsBackward(int indexBehavior, boolean executable){
		double[] activation = new double[behaviors.size()];
		if(!executable){
			Behavior beh = behaviors.get(indexBehavior);
			Collection<List<Premise>> condList = new Vector<> (beh.getPreconditions());
			for(List<Premise> condRow : condList ){
                for(Premise cond : condRow ){
                    if( !states.contains( cond.getLabel() ) ) { //p E cx is false
                        Vector<Behavior> sparseBehaviors = achieveProposition(cond.getLabel()); //j E cx n ay
                        for (int j = 0; j < sparseBehaviors.size(); j++) {
							int cardinalityAj = sparseBehaviors.size();
                            int cardinalityAy = sparseBehaviors.get(j).getAddList().size();
                            double temp = beh.getActivation() * (1d / cardinalityAj) * (1d / cardinalityAy);
                            activation[sparseBehaviors.get(j).getIdx()] += temp;
                            if( verboseVal ) {
                                System.out.println( String.format("%s spreads %s backward to %s for %s",
                                        beh.getName(), temp, sparseBehaviors.get(j).getName(), cond.getLabel()) );
                            }
                        }

                    }
                }
			}
		}
		return activation;
	}

	/**
	 * Inhibition of Conflicters
	 * Every behavior x (executable or not) decreases (by a fraction of its
	 * own activation level) the activation level of those conflicters y for which the
	 * shared proposition p E cx n dy is true. Intuitively, a behavior tries to prevent a
	 * behavior that undoes its true preconditions from becoming active. Notice that
	 * we do not allow a behavior to inhibit itself (while it may activate itself). In
	 * case of mutual conflict of behaviors, only the one with the highest activation
	 * level inhibits the other. This prevents the phenomenon that the most relevant
	 * behaviors eliminate each other. Formally, behavior x = (cx ax dx zx)
	 * takes away activation energy through all of its conflicter links for which the
	 * proposition that defines them p E cx is true, except those links for which there
	 * exists an inverse conflicter link that is stronger.
	 *
	 * Note: modified with weights.
	 */
	private double[] takesAway(int indexBehavior){
		double[] activation = new double[behaviors.size()];
		Behavior behx = behaviors.get(indexBehavior);
        Collection<List<Premise>> condListX = new Vector<>(behx.getPreconditions());

		for( List<Premise> condRow : condListX ){
            for( Premise cond : condRow ) {
				Vector<Behavior> sparseBehaviors = undoPropositionState(cond.getLabel(), indexBehavior); //j E cx n dy
                for ( Behavior behy : sparseBehaviors) {
                    double temp = 0;
                    if ((behx.getActivation() <= behy.getActivation()) && inverseTakesAway(indexBehavior, behy.getIdx())) {
                        activation[behy.getIdx()] = 0;
                    } else {
                        int cardinalityUj = sparseBehaviors.size();
                        int cardinalityDy = behy.getDeleteList().size();
                        temp = behx.getActivation() * (delta / gamma) * (1d / cardinalityUj) * (1d / cardinalityDy);
                        activation[behy.getIdx()] += temp;//Math.max(activationTemp, sparseBehaviors.get(j).getActivationPrior());
                    }
                    if( verboseVal ) {
                        System.out.println(behx.getName() + " decreases (inhibits)" + behy.getName() + " with " + temp +
                                " for " + cond);
                    }
                }
            }
		}
		return activation;
	}

	/**
	 * Finds the intersection set S(t) n cy n dx
	 * @param indexBehx
	 * @param indexBehy
	 * @return
	 *
	 * Note: modified with weights.
	 */
	private boolean inverseTakesAway(int indexBehx, int indexBehy){
        Collection<List<Premise>> condsListy = new Vector<> (behaviors.get(indexBehy).getPreconditions());
		Behavior behx = behaviors.get(indexBehx);

		for(List<Premise> condRow : condsListy ){
            for( Premise cond : condRow ) {
                if (behx.getDeleteList().contains(cond.getLabel()) ) //cy n dx
                    if (states.contains( cond.getLabel() )) //cy n S(t)
                        return true;
            }
		}
		return false;
	}

	/**
	 * A decay function ensures that the overall activation level remains constant.
	 */
	private void applyDecayFx (){
		double sum = 0, factor, mayor = 0;
		int indexMayor = 0;

		for(int i = 0; i < behaviors.size(); i++){
			sum += behaviors.get(i).getActivation();
			if(behaviors.get(i).getActivation() > mayor){
				mayor = behaviors.get(i).getActivation();
				indexMayor = i;
			}
		}
		if(sum > pi * behaviors.size()){
			factor = pi * behaviors.size() / sum;
			for (Behavior behavior : behaviors) {
				behavior.decay(factor);
			}
		}
//		//ojrl decay bahavior which increments its activation continously and is not activated
//		if(behaviors.get(indexMayor).getActivation() > (theta * 1.5) && !behaviors.get(indexMayor).getActivated()){
//			behaviors.get(indexMayor).decay(0.5);
//			for (Behavior behavior : behaviors) {
//				if (behavior.getExecutable() && behavior.getIdx() != behaviors.get(indexMayor).getIdx()
//						&& !behavior.getActivated() && indexBehActivated != -1) {
//					if (indexBehActivated != behavior.getIdx())
//						behavior.decay(1.5);
//					else
//						behavior.decay(0.9);
//				}
//			}
//		}

		for(int i = 0; i < behaviors.size(); i++){
            if( verboseVal ) {
//                System.out.println("activation-level " + behaviors.get(i).getName() + ": " + behaviors.get(i).getActivation());
            }
		}
	}

	/**
	 * The behavior that fulfills the following three conditions becomes
	 * active: (i) It has to be executable, (ii) Its level of activation has to surpass a
	 * certain threshold and (iii) It must have a higher activation level than all other
	 * competence behaviors that fulfill conditions (i) and (ii). When two competence
	 * behaviors fulfilll these conditions (i.e., they are equally strong), one of them is
	 * chosen randomly. The activation level of the behavior that has become active is
	 * reinitialized to 0 2. If none of the behaviors fulfills conditions (i) and (ii), the
	 * threshold is lowered by 10%.
	 *
	 * Note: modified with weights.
	 */
	private int activateBehavior (){
		double act = 0;
		indexBehActivated = -1;
		for( Behavior beh : behaviors){
            if( beh.getActivation() >= theta && beh.getExecutable() && beh.getActivation() > act){
                indexBehActivated = behaviors.indexOf(beh);
                act = beh.getActivation();
                nameBehaviorActivated = beh.getName(); //beh.getName();
            }
        }
        execution = false;
        if( verboseTree ) {
            System.out.println(LEVEL_2 + "STATE: " + Arrays.toString(states.toArray()));
            System.out.println(LEVEL_2 + "BEHAVIOR (B), ACTIVATION (AC), PRECONDITIONS THAT ARE TRUE (PT), LIST OF PRECONDITIONS (LP), So:  |-- B  AC  (PT) -> LP");
        }
		for( Behavior beh : behaviors){
			if( verboseTree ) {
				System.out.println(LEVEL_3 + CommonUtils.padRight( beh.getName(), padBehaviorName)
                        + CommonUtils.padRight(beh.getActivation(), 10) + "("+beh.getNumMatches()+") -> "
                        + beh.getStateMatches().toString());
			}
		}
		if( verboseTree ) {
			System.out.println(LEVEL_2 + "THETA (THRESHOLD): " + theta );
		}
        if( indexBehActivated >= 0 ){
			statesOutput = Arrays.toString(states.toArray());
			if(verboseTree ) {
				System.out.println(LEVEL_2 + "EXECUTING BEHAVIOR: " + behaviors.get(indexBehActivated).getName());
			}
            execution = true;
            behaviors.get(indexBehActivated).setActivated(true);
        }
		return indexBehActivated;
	}


    public synchronized void triggerPostconditions(Behavior beh, List<String> addConditions){
        triggerPostconditions(beh, addConditions, beh.getDeleteList());
    }

    public synchronized void triggerPostconditions(Behavior beh ) {
        triggerPostconditions(beh, beh.getAddList(), beh.getDeleteList());
    }


	public synchronized void triggerPostconditions(Behavior beh, List<String> addConditions, List<String> deleteConditions){
        try {
            previousStates = new Vector<>(states);
            for (String anAddList : addConditions) {
                states.add(anAddList);
            }
            if (removePrecond) {
                if( beh.getPreconditions() != null ) {
                    for (List<Premise> preconds : beh.getPreconditions()) {
                        for (Premise precond : preconds) {
                            states.remove(precond.getLabel());
                        }
                    }
                }
            } else {
                List<String> toRemove = new ArrayList<>();
                for (String premise : deleteConditions) {
                    if(states.contains(premise)) {
                        toRemove.add(premise);
                    }else {
                        for (String st : states) {
                            if (premise.contains("*") && Pattern.compile(premise.replace("*", "[a-zA-Z0-9]*"))
                                    .matcher(st).matches()) {
                                toRemove.add(st);
                            }
                        }
                    }
                }
                // bulk operations are not guaranteed to be performed atomically
                for(String strToRemove : toRemove){
                    states.remove(strToRemove);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		protectGoals(beh);
        for (String goal : beh.getAddGoals()) {
            goals.remove(goal);
            goals.add(goal);
        }
	}

	/**
	 * Protect the goals achieved
	 * @param beh behavior
	 * Note: modified with weights.
	 */
	private void protectGoals(Behavior beh){
		for (String goalTemp : beh.getAddList()) {
		    if(goalTemp.startsWith(beh.getUserName())) goalTemp = goalTemp.substring(beh.getUserName().length()+1);
			if (goals.contains(goalTemp)){
				goalsResolved.add(goalTemp);
				goals.remove(goalTemp);
			}
		}
	}

	/**
	 * updates the activation of each behavior
	 * Note: modified with weights.
	 */
	private void updateActivation(){
		//activation level of a behavior y at time t
		for(int i = 0; i < behaviors.size(); i++){
			for(int j = 0; j < behaviors.size(); j++){
				activationLinks[i] += activationSuccesors[j][i] + activationPredeccesors[j][i] - activationConflicters[j][i];
			}
			behaviors.get(i).updateActivation(activationInputs[i] + activationLinks[i]);
			activationInputs[i] = 0;
			activationLinks[i] = 0;
		}
	}

	/**
	 * Reset the activation of the behServMap
	 */
	private void reset(){
		//reseting
		if(execution)
			theta = initialTheta;
		for (Behavior behavior : behaviors) {
			behavior.resetActivation(behavior.getActivated());
		}
	}

	/**
	 * Note: modified with weights.
	 * @return
	 */
    public int selectBehavior() {
		if(verboseTree) System.out.println(LEVEL_1 + "BEHAVIOR NETWORK DECISION CYCLE: " + cycle);
        if (activations == null) {
            setBehaviors(behaviors, behaviors.size() + 1);
        }
        activations[behaviors.size()] = theta;
        if (verboseVal) {
            System.out.println(String.format("\nStep: %s.\tgoals achieved: %s,\tgoals: %s",cont, goalsResolved.size(), goals.size()));
        }
        computeActivation();
        computeLinks();
        updateActivation();
        activateBehavior();
        recordActivations();
        applyDecayFx();
        cont++;
        checkExecute();
        return indexBehActivated;
    }

	/**
	 * Note: modified with weights.
	 */
	private void recordActivations(){
        for (int i = 0; i < behaviors.size(); i++) {
            Behavior b = behaviors.get(i);
            if (verboseVal) {
                System.out.println("Behavior: " + b.getName() + "  activation: " + b.getActivation());
            }
            activations[i] = behaviors.get(i).getActivation();
        }
        if (indexBehActivated != -1) {
            activations[behaviors.size()] = behaviors.get(indexBehActivated).getActivation();
        }
    }

    public void checkExecute(){
        if( !execution ){
            theta *= 0.9;
            if( verboseVal ) {
                System.err.println("None of the executable behaviors has accumulated enough activation to become active");
                System.out.println("Theta: " + theta);
            }
        }else{
            cycle = 1;
        }
        reset();
    }

    public int getHighestActivation() {
        int idx = 5; //ASN
        double max = 0;
        for(int i = 0; i< behaviors.size(); i++){
            Behavior beh = behaviors.get(i);
            if( beh.getActivation() > theta && beh.getActivation() > max ){
                idx = i;
            }
        }
        behaviors.get(idx).setActivation(max * 1.15);
        nameBehaviorActivated = behaviors.get(idx).getId(); //.getName();
        return indexBehActivated = idx;
    }

	public int getHighestActivationUsingNone(){
		int idx = 7, maxPrecTrue = 0;
		double max = 0;
		for(int i = 0; i< behaviors.size(); i++){
			Behavior beh = behaviors.get(i);
			if( beh.getName().equals("NONE")){
				idx = i;
			}
			if( beh.getActivation() > theta && beh.getActivation() > max ){
				max = beh.getActivation();
			}
			if( beh.getNumMatches() > maxPrecTrue ){
				maxPrecTrue = beh.getNumMatches();
			}
		}
        if( max == 0){
            max = theta;
        }
        indexBehActivated = idx;
        nameBehaviorActivated = behaviors.get(idx).getId(); //getName();
        behaviors.get(idx).setActivation(max * 1.30);
		behaviors.get(idx).setNumMatches(maxPrecTrue);
        behaviors.get(idx).setActivated(true);
        execution = true;
        recordActivations();
		return idx;
	}

    public String[] getBehaviorNamesByHighestActivation() {
        getBehaviorsSorted();
        String[] results = new String[behaviorsCopy.size()];
		DecimalFormat formatter = new DecimalFormat("#0.00");
		output = "";
        for( int i = 0; i < behaviorsCopy.size(); i++ ){
			Behavior bp = behaviorsCopy.get(i);
            results[i] = bp.getId();
			output += "["+bp.getName()+": ("+formatter.format( bp.getActivation())+") - ("+bp.getNumMatches()+")], ";
        }
		output += "theta: " + theta;
        return results;
    }

    public List<Behavior> getBehaviorsSorted(){
        getBehaviorsCopy(false);
        Collections.sort( behaviorsCopy );
        Collections.reverse( behaviorsCopy );
        return behaviorsCopy;
    }

    private void getBehaviorsCopy(boolean deep) {
        behaviorsCopy.clear();
        for( Behavior beh : behaviors){
            behaviorsCopy.add( deep? beh.deepClone() : beh.clone() );
        }
    }

	public String[] getBehaviorNames() {
		String[] names = new String[ behaviors.size() ];
		for(int i = 0; i < names.length; i++){
			names[i] = behaviors.get(i).getName();
		}
		return names;
	}

	public double[] getOnlyActivations() {
		int size = behaviors.size();
		double[] actvs = new double[ size ];
		for( int i = 0; i < size; i++){
			actvs[i] = activations[i];
		}
		return actvs;
	}

	public void resetAll() {
		for( Behavior beh : behaviors){
			beh.reset();
		}
		theta = initialTheta;
	}


    /**
     * Using the goal, we go backward over behServMap, looking for those behServMap that
     * promise to trigger the goal-behavior. During this search, we check whether behServMap
     * have preconditions like "*-required", if so, then add them to the state.
     */
    public void endMeansAnalysis(List<String> users){
	    List<String> latentStates = new ArrayList<>();
	    for(String goal : goals){
	        for( Behavior behavior : behaviors){
	            for(String addCondition : behavior.getAddList() ){
	                if( removeUserPrefix(addCondition, users).equals(goal) ){
	                    // so this behavior promises to achieve the goal
                        latentStates = extractLatentStates(behavior, latentStates);
                    }
                }
            }
        }
        // we cannot use addAll since it is not guaranteed to be performed atomically
        for(String latentState : latentStates){
	        states.add(latentState);
        }
    }

    private String removeUserPrefix(String premise, List<String> users){
        int pos = premise.indexOf(Behavior.TOKEN);
        if( pos != -1 ){
            if( users.contains( premise.substring(0, pos)) ){
                premise = premise.substring( pos+1 );
            }
        }
        return premise;
    }

    private List<String> extractLatentStates(Behavior behavior, List<String> latentStates) {
        for( List<Premise> premises : behavior.getPreconditions() ){
            for(Premise premise : premises){
                if( !states.contains(premise.getLabel()) ) {
                    if (checkIsRequired(premise.getLabel()) && !latentStates.contains(premise.getLabel())) {
                        latentStates.add(premise.getLabel());
                    }
                    for (Behavior beh : behaviors) {
                        if (beh != behavior) {
                            for (String addCondition : beh.getAddList()) {
                                if (addCondition.equals(premise.getLabel())) {
                                    extractLatentStates( beh, latentStates);
                                }
                            }
                        }
                    }
                }
            }
        }
        return latentStates;
    }

    private boolean checkIsRequired(String premise){
        return premise.contains("-required");
        //return premise.lastIndexOf("R") == premise.length() - 1;
    }

    public void addBehaviors(List<Behavior> behaviors) {
        this.behaviors.addAll(behaviors);
    }
}
