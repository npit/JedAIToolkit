/*
* Copyright [2016] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package EntityMatching;

import BlockBuilding.*;
import DataModel.IdDuplicates;
import Utilities.DataStructures.AbstractDuplicatePropagation;
import BlockProcessing.IBlockProcessing;
import Utilities.DataStructures.UnilateralDuplicatePropagation;
import DataModel.AbstractBlock;
import DataModel.EntityProfile;
import DataModel.SimilarityPairs;
import DataReader.EntityReader.IEntityReader;
import DataReader.EntityReader.EntitySerializationReader;
import DataReader.GroundTruthReader.GtSerializationReader;
import DataReader.GroundTruthReader.IGroundTruthReader;
import Utilities.Enumerations.BlockBuildingMethod;
import Utilities.Enumerations.RepresentationModel;
import Utilities.Enumerations.SimilarityMetric;
import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */

public class TestAllMethods {
    public static void main(String[] args) {
        String entitiesFilePath =    "/home/nik/work/iit/entity-linking/JedAIToolkit/datasets/cleanCleanERfiles/dblpProfiles";
        String groundTruthFilePath = "/home/nik/work/iit/entity-linking/JedAIToolkit/datasets/cleanCleanERfiles/dblpScholarIdDuplicates";
        
        IEntityReader eReader = new EntitySerializationReader(entitiesFilePath);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles.size());

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        Set<IdDuplicates> sid = gtReader.getDuplicatePairs(eReader.getEntityProfiles());
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(
                gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());
        
        for (BlockBuildingMethod blbuMethod : BlockBuildingMethod.values()) {
            
            System.out.println("\n\nCurrent blocking metohd\t:\t" + blbuMethod);
            IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(blbuMethod);
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles, null);
            System.out.println("Original blocks\t:\t" + blocks.size());
            
            IBlockProcessing blockCleaningMethod = BlockBuildingMethod.getDefaultBlockCleaning(blbuMethod);
            if (blockCleaningMethod != null) {
                blocks = blockCleaningMethod.refineBlocks(blocks);
            }
            
            IBlockProcessing comparisonCleaningMethod = BlockBuildingMethod.getDefaultComparisonCleaning(blbuMethod);
            if (comparisonCleaningMethod != null) {
                blocks = comparisonCleaningMethod.refineBlocks(blocks);
            }
            
            for (RepresentationModel model : RepresentationModel.values()) {
                IEntityMatching pm = new ProfileMatcher(model, SimilarityMetric.getModelDefaultSimMetric(model));
                SimilarityPairs simPairs = pm.executeComparisons(blocks, profiles);
                for (int i = 0; i < 10; i++) {
                    System.out.println(simPairs.getEntityIds1()[i] + "\t\t" + simPairs.getEntityIds2()[i] + "\t\t" + simPairs.getSimilarities()[i]);
                }
            }
        }
    }
}
