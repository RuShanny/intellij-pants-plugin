// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PantsProjectTemplatesFactory extends ProjectTemplatesFactory {
  public static final String PANTS = "Pants";

  @NotNull
  @Override
  public String[] getGroups() {
    return new String[] {PANTS};
  }

  @Override
  public Icon getGroupIcon(String group) {
    return PantsIcons.Icon;
  }

  @NotNull
  @Override
  public ProjectTemplate[] createTemplates(String s, WizardContext context) {
    context.getProject();

    return new ProjectTemplate[]{
      new PantsJavaProjectTemplate(),
      new PantsScalaProjectTemplate()
    };
  }

}
