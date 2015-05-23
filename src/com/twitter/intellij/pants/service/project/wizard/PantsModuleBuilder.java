package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.twitter.intellij.pants.settings.PantsProjectSettingsControl;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * Created by rushana on 06.05.15.
 */
public class PantsModuleBuilder extends AbstractExternalModuleBuilder<PantsProjectSettings> {

  private static final String TEMPLATE_Pants_SETTINGS = "Pants Settings.Pants";
  private static final String TEMPLATE_Pants_SETTINGS_MERGE = "Pants Settings merge.Pants";
  private static final String TEMPLATE_Pants_BUILD_WITH_WRAPPER = "Pants Build Script with wrapper.Pants";
  private static final String DEFAULT_TEMPLATE_Pants_BUILD = "Pants Build Script.Pants";

  private static final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR = "MODULE_FLAT_DIR";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_GROUP = "MODULE_GROUP";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_VERSION = "MODULE_VERSION";
  private static final String TEMPLATE_ATTRIBUTE_Pants_VERSION = "Pants_VERSION";
  private static final Key<BuildScriptDataBuilder> BUILD_SCRIPT_DATA =
    Key.create("Pants.module.buildScriptData");

  private WizardContext myWizardContext;

  @Nullable
  private ProjectData myParentProject;
  private boolean myInheritGroupId;
  private boolean myInheritVersion;
  private ProjectId myProjectId;
  private String rootProjectPath;


  private static final Logger LOG = Logger.getInstance(PantsModuleBuilder.class);
  public PantsModuleBuilder() {

    super(PantsConstants.SYSTEM_ID, new PantsProjectSettings());
  }

  public String getBuilderId() {

    return getClass().getName();
  }

  @Override
  public String getPresentableName() {
    return "Aspose";
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
    if (myParentProject != null) {
      rootProjectPath = myParentProject.getLinkedExternalProjectPath();
    }
    else {
      rootProjectPath =
        FileUtil.toCanonicalPath(myWizardContext.isCreatingNewProject() ? project.getBasePath() : modelContentRootDir.getPath());
    }
    assert rootProjectPath != null;

    final VirtualFile PantsBuildFile = setupPantsBuildFile(modelContentRootDir);
    setupPantsSettingsFile(rootProjectPath, modelContentRootDir, modifiableRootModel);

    if (PantsBuildFile != null) {
      modifiableRootModel.getModule().putUserData(
        BUILD_SCRIPT_DATA, new BuildScriptDataBuilder(PantsBuildFile));
    }
  }

  @Override
  protected void setupModule(Module module) throws ConfigurationException {
    super.setupModule(module);
    assert rootProjectPath != null;

    VirtualFile buildScriptFile = null;
    final BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData(module);
    try {
      if (buildScriptDataBuilder != null) {
        buildScriptFile = buildScriptDataBuilder.getBuildScriptFile();
        final String text = buildScriptDataBuilder.build();
        appendToFile(buildScriptFile, "\n" + text);
      }
    }
    catch (IOException e) {
      LOG.warn("Unexpected exception on applying frameworks templates", e);
    }

    final Project project = module.getProject();
    if (myWizardContext.isCreatingNewProject()) {
      getExternalProjectSettings().setExternalProjectPath(rootProjectPath);
      AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID);
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      //noinspection unchecked
      settings.linkProject(getExternalProjectSettings());
    }
    else {
      FileDocumentManager.getInstance().saveAllDocuments();
      final PantsProjectSettings PantsProjectSettings = getExternalProjectSettings();
      final VirtualFile finalBuildScriptFile = buildScriptFile;
      Runnable runnable = new Runnable() {
        public void run() {
          if (myParentProject == null) {
            PantsProjectSettings.setExternalProjectPath(rootProjectPath);
            AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID);
            //noinspection unchecked
            settings.linkProject(PantsProjectSettings);
          }

          ExternalSystemUtil.refreshProject(
            project, PantsConstants.SYSTEM_ID, rootProjectPath, false,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC
          );

          final PsiFile psiFile;
          if (finalBuildScriptFile != null) {
            psiFile = PsiManager.getInstance(project).findFile(finalBuildScriptFile);
            if (psiFile != null) {
              EditorHelper.openInEditor(psiFile);
            }
          }
        }
      };

