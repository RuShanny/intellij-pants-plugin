// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

/**
 * Created by rushana on 23.05.15.
 */

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class BuildScriptDataBuilder {
  @NotNull private final VirtualFile myBuildScriptFile;
  private final Set<String> plugins = ContainerUtil.newTreeSet();
  private final Set<String> repositories = ContainerUtil.newTreeSet();
  private final Set<String> dependencies = ContainerUtil.newTreeSet();
  private final Set<String> properties = ContainerUtil.newTreeSet();
  private final Set<String> other = ContainerUtil.newTreeSet();

  public BuildScriptDataBuilder(@NotNull VirtualFile buildScriptFile) {
    myBuildScriptFile = buildScriptFile;
  }

  @NotNull
  public VirtualFile getBuildScriptFile() {
    return myBuildScriptFile;
  }

  public String build() {
    List<String> lines = ContainerUtil.newArrayList();

    final Function<String, String> padding = new Function<String, String>() {
      @Override
      public String fun(String s) {
        return StringUtil.isNotEmpty(s) ? "    " + s : "";
      }
    };
    if (!plugins.isEmpty()) {
      lines.addAll(plugins);
      lines.add("");
    }
    if (!properties.isEmpty()) {
      lines.addAll(properties);
      lines.add("");
    }
    if (!repositories.isEmpty()) {
      lines.add("repositories {");
      lines.addAll(ContainerUtil.map(repositories, padding));
      lines.add("}");
      lines.add("");
    }
    if (!dependencies.isEmpty()) {
      lines.add("dependencies {");
      lines.addAll(ContainerUtil.map(dependencies, padding));
      lines.add("}");
      lines.add("");
    }
    if (!other.isEmpty()) {
      lines.addAll(other);
    }
    return StringUtil.join(lines, "\n");
  }

  public BuildScriptDataBuilder addPluginDefinition(@NotNull String definition) {
    plugins.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addRepositoriesDefinition(@NotNull String definition) {
    repositories.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addDependencyNotation(@NotNull String notation) {
    dependencies.add(notation.trim());
    return this;
  }

  public BuildScriptDataBuilder addPropertyDefinition(@NotNull String definition) {
    properties.add(definition.trim());
    return this;
  }

  public BuildScriptDataBuilder addOther(@NotNull String definition) {
    other.add(definition.trim());
    return this;
  }
}
