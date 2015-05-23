// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.NullNode;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.twitter.intellij.pants.util.PantsConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

/**
 * Created by rushana on 23.05.15.
 */
public class SelectExternalProjectDialog extends SelectExternalSystemNodeDialog {


  private ProjectData myResult;

  public SelectExternalProjectDialog(Project project, final ProjectData current) {
    super(project, String.format("Select %s Project", PantsConstants.SYSTEM_ID.getReadableName()), ProjectNode.class,
          new SelectExternalSystemNodeDialog.NodeSelector() {
            public boolean shouldSelect(SimpleNode node) {
              if (node instanceof ProjectNode) {
                return ((ProjectNode)node).getData() == current;
              }
              return false;
            }
          });

    init();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    Action selectNoneAction = new AbstractAction("&None") {
      public void actionPerformed(ActionEvent e) {
        doOKAction();
        myResult = null;
      }
    };
    return new Action[]{selectNoneAction, getOKAction(), getCancelAction()};
  }

  @Override
  protected void doOKAction() {
    SimpleNode node = getSelectedNode();
    if (node instanceof NullNode) node = null;

    myResult = node instanceof ProjectNode ? ((ProjectNode)node).getData() : null;
    super.doOKAction();
  }

  @Override
  protected void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
    if(node instanceof ProjectNode ) {
      doOKAction();
    }
  }

  public ProjectData getResult() {
    return myResult;
  }
}