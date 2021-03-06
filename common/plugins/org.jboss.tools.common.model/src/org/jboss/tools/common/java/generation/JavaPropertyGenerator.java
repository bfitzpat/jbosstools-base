/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.common.java.generation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.ui.*;
import org.eclipse.swt.SWT;
import org.jboss.tools.common.model.plugin.ModelPlugin;

public class JavaPropertyGenerator {
	IType owner;
	
	public void setOwner(IType owner) {
		this.owner = owner;
	}
	
	public void generate(String name, String javatype, String access, boolean field, boolean getter, boolean setter) throws CoreException {
		if("default".equals(access)) access = ""; else access += " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String fa = (getter && setter) ? "private " : access; //$NON-NLS-1$
		ICompilationUnit parentCU = owner.getCompilationUnit();
		String lineDelimiter = getLineDelimiterUsed(parentCU);
		ICompilationUnit createdWorkingCopy = (ICompilationUnit) parentCU.getWorkingCopy(null);
		IType createdType = createdWorkingCopy.getType(owner.getElementName());
		boolean isInterface = createdType.isInterface();
		ICompilationUnit cu = createdType.getCompilationUnit();	
		synchronized(cu) {
			cu.reconcile(ICompilationUnit.NO_AST, true, null, null);
		}
		if(field && !isInterface && 
				(createdType.getField(name) == null || !createdType.getField(name).exists())
				) {
			String fieldContents = "\t" + fa + javatype + " " + name + ";" + lineDelimiter; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			createdType.createField(fieldContents, null, true, null);
			synchronized(cu) {
				cu.reconcile(ICompilationUnit.NO_AST, true, null, null);
			}
		}
//		String namePart = name.substring(0, 1).toUpperCase() + name.substring(1);
		if(getter) {
			createGetter(cu, createdType, access, javatype, name, lineDelimiter);
		}
		if(setter) {
			createSetter(cu, createdType, access, javatype, name, lineDelimiter);
		}		
		cu.commitWorkingCopy(true, null);
	}
	
	public static void createGetter(ICompilationUnit cu, IType type, String access, String javatype, String name, String lineDelimiter) throws CoreException {
		String methodName = getAccessorName("get", name); //$NON-NLS-1$
		if(findGetter(type, methodName) != null) return;
		String methodHeader = access + javatype + " " + methodName + "()"; //$NON-NLS-1$ //$NON-NLS-2$
		String stub = null;
		if(!type.isInterface()) {
			methodHeader += " {" + lineDelimiter; //$NON-NLS-1$
			stub = methodHeader  + "}" + lineDelimiter; //$NON-NLS-1$
		} else {
			methodHeader += ";" + lineDelimiter; //$NON-NLS-1$
			stub = methodHeader;
		}
		IMethod m = type.createMethod(stub, null, true, null);
		String methodComment = CodeGeneration.getGetterComment(cu, type.getElementName(), m.getElementName(), name, javatype, name, lineDelimiter);
		String methodContent = (type.isInterface()) ? null : CodeGeneration.getGetterMethodBodyContent(cu, cu.getElementName(), m.getElementName(), name, lineDelimiter);
		editMethod(cu, m, methodHeader, methodComment, methodContent, lineDelimiter);
	}
	
	static IMethod findGetter(IType type, String methodName) {
		if(type == null || methodName == null) return null;
		try {
			IMethod[] ms = type.getMethods();
			for (int i = 0; i < ms.length; i++) {
				if(ms[i].getElementName().equals(methodName) && ms[i].getParameterNames().length == 0) return ms[i];
			}
		} catch (JavaModelException e) {
			return null;
		}
		
		return null;
	}

	public static void createSetter(ICompilationUnit cu, IType type, String access, String javatype, String name, String lineDelimiter) throws CoreException {
		String methodName = getAccessorName("set", name); //$NON-NLS-1$
		String methodHeader = access + "void " + methodName + "(" + javatype + " " + name + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String stub = null;
		if(!type.isInterface()) {
			methodHeader += " {" + lineDelimiter; //$NON-NLS-1$
			stub = methodHeader  + "}" + lineDelimiter; //$NON-NLS-1$
		} else {
			methodHeader += ";" + lineDelimiter; //$NON-NLS-1$
			stub = methodHeader;
		}
		IMethod m = type.createMethod(stub, null, true, null);
		String methodComment = CodeGeneration.getSetterComment(cu, type.getElementName(), m.getElementName(), name, javatype, name, name, lineDelimiter);
		String methodContent = (type.isInterface()) ? null : CodeGeneration.getSetterMethodBodyContent(cu, cu.getElementName(), m.getElementName(), "this." + name, name, lineDelimiter); //$NON-NLS-1$
		editMethod(cu, m, methodHeader, methodComment, methodContent, lineDelimiter);
	}
	
	static void editMethod(ICompilationUnit cu, IMethod m, String methodHeader, String methodComment, String methodContent, String lineDelimiter) throws CoreException {
		synchronized(cu) {
			cu.reconcile(ICompilationUnit.NO_AST, true, null, null);
		}
		ISourceRange range = m.getSourceRange();
		IBuffer buf = cu.getBuffer();
		StringBuffer sb = new StringBuffer(lineDelimiter);
		if(PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.CODEGEN_ADD_COMMENTS)) {
			sb.append(methodComment);			
		}
		sb.append(methodHeader);
		if(methodContent != null) {
			sb.append(methodContent).append("}").append(lineDelimiter); //$NON-NLS-1$
		}
		String formattedContent = JavaBeanGenerator.codeFormat2(CodeFormatter.K_CLASS_BODY_DECLARATIONS, sb.toString(), 1, lineDelimiter, cu.getJavaProject());
		if(formattedContent != null && formattedContent.startsWith("\t")) { //$NON-NLS-1$
			formattedContent = formattedContent.substring(1);
		}
		buf.replace(range.getOffset(), range.getLength(), formattedContent);
	}
	
	static String getAccessorName(String prefix, String name) {
		String namePart = name.substring(0, 1).toUpperCase() + name.substring(1);
		return prefix + namePart;
	}

	public static String getLineDelimiterUsed(ICompilationUnit cu) {
		if (cu == null || !cu.exists()) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IBuffer buf = null;
		try {
			buf = cu.getBuffer();
		} catch (JavaModelException e) {
			ModelPlugin.getPluginLog().logError(e);
		}
		if (buf == null) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		int length = buf.getLength();
		for (int i = 0; i < length; i++) {
			char ch = buf.getChar(i);
			if (ch == SWT.CR) {
				if (i + 1 < length) {
					if (buf.getChar(i + 1) == SWT.LF) {
						return "\r\n"; //$NON-NLS-1$
					}
				}
				return "\r"; //$NON-NLS-1$
			} else if (ch == SWT.LF) {
				return "\n"; //$NON-NLS-1$
			}
		}
		return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
