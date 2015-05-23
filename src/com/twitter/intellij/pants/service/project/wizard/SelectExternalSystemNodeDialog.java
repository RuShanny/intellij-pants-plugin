// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.view.ExternalProjectsStructure;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewAdapter;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleNodeVisitor;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.InputEvent;
import java.util.Collection;
import java.util.List;

/**
 * Created by rushana on 23.05.15.
 */
public class SelectExternalSystemNodeDialog extends DialogWrapper {

  private final SimpleTree myTree;
  private final NodeSelector mySelector;

  public SelectExternalSystemNodeDialog(Project project,
                                        String title,
                                        final Class<? extends ExternalSystemNode> nodeClass,
                                        NodeSelector selector) {
    super(project, false);
    mySelector = selector;
    setTitle(title);

    myTree = new SimpleTree();
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    final ExternalProjectsView projectsView = ExternalProjectsManager.getInstance(project).getExternalProjectsView(PantsConstants.SYSTEM_ID);
    if(projectsView != null) {
      final ExternalProjectsStructure treeStructure = new ExternalProjectsStructure(project, myTree) {
        @SuppressWarnings("unchecked")
        @Override
        protected Class<? extends ExternalSystemNode>[] getVisibleNodesClasses() {
          return new Class[]{nodeClass};
        }
      };
      treeStructure.init(new ExternalProjectsViewAdapter(projectsView) {
                           @Nullable
                           @Override
                           public ExternalProjectsStructure getStructure() {
                             return treeStructure;
                           }

                           @Override
                           public void updateUpTo(ExternalSystemNode node) {
                             treeStructure.updateUpTo(node);
                           }

                           @Override
                           public void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
                             SelectExternalSystemNodeDialog.this.handleDoubleClickOrEnter(node, actionId, inputEvent);
                           }
                         });

      final Collection<ExternalProjectInfo> projectsData =
        ProjectDataManager.getInstance().getExternalProjectsData(project, PantsConstants.SYSTEM_ID);

      final List<DataNode<ProjectData>> dataNodes =
        ContainerUtil.mapNotNull(
          projectsData, new Function<ExternalProjectInfo, DataNode<ProjectData>>() {
            @Override
            public DataNode<ProjectData> fun(ExternalProjectInfo info) {
              return info.getExternalProjectStructure();
            }
          }
        );
      treeStructure.updateProjects(dataNodes);

      final SimpleNode[] selection = new SimpleNode[]{null};
      treeStructure.accept(new SimpleNodeVisitor() {
                             public boolean accept(SimpleNode each) {
                               if (!mySelector.shouldSelect(each)) return false;
                               selection[0] = each;
                               return true;
                             }
                           });
      if (selection[0] != null) {
        treeStructure.select(selection[0]);
      }
    }

    init();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  protected void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
  }

  protected SimpleNode getSelectedNode() {
    return myTree.getNodeFor(myTree.getSelectionPath());
  }

  @Nullable
  protected JComponent createCenterPanel() {
    final JScrollPane pane = ScrollPaneFactory.createScrollPane(myTree);
    pane.setPreferredSize(JBUI.size(320, 400));
    return pane;
  }

  protected interface NodeSelector {
    boolean shouldSelect(SimpleNode node);
  }
}

