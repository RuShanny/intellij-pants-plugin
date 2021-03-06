// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Consumer;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

public class OSSProjectInfoParserTest extends OSSPantsIntegrationTest {
  private static Consumer<String> STRING_CONSUMER = new Consumer<String>() {
    public void consume(final String t) {
    }
  };

  @NotNull
  public ProjectInfo resolveProjectInfo(@NotNull String relativeProjectPath) {
    final String absoluteProjectPath = FileUtil.join(myProjectRoot.getPath(), relativeProjectPath);
    final PantsResolver resolver = new PantsResolver(PantsCompileOptionsExecutor.create(
      absoluteProjectPath, new PantsExecutionSettings(), true
    ));
    resolver.resolve(STRING_CONSUMER, null);
    final ProjectInfo projectInfo = resolver.getProjectInfo();
    assertNotNull(projectInfo);
    return projectInfo;
  }

  public void testTargetType() {
    final ProjectInfo info = resolveProjectInfo("examples/src/scala/org/pantsbuild/example/hello/");

    final TargetInfo welcomeTarget = info.getTarget("examples/src/scala/org/pantsbuild/example/hello/welcome:welcome");
    assertNotNull(welcomeTarget);
    assertTrue(welcomeTarget.isScalaTarget());

    final TargetInfo greetTarget = info.getTarget("examples/src/java/org/pantsbuild/example/hello/greet:greet");
    assertNotNull(greetTarget);
    assertFalse(greetTarget.isScalaTarget());
  }
}
