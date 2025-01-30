package io.jenkins.plugins.ecs;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.*;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.ecs.config.PluginConfiguration;
import jenkins.tasks.SimpleBuildStep;
import org.apache.http.client.CredentialsProvider;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.Objects;

public class EcsDeployBuilder extends Builder implements SimpleBuildStep {
    private final String clusterArn;
    private final String service;
    private final String taskDefinition;
    private final String imageName;
    private final String assumeRole;
    private final String repository;
    public final PluginConfiguration config = PluginConfiguration.getInstance();

    @DataBoundConstructor
    public EcsDeployBuilder(String clusterArn, String service, String taskDefinition, String imageName, String assumeRole, String repository) {
        this.clusterArn = clusterArn;
        this.service = service;
        this.taskDefinition = taskDefinition;
        this.imageName = imageName;
        this.assumeRole = assumeRole;
        this.repository = repository;
    }

    public PluginConfiguration getConfig()
    {
        return config;
    }

    public String getClusterArn()
    {
        return clusterArn;
    }

    public String getService() {
        return service;
    }

    public String getTaskDefinition() {
        return taskDefinition;
    }

    public String getImageName() {
        return imageName;
    }

    public String getAssumeRole() {
        return assumeRole;
    }

    public String getRepository() {
        return repository;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws AbortException {
        String buildnumber = env.get("BUILD_NUMBER");
        listener.getLogger().println("Start deploying build " + buildnumber + " on ECS Service \"" + service + "\"");

        //If STS is configured, use the Assume Role Provider
        AWSCredentialsProvider credentialsProvider;
        if (Util.fixEmptyAndTrim(assumeRole) != null) {
            credentialsProvider = new STSAssumeRoleSessionCredentialsProvider.Builder(assumeRole, "ecs-deploy-session").build();
            listener.getLogger().println("Assuming role: " + assumeRole);
        } else {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }

        AmazonECS client = AmazonECSClientBuilder.standard().withRegion(config.getClient().getRegion()).withCredentials(credentialsProvider).build();
        DescribeTaskDefinitionRequest describeTaskDefinitionRequest = (new DescribeTaskDefinitionRequest()).withTaskDefinition(taskDefinition);
        TaskDefinition taskDefinitionResult = client.describeTaskDefinition(describeTaskDefinitionRequest).getTaskDefinition();
        String currentImage = taskDefinitionResult.getContainerDefinitions().get(0).getImage();
        String newImage = repository + imageName + ":" + buildnumber;
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
                .withCluster(clusterArn)
                .withService(service)
                .withTaskDefinition(newTaskDefinition);
        client.updateService(updateServiceRequest);
        listener.getLogger().println("Updated service \"" + service + "\" on cluster \"" + clusterArn + "\"");
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.EcsDeployBuilder_DescriptorImpl_DisplayName();
        }
    }
}

