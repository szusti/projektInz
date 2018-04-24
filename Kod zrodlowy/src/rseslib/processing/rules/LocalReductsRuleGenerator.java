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


package rseslib.processing.rules;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import rseslib.processing.reducts.LocalReductsProvider;
import rseslib.structure.data.DoubleData;
import rseslib.structure.indiscernibility.Indiscernibility;
import rseslib.structure.rule.Rule;
import rseslib.structure.rule.EqualityDescriptorsRule;
import rseslib.structure.table.DoubleDataTable;
import rseslib.system.Configuration;
import rseslib.system.PropertyConfigurationException;
import rseslib.system.progress.Progress;

/**
 * @author Rafal Latkowski
 *
 */
public class LocalReductsRuleGenerator extends Configuration implements RuleGenerator
{
    public static final String s_sAllowComparingMissingValues = "MissingValueDescriptorsInRules";

    boolean m_bAllowComparingMissingValues = true;
        
    /**
     * @throws PropertyConfigurationException 
     * 
     */
    public LocalReductsRuleGenerator(Properties prop) throws PropertyConfigurationException
    {
        super(prop);
        m_bAllowComparingMissingValues = getBoolProperty(s_sAllowComparingMissingValues);
        //System.out.println("Prop: ");
        //prop.list(System.out);

    }

    /**
     * @see rseslib.processing.rules.RuleGenerator#generate(rseslib.structure.table.DoubleDataTable, Progress)
     */
    public Collection<Rule> generate(DoubleDataTable tab, Progress prog) throws PropertyConfigurationException, InterruptedException
    {
        prog.set("Generating local reducts and rules", tab.getDataObjects().size());
    	LocalReductsProvider reductsProv = new LocalReductsProvider(getProperties(), tab);
        Indiscernibility ind = reductsProv.getIndiscernibilityForMissing();
        HashSet<Rule> decisionRules = new HashSet<Rule>();
        for (DoubleData object : tab.getDataObjects())
        {
            Collection<BitSet> reducts = reductsProv.getSingleObjectReducts(object);
            for (BitSet reduct : reducts)
            {
            	EqualityDescriptorsRule rule = new EqualityDescriptorsRule(reduct,object,ind);
                if (m_bAllowComparingMissingValues || !rule.hasDescriptorWithMissingValue())
                	decisionRules.add(rule);
            }
            prog.step();
        }
        new RuleStatisticsProvider().calculateStatistics(decisionRules,tab);
        return decisionRules;
    }
}
