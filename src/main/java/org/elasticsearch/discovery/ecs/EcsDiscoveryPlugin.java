/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.discovery.ecs;

import org.elasticsearch.SpecialPermission;
import org.elasticsearch.cloud.alicloud.AlicloudEcsServiceImpl;
import org.elasticsearch.cloud.alicloud.AlicloudModule;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.DiscoveryModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.repositories.RepositoriesModule;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class EcsDiscoveryPlugin extends Plugin {
  
    // ClientConfiguration clinit has some classloader problems
    // TODO: fix that
    static {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    Class.forName("com.amazonaws.ClientConfiguration");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    private final Settings settings;
    protected final ESLogger logger = Loggers.getLogger(EcsDiscoveryPlugin.class);

    public EcsDiscoveryPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "cloud-alicloud";
    }

    @Override
    public String description() {
        return "Cloud Alicloud Plugin";
    }

    @Override
    public Collection<Module> nodeModules() {
        Collection<Module> modules = new ArrayList<>();
        if (settings.getAsBoolean("cloud.enabled", true)) {
            modules.add(new AlicloudModule());
        }
        return modules;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        Collection<Class<? extends LifecycleComponent>> services = new ArrayList<>();
        if (settings.getAsBoolean("cloud.enabled", true)) {
            services.add(AlicloudEcsServiceImpl.class);
        }
        return services;
    }

    public void onModule(RepositoriesModule repositoriesModule) {
    }

    public void onModule(DiscoveryModule discoveryModule) {
        if (AlicloudModule.isEcsDiscoveryActive(settings, logger)) {
            discoveryModule.addDiscoveryType(EcsDiscovery.ECS, EcsDiscovery.class);
            discoveryModule.addUnicastHostProvider(EcsUnicastHostsProvider.class);
        }
    }
}
