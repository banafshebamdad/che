/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.git.client.tree;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.FontAwesome;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ext.git.client.compare.FileStatus.Status;
import org.eclipse.che.ide.ext.git.client.tree.ChangedFolderNode.ChangedNodePresentation;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.smartTree.NodeLoader;
import org.eclipse.che.ide.ui.smartTree.NodeStorage;
import org.eclipse.che.ide.ui.smartTree.SelectionModel;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeStyles;
import org.eclipse.che.ide.ui.smartTree.compare.NameComparator;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link TreeView}.
 *
 * @author Igor Vinokur
 */
public class TreeViewImpl extends Composite implements TreeView {
    interface ChangedListViewImplUiBinder extends UiBinder<DockLayoutPanel, TreeViewImpl> {
    }

    private static ChangedListViewImplUiBinder uiBinder = GWT.create(ChangedListViewImplUiBinder.class);

    @UiField
    LayoutPanel changedFilesPanel;
    @UiField
    Button      changeViewModeButton;
    @UiField
    Button      expandButton;
    @UiField
    Button      collapseButton;

    @UiField(provided = true)
    final GitLocalizationConstant locale;
    @UiField(provided = true)
    final GitResources            res;

    private ActionDelegate delegate;
    private Tree           tree;

    private final NodesResources nodesResources;

    //    @Inject
    public TreeViewImpl(GitResources resources,
                        GitLocalizationConstant locale,
                        NodesResources nodesResources) {
        this.res = resources;
        this.locale = locale;
        this.nodesResources = nodesResources;

        initWidget(uiBinder.createAndBindUi(this));

        NodeStorage nodeStorage = new NodeStorage();
        NodeLoader nodeLoader = new NodeLoader();
        tree = new Tree(nodeStorage, nodeLoader);
        tree.getSelectionModel().setSelectionMode(SelectionModel.Mode.SINGLE);
        tree.getSelectionModel()
            .addSelectionChangedHandler(new SelectionChangedEvent.SelectionChangedHandler() {
                @Override
                public void onSelectionChanged(SelectionChangedEvent event) {
                    List<Node> selection = event.getSelection();
                    if (!selection.isEmpty()) {
                        delegate.onNodeSelected(selection.get(0));
                    }
                }
            });
        tree.setPresentationRenderer(new ChangedListRender(tree.getTreeStyles()));
        changedFilesPanel.add(tree);

        createButtons();
    }

    private class ChangedListRender extends DefaultPresentationRenderer<Node> {
        ChangedListRender(TreeStyles treeStyles) {
            super(treeStyles);
        }

        @Override
        public Element render(final Node node, final String domID, final Tree.Joint joint, final int depth) {
            final NodePresentation presentation;
            if (node instanceof HasPresentation) {
                presentation = ((HasPresentation)node).getPresentation(false);
            } else {
                presentation = new ChangedNodePresentation();
                presentation.setPresentableText(node.getName());
            }

            Element rootContainer = getRootContainer(domID);

            Element nodeContainer = getNodeContainer();

            nodeContainer.getStyle().setPaddingLeft((double)depth * 16, Style.Unit.PX);

            Element jointContainer = getJointContainer(joint);

            Element iconContainer = getIconContainer(presentation.getPresentableIcon());

            Element userElement = getUserElement(presentation.getUserElement());

            Element presentableTextContainer = getPresentableTextContainer(createPresentableTextElement(presentation));

            Element infoTextContainer = getInfoTextContainer(createInfoTextElement(presentation));

            Element descendantsContainer = getDescendantsContainer();

            final CheckBox checkBox = new CheckBox();
            Element element = checkBox.getElement();
            ((InputElement)element.getElementsByTagName("input").getItem(0))
                    .setChecked(((ChangedNodePresentation)presentation).isSelected());
            Event.sinkEvents(element, Event.ONCLICK);
            Event.setEventListener(element, new EventListener() {
                @Override
                public void onBrowserEvent(Event event) {
                    if (Event.ONCLICK == event.getTypeInt() && event.getTarget().getTagName().equalsIgnoreCase("label")) {
//                        delegate.onFileNodeCheckBoxValueChanged(node);
                        ((ChangedNodePresentation)presentation).setSelected(!((ChangedNode)node).isSelected());
//                        ((ChangedNode)node).setSelected(!((ChangedNode)node).isSelected());
                        for (Node node : tree.getAllChildNodes(Collections.singletonList(node), false)) {
                            ((ChangedNodePresentation)((HasPresentation)node).getPresentation(false))
                                    .setSelected(((ChangedNodePresentation)presentation).isSelected());
//                            ((ChangedNode)node).setSelected(!((ChangedNode)node).isSelected());
                        }
//                        render(node, domID, joint, depth);
                    }
                }
            });
            nodeContainer.appendChild(jointContainer);
            nodeContainer.appendChild(element);
            nodeContainer.appendChild(iconContainer);
            nodeContainer.appendChild(userElement == null ? Document.get().createSpanElement() : userElement);
            nodeContainer.appendChild(presentableTextContainer);
            nodeContainer.appendChild(infoTextContainer);

            rootContainer.appendChild(nodeContainer);
            rootContainer.appendChild(descendantsContainer);

            return rootContainer;
        }

