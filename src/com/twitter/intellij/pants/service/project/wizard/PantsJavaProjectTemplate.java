// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class PantsJavaProjectTemplate extends PantsProjectTemplate {
  @NotNull
  @Override
  public String getName() {
    return "Java";
  }

  @Nullable
  @Override
  public String getDescription() {
    return "Pants Java project";
  }

  @Override
  public Icon getIcon() {
    return PantsIcons.JavaIcon;
  }
}
