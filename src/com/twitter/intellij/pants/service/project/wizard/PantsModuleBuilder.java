/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsProjectSettingsControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Denis Zhdanov
 * @since 6/26/13 11:10 AM
 */
public class PantsModuleBuilder extends AbstractExternalModuleBuilder<PantsProjectSettings> {

  private static final Logger LOG = Logger.getInstance(PantsModuleBuilder.class);

  private static final String TEMPLATE_Pants_SETTINGS = "Pants Settings.Pants";
  private static final String TEMPLATE_Pants_SETTINGS_MERGE = "Pants Settings merge.Pants";
  private static final String TEMPLATE_PANTS_BUILD_WITH_WRAPPER = "Pants Build Script with wrapper.Pants";
  private static final String DEFAULT_TEMPLATE_PANTS_BUILD = "Pants Build Script.Pants";

  private static final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_DIR_NAME = "MODULE_DIR_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";

  private @NotNull WizardContext myWizardContext;

  public String getBuilderId() {

    return getClass().getName();
  }

  @Override
  public String getPresentableName() {
    return "Pants";
  }
  /*@Override
  public String getDescription() {
    return bundle.getString("AsposeWizardPanel.myMainPanel.description");
  }*/


  @Override
  public Icon getBigIcon() {
    return PantsIcons.Icon;
  }

  @Override
  public Icon getNodeIcon() {
    return PantsIcons.Logo;
  }


  public PantsModuleBuilder() {
    super(PantsConstants.SYSTEM_ID, new PantsProjectSettings());
  }

  @Override
  public void setupRootModel(final ModifiableRootModel modifiableRootModel) throws ConfigurationException {
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
    setupPantsSettingsFile(modelContentRootDir, modifiableRootModel);

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

  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    myWizardContext = wizardContext;
    return super.createWizardSteps(wizardContext, modulesProvider);
  }

  @Nullable
  @Override
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    if (!myWizardContext.isCreatingNewProject()) return new ModuleWizardStep() {
      @Override
      public JComponent getComponent() {
        return new JPanel();
      }

      @Override
      public void updateDataModel() {

      }
    };
    final PantsProjectSettingsControl settingsControl = new PantsProjectSettingsControl(getExternalProjectSettings());
    return new ExternalModuleSettingsStep<PantsProjectSettings>(this, settingsControl);
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return sdk instanceof JavaSdkType;
  }

  @Override
  public String getParentGroup() {
    return JavaModuleType.BUILD_TOOLS_GROUP;
  }

  @Override
  public int getWeight() {
    return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
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
  private VirtualFile setupPantsSettingsFile(@NotNull VirtualFile modelContentRootDir, @NotNull ModifiableRootModel model)
    throws ConfigurationException {
    VirtualFile file = null;
    if (myWizardContext.isCreatingNewProject()) {
      final String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, model.getProject().getBaseDir(), '/');
      file = getExternalProjectConfigFile(model.getProject().getBasePath(), PantsConstants.SETTINGS_FILE_NAME);
      if (file == null) return null;

      Map<String, String> attributes = ContainerUtil.newHashMap();
      final String projectName = model.getProject().getName();
      attributes.put(TEMPLATE_ATTRIBUTE_PROJECT_NAME, projectName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_DIR_NAME, moduleDirName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, model.getModule().getName());
      saveFile(file, TEMPLATE_Pants_SETTINGS, attributes);
    }
    else {
      Map<String, Module> moduleMap = ContainerUtil.newHashMap();
      for (Module module : ModuleManager.getInstance(model.getProject()).getModules()) {
        for (ContentEntry contentEntry : model.getContentEntries()) {
          if (contentEntry.getFile() != null) {
            moduleMap.put(contentEntry.getFile().getPath(), module);
          }
        }
      }

      VirtualFile virtualFile = modelContentRootDir;
      Module module = null;
      while (virtualFile != null && module == null) {
        module = moduleMap.get(virtualFile.getPath());
        virtualFile = virtualFile.getParent();
      }

      if (module != null) {
        String rootProjectPath = module.getOptionValue(ExternalSystemConstants.ROOT_PROJECT_PATH_KEY);

        if (!StringUtil.isEmpty(rootProjectPath)) {
          VirtualFile rootProjectFile = VfsUtil.findFileByIoFile(new File(rootProjectPath), true);
          if (rootProjectFile == null) return null;

          final String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, rootProjectFile, '/');
          file = getExternalProjectConfigFile(rootProjectPath, PantsConstants.SETTINGS_FILE_NAME);
          if (file == null) return null;

          Map<String, String> attributes = ContainerUtil.newHashMap();
          attributes.put(TEMPLATE_ATTRIBUTE_MODULE_DIR_NAME, moduleDirName);
          attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, model.getModule().getName());
          appendToFile(file, TEMPLATE_Pants_SETTINGS_MERGE, attributes);
        }
      }
    }
    return file;
  }

  private static void saveFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      VfsUtil.saveText(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on applying template %s config", PantsConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't apply %s template config text", PantsConstants.SYSTEM_ID.getReadableName())
      );
    }
  }

  private static void appendToFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      VfsUtil.saveText(file, VfsUtilCore.loadText(file) +
                             (templateAttributes != null ? template.getText(templateAttributes) : template.getText()));
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on appending template %s config", PantsConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't append %s template config text", PantsConstants.SYSTEM_ID.getReadableName())
      );
    }
  }


  @Nullable
  private static VirtualFile getExternalProjectConfigFile(@NotNull String parent, @NotNull String fileName) {
    File file = new File(parent, fileName);
    FileUtilRt.createIfNotExists(file);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }
}