        private Element createInfoTextElement(NodePresentation presentation) {
            DivElement textElement = Document.get().createDivElement();

            StringBuilder sb = new StringBuilder();

            if (presentation.getInfoTextWrapper() != null) {
                sb.append(presentation.getInfoTextWrapper().first);
            }

            if (!Strings.isNullOrEmpty(presentation.getInfoText())) {
                sb.append(presentation.getInfoText());
            }

            if (presentation.getInfoTextWrapper() != null) {
                sb.append(presentation.getInfoTextWrapper().second);
            }

            textElement.setInnerText(sb.toString());
            textElement.setAttribute("style", presentation.getInfoTextCss());

            //TODO support text colorization

            return textElement;
        }

        private Element createPresentableTextElement(NodePresentation presentation) {
            DivElement textElement = Document.get().createDivElement();

            textElement.setInnerText(Strings.nullToEmpty(presentation.getPresentableText()));
            textElement.setAttribute("style", presentation.getPresentableTextCss());

            //TODO support text colorization

            return textElement;
        }
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void viewChangedFilesAsList(@NotNull Map<String, Status> items) {
        tree.getNodeStorage().clear();
        for (String file : items.keySet()) {
            tree.getNodeStorage().add(new ChangedFileNode(file, items.get(file), nodesResources, delegate, false));
        }
    }

    @Override
    public void viewChangedFilesAsTree(@NotNull Map<String, Status> items) {
        tree.getNodeStorage().clear();
        List<Node> nodes = getGroupedNodes(items);
        if (nodes.size() == 1) {
            tree.getNodeStorage().add(nodes);
            tree.setExpanded(nodes.get(0), true);
        } else {
            for (Node node : nodes) {
                tree.getNodeStorage().add(node);
            }
        }
    }

    @Override
    public void collapseAllDirectories() {
        tree.collapseAll();
    }

    @Override
    public List<Node> getNodes() {
        return tree.getRootNodes();
    }

    @Override
    public void expandAllDirectories() {
        tree.expandAll();
    }

    @Override
    public void setEnableExpandCollapseButtons(boolean enabled) {
        expandButton.setEnabled(enabled);
        collapseButton.setEnabled(enabled);
    }

    @Override
    public void setTextToChangeViewModeButton(String text) {
        changeViewModeButton.setText(text);
    }

