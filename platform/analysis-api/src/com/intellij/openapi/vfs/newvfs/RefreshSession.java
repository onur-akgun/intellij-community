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
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author max
 */
public abstract class RefreshSession {
  public long getId() {
    return 0;
  }

  public abstract boolean isAsynchronous();

  public abstract void addFile(@NotNull VirtualFile file);

  public abstract void addAllFiles(@NotNull Collection<? extends VirtualFile> files);

  public void addAllFiles(VirtualFile @NotNull ... files) {
    addAllFiles(Arrays.asList(files));
  }

  public abstract void launch();
}
