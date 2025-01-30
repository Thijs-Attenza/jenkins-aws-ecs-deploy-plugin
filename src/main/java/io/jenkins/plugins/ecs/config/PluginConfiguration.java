package io.jenkins.plugins.ecs.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class PluginConfiguration extends GlobalConfiguration {
    private Client client;

    public PluginConfiguration() {
        load();
    }

    public static PluginConfiguration getInstance() {
        return all().get(PluginConfiguration.class);
    }

    public Client getClient() {
        return client;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setClient(Client client) {
        this.client = client;
        save();
    }

    @Override
    public synchronized boolean configure(StaplerRequest req, JSONObject json) {
        this.client = null;

        req.bindJSON(this, json);
        save();
        return true;
    }
}