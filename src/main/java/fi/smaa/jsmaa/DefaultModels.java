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

package fi.smaa.jsmaa;

import fi.smaa.jsmaa.model.Alternative;
import fi.smaa.jsmaa.model.Interval;
import fi.smaa.jsmaa.model.OutrankingCriterion;
import fi.smaa.jsmaa.model.ScaleCriterion;
import fi.smaa.jsmaa.model.SMAA2Model;
import fi.smaa.jsmaa.model.SMAATRIModel;

public class DefaultModels {

	public static SMAA2Model getSMAA2Model() {
		SMAA2Model model = new SMAA2Model("SMAA-2 Model");
		addDefaultCriteria(model);
		addDefaultAlternatives(model);
		return model;
	}

	private static void addDefaultCriteria(SMAA2Model model) {
		model.addCriterion(new ScaleCriterion("Criterion 1"));
		model.addCriterion(new ScaleCriterion("Criterion 2"));
	}
	
	private static void addDefaultOutrankingCriteria(SMAA2Model model) {
		model.addCriterion(new OutrankingCriterion("Criterion 1", true, 
				new Interval(0.0, 0.0), new Interval(1.0, 1.0)));
		model.addCriterion(new OutrankingCriterion("Criterion 2", true, 
				new Interval(0.0, 0.0), new Interval(1.0, 1.0)));
	}	

	private static void addDefaultAlternatives(SMAA2Model model) {
		model.addAlternative(new Alternative("Alternative 1"));
		model.addAlternative(new Alternative("Alternative 2"));			
		model.addAlternative(new Alternative("Alternative 3"));
	}

	public static SMAATRIModel getSMAATRIModel() {
		SMAATRIModel model = new SMAATRIModel("SMAA-TRI Model");
		addDefaultAlternatives(model);				
		addDefaultOutrankingCriteria(model);
		model.addCategory(new Alternative("Category 1"));
		model.addCategory(new Alternative("Category 2"));
		
		return model;
	}

}
