package io.jenkins.plugins.ecs.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

public class Client extends AbstractDescribableImpl<Client> implements Serializable {
    private String region;

    @DataBoundConstructor
    public Client(String region) {
        this.region = region;
    }

    public String getRegion() {
        return region;
    }

    @DataBoundSetter
    public void setRegion(String region) {
        this.region = Util.fixEmptyAndTrim(region);
    }

    public AmazonECS build() {
        final AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard();

        if (region != null && !region.isEmpty()) {
            builder.setRegion(region);
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(region, client.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region);
    }

    @Extension
    @Symbol("client")
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends Descriptor<Client> {
        @Override
        @Nonnull
        public String getDisplayName() {
            return "CLIENT";//Messages.client();
        }

        public ListBoxModel doFillRegionItems() {
            final ListBoxModel regions = new ListBoxModel();
            regions.add("", "");
            for (Regions s : Regions.values()) {
                regions.add(s.getDescription(), s.getName());
            }
            return regions;
        }
    }
}

