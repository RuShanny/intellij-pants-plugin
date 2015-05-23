// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.newProjectWizard.AddSupportForFrameworksPanel;
import com.intellij.ide.util.newProjectWizard.impl.FrameworkSupportModelBase;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Created by rushana on 23.05.15.
 */
public class PantsFrameworksWizardStep extends ModuleWizardStep implements Disposable {

  private JPanel myPanel;
  private final AddSupportForFrameworksPanel myFrameworksPanel;
  private JPanel myFrameworksPanelPlaceholder;
  private JPanel myOptionsPanel;
  @SuppressWarnings("unused") private JBLabel myFrameworksLabel;

  public PantsFrameworksWizardStep(WizardContext context, final PantsModuleBuilder builder) {

    Project project = context.getProject();
    final LibrariesContainer container = LibrariesContainerFactory.createContainer(context.getProject());
    FrameworkSupportModelBase model = new FrameworkSupportModelBase(project, null, container) {
      @NotNull
      @Override
      public String getBaseDirectoryForLibrariesPath() {

        return StringUtil.notNullize(builder.getContentEntryPath());
      }
    };

    myFrameworksPanel =
      new AddSupportForFrameworksPanel(Collections.<FrameworkSupportInModuleProvider>emptyList(), model, true, null);

    List<FrameworkSupportInModuleProvider> providers = ContainerUtil.newArrayList();
    // TODO: what to do with these methods? Do we need them?
    //Collections.addAll(providers, PantsFrameworkSupportProvider.EP_NAME.getExtensions());

    //myFrameworksPanel.setProviders(providers, Collections.<String>emptySet(), Collections.singleton(PantsJavaFrameworkSupportProvider.ID));
    Disposer.register(this, myFrameworksPanel);
    myFrameworksPanelPlaceholder.add(myFrameworksPanel.getMainPanel());

    ModuleBuilder.ModuleConfigurationUpdater configurationUpdater = new ModuleBuilder.ModuleConfigurationUpdater() {
      @Override
      public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
        myFrameworksPanel.addSupport(module, rootModel);
      }
    };
    builder.addModuleConfigurationUpdater(configurationUpdater);

    ((CardLayout)myOptionsPanel.getLayout()).show(myOptionsPanel, "frameworks card");
  }

  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public void updateDataModel() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void disposeUIResources() {

    Disposer.dispose(this);
  }
}

