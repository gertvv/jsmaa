/*
	This file is part of JSMAA.
	(c) Tommi Tervonen, 2009	

    JSMAA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JSMAA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JSMAA.  If not, see <http://www.gnu.org/licenses/>.
*/

package fi.smaa;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.smaa.common.RandomUtil;

@SuppressWarnings("unchecked")
public class SMAASimulator {
	
	private SimulationThread simulationThread;
	private SMAAResults results;
	private Integer iterations;
	
	private double[] weights;
	private double[][] measurements;
	private double[] utilities;
	private Integer[] ranks;
	private List<Alternative> alternatives;
	private List<Criterion> criteria;
	private UtilitySampler sampler;
	
	private MeasurementChangeListener listener = new MeasurementChangeListener();
	
	private class MeasurementChangeListener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getSource() instanceof Measurement ||
					evt.getPropertyName().equals(CardinalCriterion.PROPERTY_ASCENDING)) {
				restart();
			}
		}		
	}
	
	private static SMAASimulator simulator;
	
	public static SMAASimulator initSimulator(SMAAModel model, Integer iterations) {
		if (simulator != null) {
			simulator.stop();
			simulator.disconnectListeners();
		}
		simulator = new SMAASimulator(model, iterations);
		return simulator;
	}
	
	private void disconnectListeners() {
		for (Criterion<Measurement> c : criteria) {
			c.removePropertyChangeListener(listener);
			for (Measurement m : c.getMeasurements().values()) {
				m.removePropertyChangeListener(listener);
			}
		}		
	}

	private SMAASimulator(SMAAModel model, Integer iterations) {
		this.criteria = new ArrayList<Criterion>(model.getCriteria());
		this.alternatives = new ArrayList<Alternative>(model.getAlternatives());
		this.iterations = iterations;
		results = new SMAAResults(alternatives, criteria, 10);
		sampler = new UtilitySampler(this.alternatives.size());
		init();
		connectListeners();
	}
	
	private void connectListeners() {
		for (Criterion<Measurement> c : criteria) {
			c.addPropertyChangeListener(listener);
			for (Measurement m : c.getMeasurements().values()) {
				m.addPropertyChangeListener(listener);
			}
		}
	}

	public Integer getTotalIterations() {
		return iterations;
	}

	public SMAAResults getResults() {
		return results;
	}
	
	synchronized public void stop() {
		if (simulationThread != null) {
			simulationThread.stopSimulation();
			simulationThread = null;
		}		
	}
			
	synchronized public void restart() {
		stop();
		resetValues();
		if (alternatives.size() == 0 || criteria.size() == 0) {
			return;
		}
		simulationThread = createSimulationThread();
		simulationThread.start();
	}
	
	public boolean isRunning() {
		if (simulationThread == null) {
			return false;
		}
		return simulationThread.isRunning();
	}	

	private SimulationThread createSimulationThread() {
		return new SimulationThread(iterations) {
			public void iterate() {
				lockCriteria();
				generateWeights();
				sampleCriteria();
				aggregate();
				rankAlternatives();
				updateHits();
				releaseCriteria();
			}
		};
	}

	protected void releaseCriteria() {
		for (Criterion c : criteria) {
			c.getChangeSemaphore().release();
		}		
	}

	protected void lockCriteria() {
		for (Criterion c : criteria) {
			try {
				c.getChangeSemaphore().acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void resetValues() {
		results.reset();
	}
	
	private void rankAlternatives() {
		UtilIndexPair[] pairs = new UtilIndexPair[utilities.length];
		for (int i=0;i<utilities.length;i++) {
			pairs[i] = new UtilIndexPair(i, utilities[i]);
		}
		Arrays.sort(pairs);
		int rank = 0;
		Double oldUtility = pairs.length > 0 ? pairs[0].util : 0.0;
		for (int i=0;i<pairs.length;i++) {
			if (!oldUtility.equals(pairs[i].util)) {
				rank++;
				oldUtility = pairs[i].util;
			}			
			ranks[pairs[i].altIndex] = rank;
		}
	}

	private void updateHits() {
		results.update(ranks, weights);
	}

	private void aggregate() {
		clearUtilities();
		
		for (int critIndex=0;critIndex<criteria.size();critIndex++) {
			Criterion crit = criteria.get(critIndex);
			for (int altIndex=0;altIndex<alternatives.size();altIndex++) {
				double partUtil = computePartialUtility(critIndex, crit, altIndex);
				utilities[altIndex] += weights[critIndex] * partUtil;
			}
		}
	}

	private double computePartialUtility(int critIndex, Criterion crit, int altIndex) {
		if (crit instanceof CardinalCriterion) {
			return UtilityFunction.utility(((CardinalCriterion)crit), measurements[critIndex][altIndex]);
		} else if (crit instanceof OrdinalCriterion) {
			// ordinal ones are directly as simulated partial utility function values
			return measurements[critIndex][altIndex];
		} else {
			throw new RuntimeException("Unknown criterion type");
		}
	}

	private void clearUtilities() {
		Arrays.fill(utilities, 0.0);
	}

	private void sampleCriteria() {
		for (int i=0;i<criteria.size();i++) {
			sampler.sample(criteria.get(i), measurements[i]);
		}
	}

	private void generateWeights() {
		RandomUtil.createSumToOneRand(weights); 
	}
	

	private void init() {
		int numAlts = alternatives.size();
		int numCrit = criteria.size();
		weights = new double[numCrit];
		measurements = new double[numCrit][numAlts];
		utilities = new double[numAlts];
		ranks = new Integer[numAlts];
	}	
}
