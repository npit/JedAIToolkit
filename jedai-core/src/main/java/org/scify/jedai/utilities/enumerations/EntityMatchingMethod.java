/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
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

package org.scify.jedai.utilities.enumerations;

import org.scify.jedai.entitymatching.GroupLinkage;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;

/**
 *
 * @author GAP2
 */
public enum EntityMatchingMethod {
    GROUP_LINKAGE,
    PROFILE_MATCHER;
    
    public static IEntityMatching getDefaultConfiguration(EntityMatchingMethod emMethod) {
        switch(emMethod) {
            case GROUP_LINKAGE:
                return new GroupLinkage();
            case PROFILE_MATCHER:
                return new ProfileMatcher();
                //return new ProfileMatcher(RepresentationModel.LDA_TOPIC_MODEL, SimilarityMetric.COSINE_SIMILARITY);
                // return new ProfileMatcher(RepresentationModel.PRETRAINED_WORD_EMBEDDINGS, SimilarityMetric.COSINE_SIMILARITY);
                // return new ProfileMatcher(RepresentationModel.PRETRAINED_CHARACTER_EMBEDDINGS, SimilarityMetric.COSINE_SIMILARITY);
            default:
                return new ProfileMatcher();
        }
    }
}
