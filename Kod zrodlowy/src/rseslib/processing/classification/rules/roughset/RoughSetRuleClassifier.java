/*
 * Copyright (C) 2002 - 2016 Logic Group, Institute of Mathematics, Warsaw University
 * 
 *  This file is part of Rseslib.
 *
 *  Rseslib is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Rseslib is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package rseslib.processing.classification.rules.roughset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import rseslib.system.*; 
import rseslib.system.progress.Progress;
import rseslib.structure.table.DoubleDataTable;
import rseslib.structure.vector.Vector;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.*;
import rseslib.structure.rule.*;
import rseslib.processing.classification.Classifier;
import rseslib.processing.rules.GlobalReductsRuleGenerator;
import rseslib.processing.rules.JohnsonReductsRuleGenerator;
import rseslib.processing.rules.LocalReductsRuleGenerator;
import rseslib.processing.rules.RuleGenerator;

/**
 * Rough set based classifier.
 * 
 * @author Rafal Latkowski
 */
public class RoughSetRuleClassifier extends ConfigurationWithStatistics implements Classifier, Serializable
{
	public enum GeneratorType { LocalReducts, GlobalReducts, JohnsonReducts; }

    /** Serialization version. */
	private static final long serialVersionUID = 1L;

	public static final String s_sRuleGenerator = "RuleGenerator";

    GeneratorType m_nRuleGeneratorMethod; 
    Collection<Rule> m_cDecisionRules = null;
    NominalAttribute m_DecAttr;
    
    /**
     * Constructor required by rseslib tools.
     *
     * @param prop                   Settings of this clasifier.
     * @param trainTable             Table used to generate rules.
     * @param prog                   Progress object to report training progress.
     * @throws InterruptedException 			when a user interrupts the execution.
     * @throws PropertyConfigurationExcpetion 	when the properties are incorrect.
     */
    public RoughSetRuleClassifier(Properties prop, DoubleDataTable trainTable, Progress prog) throws PropertyConfigurationException, InterruptedException
    {
        super(prop);
        setRuleGeneratorMethod(getProperty(s_sRuleGenerator));
        m_DecAttr = trainTable.attributes().nominalDecisionAttribute();
        // There are some properties for rules within bmorg
        RuleGenerator rulgen = null;
        switch (m_nRuleGeneratorMethod)
        {
        	case GlobalReducts:
                //System.out.println("Using global reducts");
                rulgen = new GlobalReductsRuleGenerator(getProperties());
                break;
        	case LocalReducts:
                //System.out.println("Using local reducts");
                rulgen = new LocalReductsRuleGenerator(getProperties());
                break;
        	case JohnsonReducts:
                //System.out.println("Using Johnons's reducts");
                rulgen = new JohnsonReductsRuleGenerator(getProperties());
                break;
        }
        
        m_cDecisionRules = rulgen.generate(trainTable, prog);
        //System.out.println(rules);
    }

    /**
     * 
     */
    public RoughSetRuleClassifier(Collection<Rule> rules, NominalAttribute decAttr)
    {
        m_cDecisionRules=rules;
        m_DecAttr = decAttr;
    }

    private void setRuleGeneratorMethod(String method) throws PropertyConfigurationException
    {
        //System.out.println("Setting "+method);
    	try
    	{
    		m_nRuleGeneratorMethod = GeneratorType.valueOf(method);
    	}
    	catch (IllegalArgumentException e)
    	{
			throw new PropertyConfigurationException("Unknown rule generator type: "+method);
        }
    }
    
    /**
     * Writes this object.
     *
     * @param out			Output for writing.
     * @throws IOException	if an I/O error has occured.
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
    	writeConfigurationAndStatistics(out);
    	out.defaultWriteObject();
    }

    /**
     * Reads this object.
     *
     * @param out			Output for writing.
     * @throws IOException	if an I/O error has occured.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
    	readConfigurationAndStatistics(in);
    	in.defaultReadObject();
    }

    public double classify(DoubleData object)
    {
        Vector dv = new Vector(m_DecAttr.noOfValues());
        int rules=0;
        for (Rule rule : m_cDecisionRules)
            if (rule.matches(object)) 
            { 
                dv.add(((DistributedDecisionRule)rule).getDecisionVector()); 
                rules++; 
            }
        if (rules==0) return Double.NaN;
       	int best = 0;
       	for (int i=1;i<dv.dimension();i++)
       	{
       		if (dv.get(i)>dv.get(best)) 
                best=i;
       	}
       	double result=m_DecAttr.globalValueCode(best);
        return result;
    }
    
    /**
     * Calculates statistics.
     */
    public void calculateStatistics()
    {
    	addToStatistics("number_of_rules",Integer.toString(m_cDecisionRules.size()));
    }

    /**
     * Resets statistics.
     */
    public void resetStatistics()
    {
    }
    
    /**
     * Returns collection of rules induced by this classifier.
     * @return collection of rules induced by this classifier.
     */
    public Collection<Rule> getRules()
    {
        return m_cDecisionRules;
    }
}
