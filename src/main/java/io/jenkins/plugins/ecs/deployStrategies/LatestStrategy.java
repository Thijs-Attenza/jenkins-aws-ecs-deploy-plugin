package io.jenkins.plugins.ecs.deployStrategies;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.*;
import hudson.AbortException;
import hudson.model.TaskListener;

import java.util.List;
import java.util.Objects;

public class LatestStrategy implements StrategyInterface {

    @Override
    public void build(TaskListener listener, AmazonECS client, BuildTarget buildTarget) throws AbortException {
        listener.getLogger().println("Start deploying build " + buildTarget.getBuildNumber() + " on ECS Service \"" + buildTarget.getService() + "\" using Latest strategy.");

        DescribeTaskDefinitionRequest describeTaskDefinitionRequest = (new DescribeTaskDefinitionRequest()).withTaskDefinition(buildTarget.getTaskDefinition());
        TaskDefinition taskDefinitionResult = client.describeTaskDefinition(describeTaskDefinitionRequest).getTaskDefinition();
        String currentImage = taskDefinitionResult.getContainerDefinitions().get(0).getImage();
        String targetImage = buildTarget.getRepository() + buildTarget.getImageName() + ":latest";
        String taskDefinition = taskDefinitionResult.getFamily() + ":" + taskDefinitionResult.getRevision();

        listener.getLogger().println("Target image: " + targetImage);

        if(!currentImage.equals(targetImage)) {
            taskDefinition = updateDefinition(listener, client, taskDefinitionResult, targetImage);
        }

        //Deploy
        UpdateServiceRequest updateServiceRequest = (new UpdateServiceRequest())
                .withCluster(buildTarget.getClusterArn())
                .withService(buildTarget.getService())
                .withTaskDefinition(taskDefinition)
                .withForceNewDeployment(true);
        client.updateService(updateServiceRequest);
        listener.getLogger().println("Updated service \"" + buildTarget.getService() + "\" on cluster \"" + buildTarget.getClusterArn() + "\"");
    }

    private String updateDefinition(TaskListener listener, AmazonECS client, TaskDefinition taskDefinitionResult, String image) {
        //Update container Definition
        ContainerDefinition updatedContainerDefinition = taskDefinitionResult.getContainerDefinitions().get(0);
        updatedContainerDefinition.setImage(image);

        //Register updated definition
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = (new RegisterTaskDefinitionRequest())
                .withContainerDefinitions(List.of(updatedContainerDefinition))
                .withFamily(taskDefinitionResult.getFamily())
                .withExecutionRoleArn(taskDefinitionResult.getExecutionRoleArn())
                .withNetworkMode(taskDefinitionResult.getNetworkMode())
                .withVolumes(taskDefinitionResult.getVolumes())
                .withPlacementConstraints(taskDefinitionResult.getPlacementConstraints())
                .withRequiresCompatibilities(taskDefinitionResult.getRequiresCompatibilities())
                .withCpu(taskDefinitionResult.getCpu())
                .withMemory(taskDefinitionResult.getMemory());
        RegisterTaskDefinitionResult registerTaskDefinitionResult = client.registerTaskDefinition(registerTaskDefinitionRequest);
        String newTaskDefinition = registerTaskDefinitionResult.getTaskDefinition().getFamily() + ":" + registerTaskDefinitionResult.getTaskDefinition().getRevision();
        listener.getLogger().println("Created new Task Definition: " + newTaskDefinition);

        return newTaskDefinition;
    }
}