    private void createButtons() {
        changeViewModeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                delegate.onChangeViewModeButtonClicked();
            }
        });

        expandButton.setTitle(locale.changeListExpandCollapseAllButtonTitle());
        expandButton.getElement().setInnerHTML(FontAwesome.EXPAND);
        expandButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                delegate.onExpandButtonClicked();
            }
        });

        collapseButton.setTitle(locale.changeListCollapseAllButtonTitle());
        collapseButton.getElement().setInnerHTML(FontAwesome.COMPRESS);
        collapseButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                delegate.onCollapseButtonClicked();
            }
        });
    }

    private List<Node> getGroupedNodes(Map<String, Status> items) {
        List<String> allFiles = new ArrayList<>(items.keySet());
        List<String> allPaths = new ArrayList<>();
        for (String file : allFiles) {
            String path = file.substring(0, file.lastIndexOf("/"));
            if (!allPaths.contains(path)) {
                allPaths.add(path);
            }
        }
        List<String> commonPaths = getCommonPaths(allPaths);
        for (String commonPath : commonPaths) {
            if (!allPaths.contains(commonPath)) {
                allPaths.add(commonPath);
            }
        }

        Map<String, Node> preparedNodes = new HashMap<>();
        for (int i = getMaxNestedLevel(allFiles); i > 0; i--) {

            //Collect child files of all folders of current nesting level
            Map<String, List<Node>> currentChildNodes = new HashMap<>();
            for (String file : allFiles) {
                Path pathName = Path.valueOf(file);
                if (pathName.segmentCount() != i) {
                    continue;
                }
                Node fileNode = new ChangedFileNode(file, items.get(file), nodesResources, delegate, true);
                String filePath = pathName.removeLastSegments(1).toString();
                if (currentChildNodes.keySet().contains(filePath)) {
                    currentChildNodes.get(filePath).add(fileNode);
                } else {
                    List<Node> listFiles = new ArrayList<>();
                    listFiles.add(fileNode);
                    currentChildNodes.put(filePath, listFiles);
                }
            }

            //Map child files to related folders of current nesting level or just create a common folder
            for (String path : allPaths) {
                if (!(Path.valueOf(path).segmentCount() == i - 1)) {
                    continue;
                }
                Node folder = new ChangedFolderNode(getTransitFolderName(allPaths, path), nodesResources);
                if (currentChildNodes.keySet().contains(path)) {
                    folder.setChildren(currentChildNodes.get(path));
                }
                preparedNodes.put(path, folder);
            }

            //Take all child folders and nest them to related parent folders of current nesting level
            List<String> currentPaths = new ArrayList<>(preparedNodes.keySet());
            for (String parentPath : currentPaths) {
                List<Node> nodesToNest = new ArrayList<>();
                for (String nestedItem : currentPaths) {
                    if (!parentPath.equals(nestedItem) && (nestedItem.startsWith(parentPath + "/") || parentPath.isEmpty())) {
                        nodesToNest.add(preparedNodes.remove(nestedItem));
                    }
                }
                if (nodesToNest.isEmpty()) {
                    continue;
                }
                Collections.sort(nodesToNest, new NameComparator());
                if (currentChildNodes.keySet().contains(parentPath)) {
                    nodesToNest.addAll(currentChildNodes.get(parentPath));
                }
                if (parentPath.isEmpty()) {
                    return nodesToNest;
                } else {
                    preparedNodes.get(parentPath).setChildren(nodesToNest);
                }
            }
        }
        ArrayList<Node> nodes = new ArrayList<>(preparedNodes.values());
        Collections.sort(nodes, new NameComparator());
        return new ArrayList<>(nodes);
    }

    private String getTransitFolderName(List<String> allPaths, String comparedPath) {
        Path path = Path.valueOf(comparedPath);
        int segmentCount = path.segmentCount();
        for (int i = segmentCount; i > 0; i--) {
            if (allPaths.contains(path.removeLastSegments(segmentCount - i + 1).toString())) {
                return path.removeFirstSegments(i - 1).toString();
            }
        }
        return comparedPath;
    }

    private int getMaxNestedLevel(List<String> items) {
        int level = 0;
        for (String item : items) {
            int currentLevel = Path.valueOf(item).segmentCount();
            level = currentLevel > level ? currentLevel : level;
        }
        return level;
    }

    private List<String> getCommonPaths(List<String> allPaths) {
        List<String> commonPaths = new ArrayList<>();
        for (String path : allPaths) {
            int pathIndex = allPaths.indexOf(path);
            if (pathIndex + 1 == allPaths.size()) {
                continue;
            }
            String commonPath = getCommonPath(allPaths.get(pathIndex), allPaths.get(pathIndex + 1));
            if (!commonPath.isEmpty() && !commonPaths.contains(commonPath)) {
                commonPaths.add(commonPath);
            }
        }
        return commonPaths;
    }

    private String getCommonPath(String firstPath, String secondPath) {
        Path commonPath = Path.valueOf(firstPath);
        int segmentCount = commonPath.segmentCount();
        for (int i = 1; i < segmentCount; i++) {
            String path = commonPath.removeLastSegments(segmentCount - i).toString();
            if (!secondPath.startsWith(path)) {
                return Path.valueOf(path).removeLastSegments(1).toString();
            }
        }
        return commonPath.toString();
    }
}