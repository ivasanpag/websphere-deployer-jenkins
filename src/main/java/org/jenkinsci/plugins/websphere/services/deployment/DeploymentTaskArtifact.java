package org.jenkinsci.plugins.websphere.services.deployment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentTaskArtifact {

    private String taskName;
    private String columnName;
    private Map<String, String> propertiesToChange; //UniqueName-Value

    public Map<String, String> getPropertiesToChange() {
        if(propertiesToChange == null)
            propertiesToChange = new HashMap<>();
        return propertiesToChange;
    }

    public DeploymentTaskArtifact(String taskPreferences) {
        String[] temp = taskPreferences.split(";");
        // 0 => task; 1=> columnToChange; 2,3,...=>uniqueIdentifier=ValueToChange
        this.taskName = temp[0];
        this.columnName = temp[1];
        this.propertiesToChange = new HashMap<>();
        for (int i = 2; i < temp.length; i++) {
            if(temp[i].contains("=")) {
                String[] properties = temp[i].split("=");
                if(properties.length == 2)
                    propertiesToChange.put(properties[0], properties[1]);
            }
        }

    }

}
