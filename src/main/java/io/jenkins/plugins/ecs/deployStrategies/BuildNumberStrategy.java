package io.jenkins.plugins.ecs.deployStrategies;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.*;
import hudson.AbortException;
import hudson.model.TaskListener;

import java.util.List;
import java.util.Objects;

public class BuildNumberStrategy implements StrategyInterface {

    @Override
    public void build(TaskListener listener, AmazonECS client, BuildTarget buildTarget) throws AbortException {
        listener.getLogger().println("Start deploying build " + buildTarget.getBuildNumber() + " on ECS Service \"" + buildTarget.getService() + "\" using Build Number strategy.");

        DescribeTaskDefinitionRequest describeTaskDefinitionRequest = (new DescribeTaskDefinitionRequest()).withTaskDefinition(buildTarget.getTaskDefinition());
        TaskDefinition taskDefinitionResult = client.describeTaskDefinition(describeTaskDefinitionRequest).getTaskDefinition();
        String currentImage = taskDefinitionResult.getContainerDefinitions().get(0).getImage();
        String newImage = buildTarget.getRepository() + buildTarget.getImageName() + ":" + buildTarget.getBuildNumber();
        listener.getLogger().println("Target image: " + newImage);

        if (Objects.equals(currentImage, newImage)) {
            throw new AbortException("Image already exists: " + currentImage);
        }

        //Update container Definition
        ContainerDefinition updatedContainerDefinition = taskDefinitionResult.getContainerDefinitions().get(0);
        updatedContainerDefinition.setImage(newImage);

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

        //Deploy
        UpdateServiceRequest updateServiceRequest = (new UpdateServiceRequest())
                .withCluster(buildTarget.getClusterArn())
                .withService(buildTarget.getService())
                .withTaskDefinition(newTaskDefinition);
        client.updateService(updateServiceRequest);
        listener.getLogger().println("Updated service \"" + buildTarget.getService() + "\" on cluster \"" + buildTarget.getClusterArn() + "\"");
    }
}
