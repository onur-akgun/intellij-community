package org.jetbrains.plugins.groovy.springloaded;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Position manager to debug classes reloaded by org.springsource.springloaded
 * @author Sergey Evdokimov
 */
public class SpringLoadedPositionManager implements PositionManager {

  private static final Pattern GENERATED_CLASS_NAME = Pattern.compile("\\$\\$[A-Za-z0-9]{8}");
  
  private final DebugProcess myDebugProcess;

  public SpringLoadedPositionManager(DebugProcess debugProcess) {
    myDebugProcess = debugProcess;
  }

  @Override
  public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
    throw new NoDataException();
  }

  @NotNull
  @Override
  public List<ReferenceType> getAllClasses(final SourcePosition classPosition) throws NoDataException {
    int line;
    String className;

    AccessToken accessToken = ReadAction.start();
    try {
      className = findEnclosingName(classPosition);
      if (className == null) throw new NoDataException();

      line = classPosition.getLine();
    }
    finally {
      accessToken.finish();
    }

    List<ReferenceType> referenceTypes = myDebugProcess.getVirtualMachineProxy().classesByName(className);
    if (referenceTypes.isEmpty()) throw new NoDataException();

    Set<ReferenceType> res = new HashSet<ReferenceType>();

    for (ReferenceType referenceType : referenceTypes) {
      findNested(res, referenceType, line);
    }

    if (res.isEmpty()) {
      throw new NoDataException();
    }

    return new ArrayList<ReferenceType>(res);
  }

  @NotNull
  @Override
  public List<Location> locationsOfLine(ReferenceType type, SourcePosition position) throws NoDataException {
    throw new NoDataException();
  }

  @Nullable
  private static String findEnclosingName(final SourcePosition position) {
    PsiElement element = findElementAt(position);
    while (true) {
      element = PsiTreeUtil.getParentOfType(element, GrTypeDefinition.class, PsiClassImpl.class);
      if (element == null
          || (element instanceof GrTypeDefinition && !((GrTypeDefinition)element).isAnonymous())
          || (element instanceof PsiClassImpl && ((PsiClassImpl)element).getName() != null)
        ) {
        break;
      }
    }

    return null;
  }

  @Nullable
  private static String getClassNameForJvm(final PsiClass aClass) {
    final PsiClass psiClass = aClass.getContainingClass();
    if (psiClass != null) {
      return getClassNameForJvm(psiClass) + "$" + aClass.getName();
    }

    return aClass.getQualifiedName();
  }

  @Nullable
  private static String getOuterClassName(final SourcePosition position) {
    AccessToken accessToken = ApplicationManager.getApplication().acquireReadActionLock();

    try {
      PsiElement element = findElementAt(position);
      if (element == null) return null;
      PsiElement sourceImage = PsiTreeUtil.getParentOfType(element, GrClosableBlock.class, GrTypeDefinition.class, PsiClassImpl.class);

      if (sourceImage instanceof PsiClass) {
        return getClassNameForJvm((PsiClass)sourceImage);
      }
      return null;
    }
    finally {
      accessToken.finish();
    }
  }

  @Nullable
  private static PsiElement findElementAt(SourcePosition position) {
    PsiFile file = position.getFile();
    if (!(file instanceof GroovyFileBase) && !(file instanceof PsiJavaFile)) return null;
    return file.findElementAt(position.getOffset());
  }

  @Override
  public ClassPrepareRequest createPrepareRequest(ClassPrepareRequestor requestor, SourcePosition position) throws NoDataException {
    String className = getOuterClassName(position);
    if (className == null) {
      throw new NoDataException();
    }

    return myDebugProcess.getRequestsManager().createClassPrepareRequest(requestor, className + "*");
  }

  private static boolean isSpringLoadedGeneratedClass(ReferenceType ownerClass, ReferenceType aClass) {
    String name = aClass.name();
    String ownerClassName = ownerClass.name();

    // return   name == ownerClassName + "$$" + /[A-Za-z0-9]{8}/
    return name.length() == ownerClassName.length() + 2 + 8
      && name.startsWith(ownerClassName)
      && GENERATED_CLASS_NAME.matcher(name.substring(ownerClassName.length())).matches();
  }
  
  private static void findNested(Set<ReferenceType> res, ReferenceType fromClass, int line) {
    if (!fromClass.isPrepared()) return;

    List<ReferenceType> nestedTypes = fromClass.nestedTypes();

    ReferenceType springLoadedGeneratedClass = null;

    for (ReferenceType nested : nestedTypes) {
      if (!nested.isPrepared()) continue;

      if (isSpringLoadedGeneratedClass(fromClass, nested)) {
        if (springLoadedGeneratedClass == null || !springLoadedGeneratedClass.name().equals(nested.name())) {
          springLoadedGeneratedClass = nested; // Only latest generated classes should be used.
        }
      }
      else {
        findNested(res, nested, line);
      }
    }

    try {
      final int lineNumber = line + 1;

      ReferenceType effectiveRef = springLoadedGeneratedClass == null ? fromClass : springLoadedGeneratedClass;

      if (effectiveRef.locationsOfLine(lineNumber).size() > 0) {
        res.add(effectiveRef);
      }
    }
    catch (ObjectCollectedException ignored) {

    }
    catch (AbsentInformationException ignored) {
    }
  }

}
