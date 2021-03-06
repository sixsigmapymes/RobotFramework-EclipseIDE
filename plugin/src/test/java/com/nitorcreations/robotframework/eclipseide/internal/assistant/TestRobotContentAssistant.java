/**
 * Copyright 2012 Nitor Creations Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitorcreations.robotframework.eclipseide.internal.assistant;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.nitorcreations.robotframework.eclipseide.editors.IResourceManager;
import com.nitorcreations.robotframework.eclipseide.editors.ResourceManagerProvider;
import com.nitorcreations.robotframework.eclipseide.structure.ParsedString;
import com.nitorcreations.robotframework.eclipseide.structure.ParsedString.ArgumentType;

@RunWith(Enclosed.class)
public class TestRobotContentAssistant {
    @Ignore
    public abstract static class Base {
        static final String BUILTIN_KEYWORD = "BuiltIn Keyword";
        static final String BUILTIN_VARIABLE = "${BUILTIN_VARIABLE}";
        static final String BUILTIN_PREFIX = "[BuiltIn] ";
        static final String BUILTIN_INDEX_FILE = "BuiltIn.index";

        IProposalGenerator proposalGenerator;
        ITextViewer textViewer;
        RobotContentAssistant assistant;
        IDocument document;

        final IProject project = mock(IProject.class, "project");
        final IResourceManager resourceManager = mock(IResourceManager.class, "resourceManager");

        @Before
        public void setup() throws Exception {
            proposalGenerator = mock(IProposalGenerator.class, "proposalGenerator");
            textViewer = mock(ITextViewer.class, "textViewer");
            assistant = new RobotContentAssistant(proposalGenerator);
            document = mock(IDocument.class, "document");
            when(textViewer.getDocument()).thenReturn(document);

            ResourceManagerProvider.set(resourceManager);

            final IWorkspace workspace = mock(IWorkspace.class, "workspace");
            final IWorkspaceRoot workspaceRoot = mock(IWorkspaceRoot.class, "workspaceRoot");
            final IPath projectFullPath = mock(IPath.class, "projectFullPath");
            final IPath builtinIndexPath = mock(IPath.class, "builtinIndexPath");
            final IFile builtinIndexFile = addFile(BUILTIN_INDEX_FILE, BUILTIN_KEYWORD + '\n' + BUILTIN_VARIABLE + '\n');

            when(project.getFullPath()).thenReturn(projectFullPath);
            when(projectFullPath.append("robot-indices/" + BUILTIN_INDEX_FILE)).thenReturn(builtinIndexPath);
            when(project.getWorkspace()).thenReturn(workspace);
            when(workspace.getRoot()).thenReturn(workspaceRoot);
            when(workspaceRoot.getFile(builtinIndexPath)).thenReturn(builtinIndexFile);
        }

        @SuppressWarnings("unchecked")
        protected IFile addFile(String fileName, String origContents) throws Exception {
            final IFile file = mock(IFile.class, fileName);
            ByteArrayInputStream contentStream = new ByteArrayInputStream(origContents.getBytes("UTF-8"));
            when(file.getContents()).thenReturn(contentStream).thenThrow(ArrayIndexOutOfBoundsException.class);
            when(file.getContents(anyBoolean())).thenReturn(contentStream).thenThrow(ArrayIndexOutOfBoundsException.class);
            when(file.getCharset()).thenReturn("UTF-8");
            when(file.getProject()).thenReturn(project);
            when(file.getName()).thenReturn(fileName);
            when(file.exists()).thenReturn(true);
            return file;
        }
    }

    @RunWith(Enclosed.class)
    public static class VariableReferences {

        public static class when_partially_entered extends Base {
            private static final class MockProposalAdder implements Answer<Void> {
                public final RobotCompletionProposalSet addedProposalSet = new RobotCompletionProposalSet();
                public final RobotCompletionProposal addedProposal;
                public final boolean insertInsteadOfAppend;

                MockProposalAdder(boolean basedOnInput, boolean insertInsteadOfAppend) {
                    this.insertInsteadOfAppend = insertInsteadOfAppend;
                    addedProposal = new RobotCompletionProposal(null, null, null, null, null, null, null);
                    addedProposalSet.getProposals().add(addedProposal);
                    addedProposalSet.setBasedOnInput(basedOnInput);
                }

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    @SuppressWarnings("unchecked")
                    List<RobotCompletionProposalSet> proposalSets = (List<RobotCompletionProposalSet>) invocation.getArguments()[3];
                    if (insertInsteadOfAppend) {
                        proposalSets.add(0, addedProposalSet);
                    } else {
                        proposalSets.add(addedProposalSet);
                    }
                    return null;
                }
            }

            static final String LINKED_PREFIX = "[linked] ";
            static final String LINKED_FILENAME = "linked.txt";
            static final String FOO_VARIABLE = "${FOO}";
            static final String LINKED_VARIABLE = "${LINKEDVAR}";

            @Test
            public void should_suggest_replacing_entered_variable() throws Exception {
                final String origContents1 = "*Variables\n" + FOO_VARIABLE + "  bar\n*Testcases\nTestcase\n  Log  ";
                final String origContents2 = "${F";
                final String origContents = origContents1 + origContents2;
                IFile origFile = addFile("orig.txt", origContents);
                when(resourceManager.resolveFileFor(document)).thenReturn(origFile);
                when(document.get()).thenReturn(origContents);
                when(document.getLineOfOffset(anyInt())).thenAnswer(new Answer<Integer>() {
                    @Override
                    public Integer answer(InvocationOnMock invocation) throws Throwable {
                        return document.get().substring(0, (Integer) invocation.getArguments()[0]).replaceAll("[^\n]+", "").length();
                    }
                });

                MockProposalAdder proposalAdder = new MockProposalAdder(true, false);
                doAnswer(proposalAdder).when(proposalGenerator).addVariableProposals(any(IFile.class), any(ParsedString.class), anyInt(), anyListOf(RobotCompletionProposalSet.class), anyInt(), anyInt());
                ICompletionProposal[] proposals = assistant.computeCompletionProposals(textViewer, origContents.length());
                assertSame(proposalAdder.addedProposal, proposals[0]);
                ParsedString expectedArgument = new ParsedString(origContents2, origContents1.length());
                expectedArgument.setHasSpaceAfter(false);
                expectedArgument.setType(ArgumentType.KEYWORD_ARG);
                verify(proposalGenerator).addVariableProposals(same(origFile), eq(expectedArgument), eq(origContents.length()), anyListOf(RobotCompletionProposalSet.class), eq(Integer.MAX_VALUE), eq(Integer.MAX_VALUE));
                verifyNoMoreInteractions(proposalGenerator);
            }
        }
    }
}