      // execute when current dialog is closed
      ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, runnable);
    }
  }

  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    myWizardContext = wizardContext;
    return new ModuleWizardStep[]{
      new PantsModuleWizardStep(this, wizardContext),
      new ExternalModuleSettingsStep<PantsProjectSettings>(
        wizardContext, this, new PantsProjectSettingsControl(getExternalProjectSettings()))
    };
  }

  @Nullable
  @Override
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    final PantsFrameworksWizardStep step = new PantsFrameworksWizardStep(context, this);
    Disposer.register(parentDisposable, step);
    return step;
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
  private VirtualFile setupPantsBuildFile(@NotNull VirtualFile modelContentRootDir)
    throws ConfigurationException {
    final VirtualFile file = getOrCreateExternalProjectConfigFile(modelContentRootDir.getPath(), PantsConstants.DEFAULT_SCRIPT_NAME);

    if (file != null) {
      final String templateName = DEFAULT_TEMPLATE_Pants_BUILD;
      Map<String, String> attributes = ContainerUtil.newHashMap();
      if (myProjectId != null) {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_VERSION, myProjectId.getVersion());
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_GROUP, myProjectId.getGroupId());
        // TODO: remove this stub
        attributes.put(TEMPLATE_ATTRIBUTE_Pants_VERSION, "1.0");
      }
      saveFile(file, templateName, attributes);
    }
    return file;
  }

  @Nullable
  private VirtualFile setupPantsSettingsFile(@NotNull String rootProjectPath,
                                              @NotNull VirtualFile modelContentRootDir,
                                              @NotNull ModifiableRootModel model)
    throws ConfigurationException {
    final VirtualFile file = getOrCreateExternalProjectConfigFile(rootProjectPath, PantsConstants.SETTINGS_FILE_NAME);
    if (file == null) return null;

    final String moduleName = myProjectId == null ? model.getModule().getName() : myProjectId.getArtifactId();
    if (myWizardContext.isCreatingNewProject() || myParentProject == null) {
      final String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, file.getParent(), '/');

      Map<String, String> attributes = ContainerUtil.newHashMap();
      final String projectName = model.getProject().getName();
      attributes.put(TEMPLATE_ATTRIBUTE_PROJECT_NAME, projectName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, moduleDirName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      saveFile(file, TEMPLATE_Pants_SETTINGS, attributes);
    }
    else {
      char separatorChar = file.getParent() == null || !VfsUtilCore.isAncestor(file.getParent(), modelContentRootDir, true) ? '/' : ':';
      String modulePath = VfsUtil.getPath(file, modelContentRootDir, separatorChar);

      Map<String, String> attributes = ContainerUtil.newHashMap();
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      // check for flat structure
      final String flatStructureModulePath =
        modulePath != null && StringUtil.startsWith(modulePath, "../") ? StringUtil.trimStart(modulePath, "../") : null;
      if (StringUtil.equals(flatStructureModulePath, modelContentRootDir.getName())) {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR, "true");
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, flatStructureModulePath);
      }
      else {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, modulePath);
      }

      appendToFile(file, TEMPLATE_Pants_SETTINGS_MERGE, attributes);
    }
    return file;
  }

  private static void saveFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
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
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on appending template %s config", PantsConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't append %s template config text", PantsConstants.SYSTEM_ID.getReadableName())
      );
    }
  }


  @Nullable
  private static VirtualFile getOrCreateExternalProjectConfigFile(@NotNull String parent, @NotNull String fileName) {
    File file = new File(parent, fileName);
    FileUtilRt.createIfNotExists(file);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  public void setParentProject(@Nullable ProjectData parentProject) {
    myParentProject = parentProject;
  }

  public boolean isInheritGroupId() {
    return myInheritGroupId;
  }

  public void setInheritGroupId(boolean inheritGroupId) {
    myInheritGroupId = inheritGroupId;
  }

  public boolean isInheritVersion() {
    return myInheritVersion;
  }

  public void setInheritVersion(boolean inheritVersion) {
    myInheritVersion = inheritVersion;
  }

  public ProjectId getProjectId() {
    return myProjectId;
  }

  public void setProjectId(@NotNull ProjectId projectId) {
    myProjectId = projectId;
  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    if (settingsStep instanceof ProjectSettingsStep) {
      final ProjectSettingsStep projectSettingsStep = (ProjectSettingsStep)settingsStep;
      if (myProjectId != null) {
        final JTextField moduleNameField = settingsStep.getModuleNameField();
        if (moduleNameField != null) {
          moduleNameField.setText(myProjectId.getArtifactId());
        }
        projectSettingsStep.setModuleName(myProjectId.getArtifactId());
      }
      projectSettingsStep.bindModuleSettings();
    }
    return super.modifySettingsStep(settingsStep);
  }

  public static void appendToFile(@NotNull VirtualFile file, @NotNull String text) throws IOException {
    String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);
    if (lineSeparator == null) {
      lineSeparator = CodeStyleSettingsManager.getSettings(ProjectManagerEx.getInstanceEx().getDefaultProject()).getLineSeparator();
    }
    final String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(file));
    String content = (StringUtil.isNotEmpty(existingText) ? existingText + lineSeparator : "") +
                     StringUtil.convertLineSeparators(text, lineSeparator);
    VfsUtil.saveText(file, content);
  }

  @Nullable
  public static BuildScriptDataBuilder getBuildScriptData(@Nullable Module module) {
    return module == null ? null : module.getUserData(BUILD_SCRIPT_DATA);
  }
}
