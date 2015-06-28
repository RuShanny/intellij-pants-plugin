// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PantsModuleBuilder extends AbstractExternalModuleBuilder<PantsProjectSettings> {
  private static final String DEFAULT_TEMPLATE_PANTS_BUILD = "Java Build Template";
  private static final Key<BuildScriptDataBuilder> BUILD_SCRIPT_DATA =
    Key.create("gradle.module.buildScriptData");


  private @NotNull WizardContext myWizardContext;
  private static final Logger LOG = Logger.getInstance(PantsModuleBuilder.class);

  public PantsModuleBuilder() {
    super(PantsConstants.SYSTEM_ID, new PantsProjectSettings());
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    myWizardContext = wizardContext;
    return super.createWizardSteps(wizardContext, modulesProvider);
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    String contentEntryPath = getContentEntryPath();
    if (StringUtil.isEmpty(contentEntryPath)) {
      return;
    }
    File contentRootDir = new File(contentEntryPath);
    FileUtilRt.createDirectory(contentRootDir);
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);
    if (modelContentRootDir == null) {
      return;
    }

    modifiableRootModel.addContentEntry(modelContentRootDir);
    // todo this should be moved to generic ModuleBuilder
    if (myJdk != null){
      modifiableRootModel.setSdk(myJdk);
    } else {
      modifiableRootModel.inheritSdk();
    }

    final Project project = modifiableRootModel.getProject();

    setupPantsBuildFile(modelContentRootDir);

    if (myWizardContext.isCreatingNewProject()) {
      String externalProjectPath = FileUtil.toCanonicalPath(project.getBasePath());
      getExternalProjectSettings().setExternalProjectPath(externalProjectPath);
      AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID);
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      //noinspection unchecked
      settings.linkProject(getExternalProjectSettings());
    }
    else {
      FileDocumentManager.getInstance().saveAllDocuments();
      ExternalSystemUtil.refreshProjects(project, PantsConstants.SYSTEM_ID, false);
    }
  }

  @Nullable
  private VirtualFile setupPantsBuildFile(@NotNull VirtualFile modelContentRootDir) throws ConfigurationException {
    final VirtualFile file = getExternalProjectConfigFile(modelContentRootDir.getPath(), PantsConstants.DEFAULT_SCRIPT_NAME);
    final String templateName = DEFAULT_TEMPLATE_PANTS_BUILD;

    Map attributes = ContainerUtil.newHashMap();
    if (file != null) {
      saveFile(file, templateName, attributes);
    }
    return file;
  }

  @Nullable
  private static VirtualFile getExternalProjectConfigFile(@NotNull String parent, @NotNull String fileName) {
    File file = new File(parent, fileName);
    FileUtilRt.createIfNotExists(file);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  private static void saveFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    //getInternalTemplate looks for file with templateName in hash-table
    // FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData();
      VfsUtil.saveText(file, buildScriptDataBuilder != null ? buildScriptDataBuilder.build() : "");
      //VfsUtil.saveText(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on applying template %s config", PantsConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't apply %s template config text", PantsConstants.SYSTEM_ID.getReadableName())
      );
    }
  }

  @Nullable
  public static BuildScriptDataBuilder getBuildScriptData() {
    // this is just a stub
    return new BuildScriptDataBuilder("JavaSDK");
  }

}
