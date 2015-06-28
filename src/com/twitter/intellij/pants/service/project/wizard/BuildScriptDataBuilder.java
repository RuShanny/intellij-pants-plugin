// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class BuildScriptDataBuilder {
  //@NotNull private VirtualFile myBuildScriptFile;
  @NotNull private final String sdkType;
  private final Set<String> dependencies = ContainerUtil.newTreeSet();

  public BuildScriptDataBuilder(@NotNull String sdkType) {
    this.sdkType = sdkType;
  }

  public String build() {
    List<String> lines = ContainerUtil.newArrayList();

    final Function<String, String> padding = new Function<String, String>() {
      @Override
      public String fun(String s) {
        return StringUtil.isNotEmpty(s) ? "    " + s : "";
      }
    };

    // This is just a stub, will remove later
    if (sdkType == "ScalaSDK") {
      lines.add("scala_library(");
    } else {
      lines.add("java_library(");
    }

    lines.add("\tdependencies = [");
    if (!dependencies.isEmpty()) {
      lines.addAll(ContainerUtil.map(dependencies, padding));
    }
    lines.add("],");

    // This is just a stub, will remove later
    if (sdkType == "ScalaSDK") {
      lines.add("sources = globs('*.scala')");
    } else {
      lines.add("sources = globs('*.java')");
    }

    lines.add(")");
    return StringUtil.join(lines, "\n");
  }

  public BuildScriptDataBuilder addDependencyNotation(@NotNull String notation) {
    dependencies.add(notation.trim());
    return this;
  }

}
