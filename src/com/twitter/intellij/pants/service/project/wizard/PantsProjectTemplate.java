// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PantsProjectTemplate implements ProjectTemplate {
  @NotNull
  @Override
  public AbstractModuleBuilder createModuleBuilder() {
    return new PantsJavaModuleBuilder();
  }

  @Nullable
  @Override
  public ValidationInfo validateSettings() {
    return null;
  }
}
