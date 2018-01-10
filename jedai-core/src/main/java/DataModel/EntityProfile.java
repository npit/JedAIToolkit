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

package DataModel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */

public class EntityProfile implements Serializable {

    private static final long serialVersionUID = 122354534453243447L;

    private final Set<Attribute> attributes;
    private final String entityUrl;

    public EntityProfile(String url) {
        entityUrl = url;
        attributes = new HashSet();
    }

    public void addAttribute(String propertyName, String propertyValue) {
        attributes.add(new Attribute(propertyName, propertyValue));
    }

    public String getEntityUrl() {
        return entityUrl;
    }

    public int getProfileSize() {
        return attributes.size();
    }
    
    public Set<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString(){
        String str = "url = " + this.entityUrl + ", attributes = {";
        for(Attribute a : attributes){
            str += " (" + a.getName() + " = " + a.getValue() + ")";
        }
        str+="}";

        return str;
    }
    public String short_str(){
        String str = "(" + this.getEntityUrl() +", "+ test_multiling.test_multiling.getEntityValue(this,"topic_id") + ")";
        return str;
    }
}