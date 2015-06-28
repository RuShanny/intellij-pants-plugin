// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SdkSettingsStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Condition;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsModuleBuilder extends AbstractExternalModuleBuilder<PantsProjectSettings> {
  public PantsModuleBuilder() {
    super(PantsConstants.SYSTEM_ID, new PantsProjectSettings());
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {

  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
    final AbstractExternalModuleBuilder builder = this;
    return new SdkSettingsStep(
      settingsStep, this, new Condition<SdkTypeId>() {
        @Override
        public boolean value(SdkTypeId id) {
          return builder.isSuitableSdkType(id);
        }
      }
    );
  }
}
