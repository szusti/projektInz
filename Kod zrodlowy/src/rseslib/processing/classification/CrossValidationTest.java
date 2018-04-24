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


package rseslib.processing.classification;

import java.util.*;

import rseslib.structure.data.DoubleData;
import rseslib.structure.table.ArrayListDoubleDataTable;
import rseslib.structure.table.DoubleDataTable;
import rseslib.system.Configuration;
import rseslib.system.PropertyConfigurationException;
import rseslib.system.progress.EmptyProgress;
import rseslib.system.progress.Progress;

/**
 * Cross-validation object that splits a given test table
 * into a number of folds and performs the cross-validation test.
 *
 * @author      Arkadiusz Wojna
 */
public class CrossValidationTest extends Configuration
{
    /** Name of property for number of folds in the cross-validation test. */
    public static final String NO_OF_FOLDS_PROPERTY_NAME = "noOfFolds";

    /** Number of folds in the cross-validation test. */
    private final int m_nNoOfFolds = getIntProperty(NO_OF_FOLDS_PROPERTY_NAME);
    /** The set of classifiers. */
    private ClassifierSet m_Classifiers;

    /**
     * Constructs the cross-validation tester for a given classifiers.
     *
     * @param prop              Map between property names and property values.
     * @param classifiers       Multi-classifier for classifiers.
     */
    public CrossValidationTest(Properties prop, ClassifierSet classifiers) throws PropertyConfigurationException
    {
        super(prop);
        m_Classifiers = classifiers;
    }

    /**
     * Test the table.
     *
     * @param table Table to be tested.
     * @param prog  Progress object for test process.
     * @return          Map of entries: name of a classifier
     *                  and the object MulitpleTestResult with a classification result.
     * @throws InterruptedException when the user interrupts the execution.
     */
    public Map<String,MultipleTestResult> test(DoubleDataTable table, Progress prog) throws InterruptedException
    {
        int[][] m_arrConfusionMatrix = new int[table.attributes().nominalDecisionAttribute().noOfValues()][table.attributes().nominalDecisionAttribute().noOfValues()];
        for(int i=0;i<m_arrConfusionMatrix.length;i++) {
            for (int j = 0; j < table.attributes().nominalDecisionAttribute().noOfValues(); j++) {
                m_arrConfusionMatrix[i][j] = 0;
            }
        }
//        for(int i=0;i<m_arrConfusionMatrix.length;i++) {
//            for (int j = 0; j < m_arrConfusionMatrix.length; j++) {
//                System.out.print(m_arrConfusionMatrix[i][j]);
//            }
//            System.out.print("\n");
//        }

        // podzial danych na n czesci
        Collection<DoubleData>[] parts =  table.randomPartition(m_nNoOfFolds);
        Map<String,TestResult[]> mapOfAccuracyForClassifiers = new HashMap<String,TestResult[]>();
        Progress emptyProg = new EmptyProgress();
        prog.set("Cross-validation test", 2*parts.length);
        for (int cv = 0; cv < parts.length; cv++)
        {
            // utworzenie tabeli treningowej i testowej
            DoubleDataTable trnTable = new ArrayListDoubleDataTable(table.attributes());
            DoubleDataTable tstTable = new ArrayListDoubleDataTable(table.attributes());
            for (int part = 0; part < parts.length; part++)
            {
                DoubleDataTable tab = trnTable;
                if (part==cv) tab = tstTable;
                for (DoubleData obj : parts[part]) tab.add(obj);
            }

            // wyuczenie klasyfikatorow z tabeli treningowej
            m_Classifiers.train(trnTable, emptyProg);
            prog.step();

            // klasyfikacja tabeli testowej
            Map<String,TestResult> classificationResults = m_Classifiers.classify(tstTable, emptyProg);

            for (Map.Entry<String,TestResult> clRes : classificationResults.entrySet())
            {
                // matrix
                for(int i=0;i<clRes.getValue().decisionAttribute().noOfValues();i++){
                    for(int j=0; j<clRes.getValue().decisionAttribute().noOfValues();j++){
                        m_arrConfusionMatrix[i][j] += clRes.getValue().getNoOfObjects(table.attributes().nominalDecisionAttribute().globalValueCode(i),table.attributes().nominalDecisionAttribute().globalValueCode(j));
                        //System.out.println(clRes.getValue().getNoOfObjects(table.attributes().nominalDecisionAttribute().globalValueCode(i),table.attributes().nominalDecisionAttribute().globalValueCode(j)));
                    }
                }
                // end matrix
                TestResult[] results = (TestResult[])mapOfAccuracyForClassifiers.get(clRes.getKey());
                if (results==null)
                {
                    results = new TestResult[parts.length];
                    mapOfAccuracyForClassifiers.put(clRes.getKey(), results);
                }
                results[cv] = clRes.getValue();
            }
            prog.step();
        }

        // zsumowanie wynikow
        Map<String,MultipleTestResult> crossValidationResults = new HashMap<String,MultipleTestResult>();
        for (Map.Entry<String,TestResult[]> clRes : mapOfAccuracyForClassifiers.entrySet())
            crossValidationResults.put(clRes.getKey(), new MultipleTestResult(clRes.getValue(), m_arrConfusionMatrix, table.attributes().nominalDecisionAttribute()));
        return crossValidationResults;
    }
}
