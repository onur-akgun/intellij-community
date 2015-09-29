/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class NextOccurrenceAction extends PrevNextOccurrenceAction {
  public NextOccurrenceAction() {
    super(IdeActions.ACTION_NEXT_OCCURENCE);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    e.getRequiredData(SearchSession.KEY).searchForward();
  }

  @NotNull
  @Override
  protected List<Shortcut> getDefaultShortcuts() {
    return Utils.shortcutsOf(IdeActions.ACTION_FIND_NEXT);
  }

  @NotNull
  @Override
  protected List<Shortcut> getSingleLineShortcuts() {
    return ContainerUtil.append(Utils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN), CommonShortcuts.ENTER.getShortcuts());
  }
}
