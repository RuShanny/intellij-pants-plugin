// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.platform.ProjectTemplate;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class PantsScalaProjectTemplate extends PantsProjectTemplate {
  @NotNull
  @Override
  public String getName() {
    return "Scala";
  }

  @Nullable
  @Override
  public String getDescription() {
    return "Pants Scala project";
  }

  @NotNull
  @Override
  public AbstractModuleBuilder createModuleBuilder() {
    return new PantsScalaModuleBuilder();
  }

  @Override
  public Icon getIcon() {
    return PantsIcons.ScalaIcon;
  }
}
