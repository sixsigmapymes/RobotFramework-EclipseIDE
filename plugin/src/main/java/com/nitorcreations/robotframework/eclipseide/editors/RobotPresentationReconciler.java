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
package com.nitorcreations.robotframework.eclipseide.editors;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;

public class RobotPresentationReconciler extends PresentationReconciler {

    public RobotPresentationReconciler(ColorManager colorManager) {
        ITokenScanner coloringScanner = new ColoringScanner(colorManager);
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(coloringScanner) {
            @Override
            public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {
                System.out.println("Document " + ResourceManagerProvider.get().resolveFileFor(e.getDocument()) + " region changed: " + e);
                // force damaging entire document for now; we don't support
                // partial reparsing just yet
                return partition;
            }
        };
        setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
    }

}
